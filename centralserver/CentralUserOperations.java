package centralserver;

import data.*;
import util.ClientIPUtil;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;

public class CentralUserOperations extends UnicastRemoteObject implements ICentralUserOperations {

    private final List<RMIAccess<IChatroomOperations>> chatroomNodes;
    private final Object chatroomNodeLock;
    private final List<RMIAccess<IDataOperations>> dataNodesOperations;
    private final Object dataNodeOperationsLock;
    private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
    private final Object dataNodeParticipantsLock;
    private final ResourceCleaner cleaner;

    private final CentralCoordinator coordinator;
    // const message for existing chatrooms -- used during re-establish connection
    private static final String EXISTING_CHATROOM_MESSAGE = "A chatroom with this name already exists";

    public CentralUserOperations(List<RMIAccess<IChatroomOperations>> chatroomNodes,
                             Object chatroomNodeLock,
                             List<RMIAccess<IDataOperations>> dataNodesOperations,
                             Object dataNodeOperationsLock,
                             List<RMIAccess<IDataParticipant>> dataNodesParticipants,
                             Object dataNodeParticipantsLock,
                             CentralCoordinator coordinator,
                             ResourceCleaner cleaner) throws RemoteException {
        this.chatroomNodes = chatroomNodes;
        this.chatroomNodeLock = chatroomNodeLock;
        this.dataNodesOperations = dataNodesOperations;
        this.dataNodeOperationsLock = dataNodeOperationsLock;
        this.dataNodesParticipants = dataNodesParticipants;
        this.dataNodeParticipantsLock = dataNodeParticipantsLock;
        this.coordinator = coordinator;
        this.cleaner = cleaner;
        
    }

    @Override
    public Response registerUser(String username, String password) throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Received register user request from client with IP \"%s\" for username \"%s\"",
				ClientIPUtil.getClientIP(),
				username
		));
 
        boolean success = false;
        String errorMessage = "";
        boolean userExists = false;
        synchronized (dataNodeOperationsLock) {
        	for (RMIAccess<IDataOperations> node : this.dataNodesOperations) {
				try {
					userExists = node.getAccess().userExists(username);
					break;
				} catch (RemoteException | NotBoundException e1) {
					Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
							"Unable to contact data node at \"%s:%d\"; skipping",
							node.getHostname(),
							node.getPort()
					));
				}
			}
		}

        // Don't allow users to have a : in the name or password!
        if (username.contains(":") || password.contains(":")) {
    		errorMessage = "You cannot have a username or password that contains \":\"";
    		Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Tried to create a username or password with \":\" %s, %s ",
                    username, password
            ));
    		return new Response(ResponseStatus.FAIL, errorMessage);
    	} else if (userExists) {
    		errorMessage = "User already exists";
    		Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    errorMessage,
                    username
            ));
    		return new Response(ResponseStatus.FAIL, errorMessage);
    	}
        
        else {
			Transaction t = new Transaction(Operations.CREATEUSER, username, password);
			success = TwoPhaseCommit.GenericCommit(dataNodeParticipantsLock, dataNodesParticipants, t, coordinator);
		}

        if (success) {
        	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                       "Created user \"%s\" successfully",
                       username
               ));
            return new Response(ResponseStatus.OK, "success");
        } else {
			Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
					"Failed to create new user \"%s\"",
					username
			));
            return new Response(ResponseStatus.FAIL, errorMessage);
        }
    }

    @Override
    public Response login(String username, String password) throws RemoteException {

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Attempting to log in user \"%s\"",
                username
        ));

        synchronized (dataNodeOperationsLock) {
            if (this.dataNodesOperations.size() == 0) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "There are currently no data nodes registered with the central server; unable to perform login for user \"%s\"",
                        username
                ));
                return new Response(ResponseStatus.FAIL, "Unable to perform login");
            }
            // iterate through data nodes to attempt login
            // in the event one node crashes, another node may have the login info necessary
            boolean success = false;
            Response response = null;
            for (RMIAccess<IDataOperations> nodeAccessor : this.dataNodesOperations) {
                try {
                    response = nodeAccessor.getAccess().verifyUser(username, password);
                } catch (NotBoundException e) {
                    Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact data node at \"%s:%d\"; skipping",
                            nodeAccessor.getHostname(),
                            nodeAccessor.getPort()
                    ));
                    continue;
                }
                if (response.getStatus() == ResponseStatus.OK) {
                    success = true;
                    break;
                }
            }

            if (success) {
                Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "Successfully logged in user",
                        username
                ));
                return new Response(ResponseStatus.OK, "success");
            } else {
                Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "Unable to log in user \"%s\" with message: \"%s\"",
                        username,
                        response.getMessage()
                ));
                return new Response(ResponseStatus.FAIL, "Login failed");
            }
        }
    }

    @Override
    public List<String> listChatrooms() throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Received request for list of chatrooms from user at \"%s\"",
				ClientIPUtil.getClientIP()
		));

        List<String> chatroomList = new LinkedList<>();
        synchronized (chatroomNodeLock) {
            for (RMIAccess<IChatroomOperations> chatroomAccess : this.chatroomNodes) {
                ChatroomListResponse chatroomListResponse = null;
                try {
                    chatroomListResponse = chatroomAccess.getAccess().getChatrooms();
                } catch (NotBoundException e) {
                    Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact Chat server at \"%s:%d\"; skipping",
                            chatroomAccess.getHostname(),
                            chatroomAccess.getPort()
                    ));
                    continue;
                }
                for (String name : chatroomListResponse.getChatroomNames()) {
                    chatroomList.add(name);
                }
            }
        }
        return chatroomList;
    }

    @Override
    public ChatroomResponse createChatroom(String chatroomName, String username) throws RemoteException {
    	// do 2pc here    if it fails, return response with fail and message ??check length of chatroom nodes list??
    	// can commit? if yes then then get ready to return innercreatechatroom then do commit,

		Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
				"Received create chatroom request for chatroom \"%s\" from user \"%s\" at \"%s\"",
				chatroomName,
				username,
				ClientIPUtil.getClientIP()
		));
   
        String errorMessage = "";
        boolean chatroomExists = false;
        ChatroomResponse response;

        // determine if chatroom already exists at one of the nodes
        synchronized (dataNodeOperationsLock) {
			chatroomExists = isChatroomExists(chatroomName);
		}

        // Don't allow users to have a : in the name or password!
        if (chatroomName.contains(":")) {
    		errorMessage = "You cannot have a chatroom name that contains \":\"";
    		Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Tried to create a chatroom with \":\" %s ",
                    chatroomName
            ));
    		return new ChatroomResponse(ResponseStatus.FAIL, errorMessage);
    	} else if (chatroomExists) {
    		errorMessage = "Chatroom already exists";
    		Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    errorMessage,
                    chatroomName
            ));
    		return new ChatroomResponse(ResponseStatus.FAIL, errorMessage);
    	} else {

			int votesYes = 0;
			int nodesContacted = 0;

    		Transaction t = new Transaction(Operations.CREATECHATROOM, chatroomName, username);

			coordinator.setCoordinatorDecision(t, Ack.NA);

            // if there's an error, create a string for errorMessage to send back to the client
            synchronized (dataNodeParticipantsLock) {
				// Attempt a canCommit
				for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
					IDataParticipant dataNode = null;
					try {
						dataNode = participant.getAccess();
					} catch (NotBoundException | RemoteException e) {
						Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
								"Unable to contact data node at \"%s:%d\", skipping...",
								participant.getHostname(),
								participant.getPort()
						));
						continue;
					}

					nodesContacted++;

					// Make sure everyone votes yes.
					// Make sure everyone votes yes.
					if (dataNode.canCommit(t, participant) == Ack.YES) {
						Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
								"Participant node at \"%s:%d\" voted YES",
								participant.getHostname(),
								participant.getPort()
						));
						votesYes++;
					} else {
						// If we get a no or NA vote, just stop looping
						Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
								"Participant node at \"%s:%d\" voted NO",
								participant.getHostname(),
								participant.getPort()
						));
					}
				}
            }
            if (votesYes == nodesContacted) {

            	// Roll the inner create chatroom THEN doCommit
            	response = CentralUserOperations.innerCreateChatroom(chatroomName, this.chatroomNodeLock, this.chatroomNodes);
            	
            	// If we can't advance, 
            	if (response.getStatus() == ResponseStatus.FAIL) {
					Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
							"Unable to create resources for transaction \"%s\", forcing abort",
							t.toString()
					));
            		coordinator.setCoordinatorDecision(t, Ack.NO);
					TwoPhaseCommit.forceAbort(t, dataNodesParticipants);
            		coordinator.removeCoordinatorDecision(t);
            		return response;
            	}

				coordinator.setCoordinatorDecision(t, Ack.YES);
            	
        		Object waitObject = new Object();
        		for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
        			Thread commitThread = null;

					try {
						commitThread = new Thread(new Runnable() {
							IDataParticipant dataNode = participant.getAccess();
							@Override
							public void run() {
								try {
									dataNode.doCommit(t, participant);
								} catch (RemoteException e) {
									Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
											"Something went wrong creating a thread for data node participant at \"%s:%d\"",
											participant.getHostname(),
											participant.getPort()
									));
								}
							}
						});
					} catch (Exception e) {
						Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
								"Unable to contact participant data node at \"%s:%d\", skipping...",
								participant.getHostname(),
								participant.getPort()
						));
						continue;
					}

					commitThread.start();
					coordinator.addWaitCommit(t, waitObject);
        		}
        		synchronized(waitObject) {
        			try {
						waitObject.wait(1000);
					} catch (InterruptedException e) {
						Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                "Something went wrong with the wait \"%s\"",
                                e
                        ));
						return new ChatroomResponse(ResponseStatus.FAIL, "Something went wrong, please try again");
					}
        		}
        		Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "Created chatrooom \"%s\" by \"%s\" successfully",
                        chatroomName, username
                ));

        		coordinator.removeCoordinatorDecision(t);

        		// Everything went well, return the response.
        		return response;
        	} else {
            	coordinator.setCoordinatorDecision(t, Ack.NO);
        		TwoPhaseCommit.forceAbort(t, dataNodesParticipants);
        		Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to create chatroom \"%s\"",
                        chatroomName
                ));

        		coordinator.removeCoordinatorDecision(t);

        		// We did an abort, send a fail message
        		return new ChatroomResponse(ResponseStatus.FAIL, "Something went wrong, please try again");
        	}
    	}
    }

	private boolean isChatroomExists(String chatroomName) {
    	boolean chatroomExists = false;
		for (RMIAccess<IDataOperations> node : this.dataNodesOperations) {
			try {
				chatroomExists = node.getAccess().chatroomExists(chatroomName);
				break;
			} catch (RemoteException | NotBoundException e1) {
				Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
						"Unable to contact data node at \"%s:%d\"; skipping",
						node.getHostname(),
						node.getPort()
				));
			}
		}
		return chatroomExists;
	}

	@Override
    public Response deleteChatroom(String chatroomName, String username, String password) throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Received delete chatroom request for chatroom \"%s\" from user \"%s\" at \"%s\"",
				chatroomName,
				username,
				ClientIPUtil.getClientIP()
		));

    	String errorMessage = "";
        boolean chatroomExists = false;
        int votesYes = 0;
        int nodesContacted = 0;
        Response response;
        
        synchronized (this.dataNodeOperationsLock) {
			chatroomExists = isChatroomExists(chatroomName);
		}

        if (!chatroomExists) {
    		errorMessage = "Chatroom doesn't exist";
    		Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    errorMessage,
                    chatroomName
            ));
    		return new Response(ResponseStatus.FAIL, errorMessage);
    	} else {
    		Transaction t = new Transaction(Operations.DELETECHATROOM, chatroomName, username);

    		coordinator.setCoordinatorDecision(t, Ack.NA);

            // if there's an error, create a string for errorMessage to send back to the client
            synchronized (dataNodeParticipantsLock) {
				// Attempt a canCommit
				for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
					IDataParticipant dataNode;
					try {
						dataNode = participant.getAccess();
					} catch (NotBoundException e) {
						Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
								"Unable to contact participant data node at \"%s:%d\"",
								participant.getHostname(),
								participant.getPort()
						));
						continue;
					}

					// Make sure everyone votes yes.
					if (dataNode.canCommit(t, participant) == Ack.YES) {
						Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
								"Participant node at \"%s:%d\" voted YES",
								participant.getHostname(),
								participant.getPort()
						));
						votesYes++;
						nodesContacted++;
					} else {
						// If we get a no or NA vote, just stop looping
						Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
								"Participant node at \"%s:%d\" voted NO",
								participant.getHostname(),
								participant.getPort()
						));
						nodesContacted++;
					}
				}
            }
            if (votesYes == nodesContacted) {
            	// Roll the inner create chatroom THEN doCommit
            	response = innerDeleteChatroom(chatroomName);
            	
            	// If we can't advance, 
            	if (response.getStatus() == ResponseStatus.FAIL) {
            		Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            				"Unable to delete resources for transaction \"%s\", forcing abort",
							t.toString()
					));
					coordinator.setCoordinatorDecision(t, Ack.NO);
					TwoPhaseCommit.forceAbort(t, dataNodesParticipants);
					coordinator.removeCoordinatorDecision(t);
					return response;
            	}

            	coordinator.setCoordinatorDecision(t, Ack.YES);
            	
        		Object waitObject = new Object();
        		for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
					Thread commitThread;
					try {
						commitThread = new Thread(new Runnable() {
							IDataParticipant dataNode = participant.getAccess();
							@Override
							public void run() {
								try {
									dataNode.doCommit(t, participant);
								} catch (RemoteException e) {
									Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
											"Something went wrong starting a thread at %s",
											participant.getHostname()
									));
								}
							}
						});
					} catch (Exception e) {
						Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
								"Unable to contact participant data node at \"%s:%d\", skipping...",
								participant.getHostname(),
								participant.getPort()
						));
						continue;
					}

					commitThread.start();
					coordinator.addWaitCommit(t, waitObject);
        		}
        		synchronized(waitObject) {
        			try {
						waitObject.wait(1000);
					} catch (InterruptedException e) {
						Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                "Something went wrong with the wait \"%s\"",
                                e
                        ));
						return new ChatroomResponse(ResponseStatus.FAIL, "Something went wrong, please try again");
					}
        		}
        		Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "Deleted chatrooom \"%s\" by \"%s\" successfully",
                        chatroomName, username
                ));

        		coordinator.removeCoordinatorDecision(t);

        		// Everything went well, return the response.
        		return response;
        	} else {
            	coordinator.setCoordinatorDecision(t, Ack.NO);
        		TwoPhaseCommit.forceAbort(t, dataNodesParticipants);
        		Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to delete chatroom \"%s\"",
                        chatroomName
                ));

        		coordinator.removeCoordinatorDecision(t);
        		// We did an abort, send a fail message
        		return new ChatroomResponse(ResponseStatus.FAIL, "Something went wrong, please try again");
        	}
    	}
	}

    @Override
    public ChatroomResponse getChatroom(String chatroomName) throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Received getChatroom request for chatroom \"%s\" from client at \"%s\"",
				chatroomName,
				ClientIPUtil.getClientIP()
		));

        synchronized (chatroomNodeLock) {
            return getChatroomResponse(chatroomName, chatroomNodes);
        }
    }


    @Override
    public ChatroomResponse reestablishChatroom(String chatroomName, String username) throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Received reestablish chatroom request for chatroom \"%s\" from user \"%s\" at \"%s\"",
				chatroomName,
				username,
				ClientIPUtil.getClientIP()
		));

        // clean outstanding chatroom nodes since we suspect one is now not working
        cleaner.cleanChatroomNodes();
        // create new chatroom using existing create chatroom functionality
        ChatroomResponse response = CentralUserOperations.innerCreateChatroom(chatroomName, this.chatroomNodeLock, this.chatroomNodes);
        if (response.getStatus() == ResponseStatus.FAIL && response.getMessage().compareTo(CentralUserOperations.EXISTING_CHATROOM_MESSAGE) == 0) {
        	Logger.writeMessageToLog("Chatroom has already been reestablished; getting chatroom data...");
            return CentralUserOperations.getChatroomResponse(chatroomName, chatroomNodes);
        } else {
            return response;
        }
    }

    private static ChatroomResponse getChatroomResponse(String chatroomName, List<RMIAccess<IChatroomOperations>> chatroomNodes) throws RemoteException {
        RMIAccess<IChatroomOperations> accessor = findChatroom(chatroomName, chatroomNodes);

        if (accessor == null) {
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Unable to find Chat Node with chatroom with name \"%s\"",
                    chatroomName
            ));
            return new ChatroomResponse(ResponseStatus.FAIL, "Unable to locate chatroom");
        }

        ChatroomDataResponse dataResponse = null;
        try {
            dataResponse = accessor.getAccess().getChatroomData();
        } catch (NotBoundException e) {
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Unable to access chat node server at \"%s:%d\"; unable to get chatroom data",
                    accessor.getHostname(),
                    accessor.getPort()
            ));
            return new ChatroomResponse(ResponseStatus.FAIL, "Unable to get chatroom data");
        }

        return new ChatroomResponse(ResponseStatus.OK, "success", chatroomName, dataResponse.getHostname(), dataResponse.getTcpPort(), dataResponse.getRmiPort());
    }

    private synchronized static RMIAccess<IChatroomOperations> findChatroom(String chatroomName, List<RMIAccess<IChatroomOperations>> chatroomNodes) throws RemoteException {
        RMIAccess<IChatroomOperations> accessor = null;
        for (RMIAccess<IChatroomOperations> chatroomAccess : chatroomNodes) {
            ChatroomListResponse listResponse = null;
            try {
                listResponse = chatroomAccess.getAccess().getChatrooms();
            } catch (NotBoundException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to contact Chat server at \"%s:%d\"; skipping",
                        chatroomAccess.getHostname(),
                        chatroomAccess.getPort()
                ));
                continue;
            }

            for (String name : listResponse.getChatroomNames()) {
                if (name.compareTo(chatroomName) == 0) {
                    accessor = chatroomAccess;
                    break;
                }
            }
        }
        return accessor;
    }

    private synchronized static ChatroomResponse innerCreateChatroom(String chatroomName,
                                                        Object chatroomNodeLock,
                                                        List<RMIAccess<IChatroomOperations>> chatroomNodes) throws RemoteException {
        boolean chatroomExists = false;
        synchronized (chatroomNodeLock) {
            for (RMIAccess<IChatroomOperations> chatroomAccess : chatroomNodes) {
                ChatroomListResponse chatroomListResponse = null;
                try {
                    chatroomListResponse = chatroomAccess.getAccess().getChatrooms();
                } catch (NotBoundException e) {
                    Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact Chat server at \"%s:%d\"; skipping",
                            chatroomAccess.getHostname(),
                            chatroomAccess.getPort()
                    ));
                    continue;
                }

                for (String name : chatroomListResponse.getChatroomNames()) {
                    if (name.compareTo(chatroomName) == 0) {
                        chatroomExists = true;
                        break;
                    }
                }
                if (chatroomExists) {
                    break;
                }
            }


            if (chatroomExists) {
                return new ChatroomResponse(ResponseStatus.FAIL, CentralUserOperations.EXISTING_CHATROOM_MESSAGE);
            }

            ChatroomDataResponse min = null;
            RMIAccess<IChatroomOperations> minAccess = null;
            for (RMIAccess<IChatroomOperations> chatroomAccess : chatroomNodes) {
                ChatroomDataResponse chatroomDataResponse = null;
                try {
                    chatroomDataResponse = chatroomAccess.getAccess().getChatroomData();
                } catch (NotBoundException e) {
                    Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact Chat server at \"%s:%d\"; skipping",
                            chatroomAccess.getHostname(),
                            chatroomAccess.getPort()
                    ));
                    continue;
                }

                if (min == null) {
                    min = chatroomDataResponse;
                    minAccess = chatroomAccess;
                } else {
                    // if the current min has more users than the new chatroom node, set min to the new chatroom node
                    if (min.getUsers() > chatroomDataResponse.getUsers()) {
                        min = chatroomDataResponse;
                        minAccess = chatroomAccess;
                    }
                    // if the current min has the same number of users than the new chatroom node,
                    // and the current min has more chatrooms than the new chatroom node,
                    // set the new chatroom node to be the min
                    else if (min.getUsers() == chatroomDataResponse.getUsers() && min.getChatrooms() > chatroomDataResponse.getChatrooms()) {
                        min = chatroomDataResponse;
                        minAccess = chatroomAccess;
                    }
                }
            }

            if (min == null || minAccess == null) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to determine Chat server with the least load; unable to create chatroom \"%s\"",
                        chatroomName
                ));
                return new ChatroomResponse(ResponseStatus.FAIL, "Unable to create chatroom");
            }

            Response response = null;
            try {
                response = minAccess.getAccess().createChatroom(chatroomName);
            } catch (NotBoundException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to contact Chat server at \"%s:%d\"; cannot create chatroom \"%s\"",
                        minAccess.getHostname(),
                        minAccess.getPort(),
                        chatroomName
                ));
                return new ChatroomResponse(ResponseStatus.FAIL, "Unable to create chatroom");
            }

            if (response.getStatus() == ResponseStatus.FAIL) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to create chatroom \"%s\" at Chat server at \"%s:%d\"",
                        chatroomName,
                        minAccess.getHostname(),
                        minAccess.getPort()
                ));
                return new ChatroomResponse(ResponseStatus.FAIL, "Unable to create chatroom");
            }

            return new ChatroomResponse(ResponseStatus.OK, "success", chatroomName, min.getHostname(), min.getTcpPort(), min.getRmiPort());
        }
    }

    public Response innerDeleteChatroom(String chatroomName) throws RemoteException {
    	synchronized (chatroomNodeLock) {
        	// Make sure that user has chatroom ownership
            RMIAccess<IChatroomOperations> accessor = CentralUserOperations.findChatroom(chatroomName, chatroomNodes);

            try {
                accessor.getAccess().deleteChatroom(chatroomName);
            } catch (NotBoundException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to contact Chat server at \"%s:%d\"; cannot delete chatroom \"%s\"",
                        accessor.getHostname(),
                        accessor.getPort(),
                        chatroomName
                ));
                return new Response(ResponseStatus.FAIL, "Unable to delete chatroom");
            }

            return new Response(ResponseStatus.OK, "Chatroom was successfully deleted");
        }
    }
    

    
}

package centralserver;

import data.*;
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

    public CentralUserOperations(List<RMIAccess<IChatroomOperations>> chatroomNodes,
                             Object chatroomNodeLock,
                             List<RMIAccess<IDataOperations>> dataNodesOperations,
                             Object dataNodeOperationsLock,
                             List<RMIAccess<IDataParticipant>> dataNodesParticipants,
                             Object dataNodeParticipantsLock) throws RemoteException {
        this.chatroomNodes = chatroomNodes;
        this.chatroomNodeLock = chatroomNodeLock;
        this.dataNodesOperations = dataNodesOperations;
        this.dataNodeOperationsLock = dataNodeOperationsLock;
        this.dataNodesParticipants = dataNodesParticipants;
        this.dataNodeParticipantsLock = dataNodeParticipantsLock;
    }

    @Override
    public Response registerUser(String username, String password) throws RemoteException {

        boolean success = false;
        String errorMessage = "";
        synchronized (dataNodeParticipantsLock) {
            // do 2pc here for registering a user
            // if there's an error, create a string for errorMessage to send back to the client

        }

        if (success) {
            return new Response(ResponseStatus.OK, "success");
        } else {
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
        //

        //

        return CentralUserOperations.innerCreateChatroom(chatroomName, username, this.chatroomNodeLock, this.chatroomNodes);
    }

    @Override
    public ChatroomResponse getChatroom(String chatroomName) throws RemoteException {
        synchronized (chatroomNodeLock) {
            return getChatroomResponse(chatroomName, chatroomNodes);
        }
    }

    @Override
    public Response deleteChatroom(String chatroomName, String username, String password) throws RemoteException {
        synchronized (chatroomNodeLock) {
            RMIAccess<IChatroomOperations> accessor = CentralUserOperations.findChatroom(chatroomName, chatroomNodes);

            if (accessor == null) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to locate chatroom \"%s\"; cannot delete chatroom",
                        chatroomName
                ));
                return new Response(ResponseStatus.FAIL, "Cannot find chatroom to delete");
            }

            ChatroomUserResponse userResponse = null;
            try {
                userResponse = accessor.getAccess().getChatroom(chatroomName);
            } catch (NotBoundException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to contact Chat server at \"%s:%d\"; cannot delete chatroom \"%s\"",
                        accessor.getHostname(),
                        accessor.getPort(),
                        chatroomName
                ));
                return new Response(ResponseStatus.FAIL, "Unable to delete chatroom");
            }

            if (userResponse.getUsername().compareTo(username) != 0) {
                Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "User \"%s\" attempted to delete a chatroom they did not create; denying delete request",
                        username
                ));
                return new Response(ResponseStatus.FAIL, "Delete request denied");
            }

            synchronized (dataNodeOperationsLock) {
                for (RMIAccess<IDataOperations> operationsAccess : dataNodesOperations) {
                    Response response = null;
                    try {
                        response = operationsAccess.getAccess().verifyUser(username, password);
                    } catch (NotBoundException e) {
                        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                "Unable to contact data node at \"%s:%d\"; trying next node...",
                                operationsAccess.getHostname(),
                                operationsAccess.getPort()
                        ));
                        continue;
                    }

                    if (response.getStatus() == ResponseStatus.FAIL) {
                        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                                "User \"%s\" either provided an invalid password or does not exist; cannot delete chatroom \"%s\"",
                                username,
                                chatroomName
                        ));
                        return new Response(ResponseStatus.FAIL, "Cannot verify user; unable to complete delete request");
                    }
                    break;
                }
            }

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

    @Override
    public ChatroomResponse reestablishChatroom(String chatroomName, String username) throws RemoteException {
        synchronized (chatroomNodeLock) {
            // remove the downed chat server node from the list of nodes
            List<RMIAccess<IChatroomOperations>> downedChatServers = new LinkedList<>();
            for (RMIAccess<IChatroomOperations> chatNode : chatroomNodes) {
                try {
                    chatNode.getAccess();
                } catch (NotBoundException e) {
                    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact chat server node at \"%s:%d\"; removing from list of active chat server nodes",
                            chatNode.getHostname(),
                            chatNode.getPort()

                    ));
                    downedChatServers.add(chatNode);
                }
            }
            for (RMIAccess<IChatroomOperations> chatNode : downedChatServers) {
                chatroomNodes.remove(chatNode);
            }
        }
        // create new chatroom using existing create chatroom functionality
        ChatroomResponse response = CentralUserOperations.innerCreateChatroom(chatroomName, username, this.chatroomNodeLock, this.chatroomNodes);
        if (response.getStatus() == ResponseStatus.FAIL && response.getMessage().compareTo("A chatroom with this name already exists") == 0) {
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

    private static RMIAccess<IChatroomOperations> findChatroom(String chatroomName, List<RMIAccess<IChatroomOperations>> chatroomNodes) throws RemoteException {
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

    private static ChatroomResponse innerCreateChatroom(String chatroomName,
                                                        String username,
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
                return new ChatroomResponse(ResponseStatus.FAIL, "A chatroom with this name already exists");
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
                response = minAccess.getAccess().createChatroom(chatroomName, username);
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
}

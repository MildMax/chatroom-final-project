package centralserver;

import data.Ack;
import data.ChatroomDataResponse;
import data.ChatroomListResponse;
import data.ChatroomResponse;
import data.ICentralUserOperations;
import data.IChatroomOperations;
import data.IDataOperations;
import data.IDataParticipant;
import data.Operations;
import data.Response;
import data.ResponseStatus;
import data.Transaction;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import util.ClientIPUtil;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

/**
 * CentralUserOperations class implements ICentralUserOperations interface  and is responsible
 * for validating all the user operations which can be acrried out in a chatroom
 * registration to transaction of data among several chatrooms.
 *
 */
public class CentralUserOperations extends UnicastRemoteObject implements ICentralUserOperations {

  private final List<RMIAccess<IChatroomOperations>> chatroomNodes;
  private final Object chatroomNodeLock;
  private final List<RMIAccess<IDataOperations>> dataNodesOperations;
  private final Object dataNodeOperationsLock;
  private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
  private final Object dataNodeParticipantsLock;
  private final ResourceCleaner cleaner;
  private final CentralCoordinator coordinator;
  private final Object reestablishLock;

  // const message for existing chatrooms -- used during re-establish connection
  private static final String EXISTING_CHATROOM_MESSAGE = 
      "A chatroom with this name already exists";

  /**
   * Constructor of centralUserOperations which accespts the below parameters. 
   * @param chatroomNodes list of all chatroom nodes
   * @param chatroomNodeLock lock on specific nodes
   * @param dataNodesOperations list of data operations on nodes
   * @param dataNodeOperationsLock locks on nodes
   * @param dataNodesParticipants list of participants 
   * @param dataNodeParticipantsLock list of locks on datanodes of participants
   * @param coordinator instance of the central coordinator
   * @param cleaner instance to clean to chatroom
   * @throws RemoteException to handle remote object invocations.
   */
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
    this.reestablishLock = new Object();

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

    // iterate through registered data nodes to determine if the 
    //provided username exists in the system
    synchronized (dataNodeOperationsLock) {
      if (this.dataNodesOperations.size() == 0) {
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "There are currently no data nodes registered with the central server; "
                + "unable to register user \"%s\"",
                username
            ));
        return new Response(ResponseStatus.FAIL, "Unable to register user");
      }

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
          "User ried to create a username or password with \":\" %s, %s ",
          username, password
          ));
      return new Response(ResponseStatus.FAIL, errorMessage);
      // if the username already exists, indicate to the client that 
      //they cannot create a duplicate username
    } else if (userExists) {
      errorMessage = "User already exists";
      Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
          errorMessage,
          username
          ));
      return new Response(ResponseStatus.FAIL, errorMessage);
      // otherwise, attempt to create a new username and password for the user
    } else {
      Transaction t = new Transaction(Operations.CREATEUSER, username, password);
      TwoPhaseCommit committer = new TwoPhaseCommit();
      success = committer.GenericCommit(dataNodeParticipantsLock, 
          dataNodesParticipants, t, coordinator);
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
      // if there are no data nodes currently registered, cannot
      if (this.dataNodesOperations.size() == 0) {
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "There are currently no data nodes registered with the "
                + "central server; unable to perform login for user \"%s\"",
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
            "Unable to log in user \"%s\": \"%s\"",
            username,
            response.getMessage()
            ));
        return new Response(ResponseStatus.FAIL, "Login failed");
      }
    }
  }

  @Override
  public ChatroomListResponse listChatrooms() throws RemoteException {

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

        if (chatroomListResponse != null) {
          chatroomList.addAll(chatroomListResponse.getChatroomNames());
        }
      }
    }
    return new ChatroomListResponse(chatroomList);
  }

  @Override
  public ChatroomResponse createChatroom(String chatroomName, 
      String username) throws RemoteException {

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
          "Tried to create a chatroom with \":\": %s",
          chatroomName
          ));
      return new ChatroomResponse(ResponseStatus.FAIL, errorMessage);
    } else if (chatroomExists) {
      // if chatroom exists log error and return message indicating room already exists to client
      errorMessage = ThreadSafeStringFormatter.format(
          "Chatroom \"%s\" already exists",
          chatroomName
          );
      Logger.writeErrorToLog(
          errorMessage
          );
      return new ChatroomResponse(ResponseStatus.FAIL, errorMessage);
    } else {

      // if chatroom does not exist and the chat name is valid, do 2pc to commit information for
      // new client
      Transaction t = new Transaction(Operations.CREATECHATROOM, chatroomName, username);
      coordinator.setCoordinatorDecision(t, Ack.NA);

      TwoPhaseCommit committer = new TwoPhaseCommit();
      boolean success = committer.canCommit(t, 
          this.dataNodesParticipants, this.dataNodeParticipantsLock);


      if (success) {

        // Roll the inner create chatroom THEN doCommit
        response = CentralUserOperations.innerCreateChatroom(chatroomName, 
            this.chatroomNodeLock, this.chatroomNodes);

        // If we can't advance,
        if (response.getStatus() == ResponseStatus.FAIL) {
          Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
              "Unable to create resources for transaction \"%s\", forcing abort",
              t.toString()
              ));
          coordinator.setCoordinatorDecision(t, Ack.NO);
          committer.doAbort(t, dataNodesParticipants, dataNodeParticipantsLock);
          coordinator.removeCoordinatorDecision(t);
          return response;
        }

        coordinator.setCoordinatorDecision(t, Ack.YES);

        committer.doCommit(t, this.dataNodesParticipants, 
            this.dataNodeParticipantsLock, coordinator);
        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
            "Created chatrooom \"%s\" by \"%s\" successfully",
            chatroomName, username
            ));

        coordinator.removeCoordinatorDecision(t);

        // Everything went well, return the response.
        return response;
      } else {
        coordinator.setCoordinatorDecision(t, Ack.NO);
        committer.doAbort(t, dataNodesParticipants, dataNodeParticipantsLock);
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

  private boolean isUserVerified(String username, String password) {
    // iterate through data nodes until the application can verify that the user exists
    // and that they have provided the correct password
    for (RMIAccess<IDataOperations> nodeAccessor : this.dataNodesOperations) {
      try {
        Response response = nodeAccessor.getAccess().verifyUser(username, password);
        // if verified at any node, return true to signal user and password combination
        // is valid
        // provides replication/location transparency
        if (response.getStatus() == ResponseStatus.OK) {
          return true;
        }
      } catch (NotBoundException | RemoteException e) {
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Unable to contact data node at \"%s:%d\"; skipping",
            nodeAccessor.getHostname(),
            nodeAccessor.getPort()
            ));
      }
    }
    return false;
  }

  private boolean isUserOwner(String chatroomName, String username) {
    // iterate through list of data nodes to verify that a particular user
    // is the original creator of the chatroom
    for (RMIAccess<IDataOperations> nodeAccessor : this.dataNodesOperations) {
      try {
        Response response = nodeAccessor.getAccess().verifyOwnership(chatroomName, username);
        // if user can be verified to own the chatroom, return true
        if (response.getStatus() == ResponseStatus.OK) {
          return true;
        }
      } catch (NotBoundException | RemoteException e) {
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Unable to contact data node at \"%s:%d\"; skipping",
            nodeAccessor.getHostname(),
            nodeAccessor.getPort()
            ));
      }
    }
    return false;
  }

  @Override
  public Response deleteChatroom(String chatroomName, 
      String username, String password) throws RemoteException {

    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received delete chatroom request for chatroom \"%s\" from user \"%s\" at \"%s\"",
        chatroomName,
        username,
        ClientIPUtil.getClientIP()
        ));

    String errorMessage = "";
    Response response;

    synchronized (this.dataNodeOperationsLock) {

      boolean chatroomExists = false;
      boolean userVerified = false;
      boolean userOwns = false;

      // if the chatroom does not exist, then there is nothing to delete
      // log error and return message to client without doing any remove operation
      chatroomExists = isChatroomExists(chatroomName);

      if (!chatroomExists) {
        errorMessage = "Chatroom doesn't exist";
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            errorMessage,
            chatroomName
            ));
        return new Response(ResponseStatus.FAIL, errorMessage);
      }

      // verify that the user attempting to delete the chatroom is registered with our
      // application
      userVerified = isUserVerified(username, password);

      // if the user is not verified, log error and send message back to client
      if (!userVerified) {
        errorMessage = "Unable to verify user";
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Could not verify user \"%s\" for create chatroom \"%s\"",
            username,
            chatroomName
            ));
        return new Response(ResponseStatus.FAIL, errorMessage);
      }

      // verify that the user attempting to delete the chatroom is the owner of
      // the chatroom
      userOwns = isUserOwner(chatroomName, username);

      // if the user is not the owner, log that another user attempted to delete a chatroom
      // they do not own, and send a fail message back to the client
      if (!userOwns) {
        errorMessage = ThreadSafeStringFormatter.format(
            "User \"%s\" is unauthorized to delete chatroom \"%s\"",
            username,
            chatroomName
            );
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "User \"%s\" attempted to delete chatroom \"%s\" that they do not own",
            username,
            chatroomName
            ));
        return new Response(ResponseStatus.FAIL, errorMessage);
      }
    }

    // otherwise, commit the delete operation with 2PC to clean up resources
    // at the data nodes and chat server nodes
    Transaction t = new Transaction(Operations.DELETECHATROOM, chatroomName, username);
    coordinator.setCoordinatorDecision(t, Ack.NA);
    TwoPhaseCommit committer = new TwoPhaseCommit();

    boolean success = committer.canCommit(t, 
        this.dataNodesParticipants, this.dataNodeParticipantsLock);

    if (success) {
      // determine whether the actual resource can be deleted
      response = innerDeleteChatroom(chatroomName);

      // If we can't delete the chatroom, indicate the operation failed and force abort even if
      // all participants are able to commit
      if (response.getStatus() == ResponseStatus.FAIL) {
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Unable to delete resources for transaction \"%s\", forcing abort",
            t.toString()
            ));
        coordinator.setCoordinatorDecision(t, Ack.NO);
        committer.doAbort(t, dataNodesParticipants, dataNodeParticipantsLock);
        coordinator.removeCoordinatorDecision(t);
        return response;
      }

      // otherwise, resource has been deleted, issue doCommit to available data server nodes
      coordinator.setCoordinatorDecision(t, Ack.YES);

      committer.doCommit(t, 
          this.dataNodesParticipants, this.dataNodeParticipantsLock, this.coordinator);

      Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
          "Deleted chatrooom \"%s\" by \"%s\" successfully",
          chatroomName, username
          ));

      coordinator.removeCoordinatorDecision(t);

      // Everything went well, return the response.
      return response;
    } else {
      // otherwise, if a NO ack is received, indicate abort to all data node participants
      coordinator.setCoordinatorDecision(t, Ack.NO);
      committer.doAbort(t, dataNodesParticipants, dataNodeParticipantsLock);
      Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Unable to delete chatroom \"%s\"",
          chatroomName
          ));

      coordinator.removeCoordinatorDecision(t);
      // We did an abort, send a fail message
      return new ChatroomResponse(ResponseStatus.FAIL, "Something went wrong, please try again");
    }

  }

  @Override
  public ChatroomResponse getChatroom(String chatroomName) throws RemoteException {

    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received getChatroom request for chatroom \"%s\" from client at \"%s\"",
        chatroomName,
        ClientIPUtil.getClientIP()
        ));
    // return information regarding address and ports for 
    //chatserver that contains the chatroom name provided
    // if chatroom does not exist , returns a fail message
    synchronized (chatroomNodeLock) {
      return getChatroomResponse(chatroomName, chatroomNodes);
    }
  }


  @Override
  public ChatroomResponse reestablishChatroom(String chatroomName, 
      String username) throws RemoteException {

    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received reestablish chatroom request for chatroom \"%s\" from user \"%s\" at \"%s\"",
        chatroomName,
        username,
        ClientIPUtil.getClientIP()
        ));

    // ensure one client can reestablish at a time
    // otherwise, innerCreateChatroom may incorrectly fail to 
    //return the EXISTING_CHATROOM_MESSAGE response
    synchronized (this.reestablishLock) {
      // clean outstanding chatroom nodes since we suspect one is now not working
      cleaner.cleanChatroomNodes();
      // create new chatroom using existing create chatroom functionality
      ChatroomResponse response = 
          CentralUserOperations.innerCreateChatroom(chatroomName, 
              this.chatroomNodeLock, this.chatroomNodes);
      // if the create operation fails saying an existing chatroom 
      //already exists, then another user already
      // initiated reestablishing the chatroom
      // instead, simply grab the info for the existing chatroom
      if (response.getStatus() 
          == ResponseStatus.FAIL && response.getMessage()
          .compareTo(CentralUserOperations.EXISTING_CHATROOM_MESSAGE) == 0) {
        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
            "Chatroom \"%s\" has already been reestablished; getting chatroom data...",
            chatroomName
            ));
        synchronized (chatroomNodeLock) {
          return CentralUserOperations.getChatroomResponse(chatroomName, chatroomNodes);
        }
        // otherwise, indicate the reestablish operation succeeded
      } else {
        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
            "Successfully reestablished chatroom \"%s\"",
            chatroomName
            ));
        return response;
      }
    }
  }

  /**
   * getChatroomResponse method fulfils the functionality of identifying
   * rmi accessor for the speciifc chat room and getting the data response 
   * from the server for the specific chatroom.
   * @param chatroomName name of the chat room
   * @param chatroomNodes nodes of the chatroom.
   * @return returns the tcp and rmi ports and address used to connect
   * @throws RemoteException handles exceptions caused while accessing remote objects.
   */
  private static ChatroomResponse getChatroomResponse(String chatroomName, 
      List<RMIAccess<IChatroomOperations>> chatroomNodes) throws RemoteException {
    // find the RMI accessor supporting the provided chatroom 
    //from the available chatroom nodes in the system
    RMIAccess<IChatroomOperations> accessor = findChatroom(chatroomName, chatroomNodes);

    if (accessor == null) {
      Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Unable to find Chat Node with chatroom with name \"%s\"",
          chatroomName
          ));
      return new ChatroomResponse(ResponseStatus.FAIL, "Unable to locate chatroom");
    }

    // if there is an error getting the data for the chat server, respond with FAIL
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

    // otherwise, respond to the user with the address and TCP 
    //and RMI ports used to connect with the
    // chat server hosting the chatroom
    return new ChatroomResponse(ResponseStatus.OK, "success", 
        chatroomName, dataResponse.getHostname(), 
        dataResponse.getTcpPort(), dataResponse.getRmiPort());
  }

  private synchronized static RMIAccess<IChatroomOperations> findChatroom(
      String chatroomName, List<RMIAccess<IChatroomOperations>> chatroomNodes) 
          throws RemoteException {
    // iterate through a list of available chat nodes to find the chatroom dynamically
    RMIAccess<IChatroomOperations> accessor = null;
    for (RMIAccess<IChatroomOperations> chatroomAccess : chatroomNodes) {
      // get a list of chatrooms from the chatroom server
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

      // iterate through the list of chatroom names, and if the provided 
      //name matches the name of a chatroom
      // on the server, collect the RMI accessor for the server hosting
      //the chatroom and break out of the look
      for (String name : listResponse.getChatroomNames()) {
        if (name.compareTo(chatroomName) == 0) {
          accessor = chatroomAccess;
          break;
        }
      }

      // if accessor is not null, it has been set after finding the appropriate chatroom,
      // break out of the loop
      if (accessor != null) {
        break;
      }
    }
    // return the RMI accessor that has access to the provided chatroom
    return accessor;
  }

  /**
   * ChatroomResponse fulfills the functionality of first iterating over
   * all the availablke chatrooms to see if the current chatroom instance is
   * available then verifying if the server contains the same name and if the 
   * chatroom exists in the server as well it returns with a failed message asking
   * user to reestablish a unique one. This method takes care of all the chatroom
   * operation responses.
   * @param chatroomName name of the chat room
   * @param chatroomNodeLock lock on the speciifc chatroomnode
   * @param chatroomNodes list of all chatroomNodes
   * @return the chatroomresponse message of the specific chatroom
   * @throws RemoteException handles the remote exceptions 
   */
  public static ChatroomResponse innerCreateChatroom(String chatroomName,
      Object chatroomNodeLock,
      List<RMIAccess<IChatroomOperations>> chatroomNodes) throws RemoteException {
    // iterate through a list of chatroom nodes to determine if an instance of the chatroom
    // exists at a chat server node in the system
    boolean chatroomExists = false;
    synchronized (chatroomNodeLock) {
      for (RMIAccess<IChatroomOperations> chatroomAccess : chatroomNodes) {

        // get a list of names of chatrooms that exist at the server
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

        // if the chat server contains a server with the same name, then  the chatroom exists
        // mark as such and break out of loop
        for (String name : chatroomListResponse.getChatroomNames()) {
          if (name.compareTo(chatroomName) == 0) {
            chatroomExists = true;
            break;
          }
        }
        // if chatroom has been found already, no need to continue iterating, break out
        // of loop
        if (chatroomExists) {
          break;
        }
      }
    }

    // if chatroom exists, respond with a FAIL message and use the constant existing chatroom
    // message (used in reestablish chatroom when a user has 
    //already reestablished prior to another user)
    if (chatroomExists) {
      return new ChatroomResponse(ResponseStatus.FAIL, 
          CentralUserOperations.EXISTING_CHATROOM_MESSAGE);
    }

    ChatroomDataResponse min = null;
    RMIAccess<IChatroomOperations> minAccess = null;

    // determine the chatroom with the least amount of load
    synchronized (chatroomNodeLock) {
      // iterate through all chatrooms to collect their user count and room count
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

        // if min is null, then no chatroom has yet been evauluated, set the first chatroom
        // as the min by which other nodes are compared
        if (min == null) {
          min = chatroomDataResponse;
          minAccess = chatroomAccess;
        } else {
          // if the current min has more users than the new chatroom node, 
          //set min to the new chatroom node
          if (min.getUsers() > chatroomDataResponse.getUsers()) {
            min = chatroomDataResponse;
            minAccess = chatroomAccess;
            // if the current min has the same number of users than the new chatroom node,
            // and the current min has more chatrooms than the new chatroom node,
            // set the new chatroom node to be the min
          } else if (min.getUsers() == chatroomDataResponse.getUsers() 
              && min.getChatrooms() > chatroomDataResponse.getChatrooms()) {
            min = chatroomDataResponse;
            minAccess = chatroomAccess;
          }
        }
      }
    }

    // if min cannot be determined, there are no servers available, log the error and
    // send an error message to the client
    if (min == null || minAccess == null) {
      Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Unable to determine Chat server with the least load; unable to create chatroom \"%s\"",
          chatroomName
          ));
      return new ChatroomResponse(ResponseStatus.FAIL, "Unable to create chatroom");
    }

    //once min has been found, create a new chatroom with the given name at that chat server node
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

    // if the create command returns with a FAIL message, log failure and send message
    // to the client indicating chatroom cannot be created
    if (response.getStatus() == ResponseStatus.FAIL) {
      Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Unable to create chatroom \"%s\" at Chat server at \"%s:%d\"",
          chatroomName,
          minAccess.getHostname(),
          minAccess.getPort()
          ));
      return new ChatroomResponse(ResponseStatus.FAIL, "Unable to create chatroom");
    }

    // otherwise, log that the create operation succeeded and return information about the server
    // hosting the chatroom
    // can be used by the client to connect to the new  chatroom
    return new ChatroomResponse(ResponseStatus.OK, "success", 
        chatroomName, min.getHostname(), min.getTcpPort(), min.getRmiPort());

  }

  private Response innerDeleteChatroom(String chatroomName) throws RemoteException {
    synchronized (chatroomNodeLock) {
      // find the chat server hosting the chatroom to be deleted
      RMIAccess<IChatroomOperations> accessor = 
          CentralUserOperations.findChatroom(chatroomName, chatroomNodes);

      // if accessor is null, unable to determine which node contains the chatroom
      // indicate resource cannot be found and thus cant be deleted, and respond
      // with appropriate fail message
      if (accessor == null) {
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Unable to find chatroom node with chatroom \"%s\"",
            chatroomName
            ));
        return new Response(ResponseStatus.FAIL, "Chatroom does not exist");
      }

      // attempt to delete the chatroom from the node that hosts the chatroom
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

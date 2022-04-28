package centralserver;

import data.ChatroomResponse;
import data.ICentralOperations;
import data.IChatroomOperations;
import data.IDataOperations;
import data.IDataParticipant;
import data.RegisterResponse;
import data.ResponseStatus;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import util.ClientIPUtil;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

/**
 * CentralOperations class implements the ICentralOperations interface and.
 * is mainly responsible for registering the chatroom nodes and data nodes
 * and to provide the master time for Cristian's algorithm.
 *
 */
public class CentralOperations extends UnicastRemoteObject implements ICentralOperations {

  private final List<RMIAccess<IChatroomOperations>> chatroomNodes;
  private final Object chatroomNodeLock;
  private final List<RMIAccess<IDataOperations>> dataNodesOperations;
  private final Object dataNodeOperationsLock;
  private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
  private final Object dataNodeParticipantsLock;
  private final ServerInfo serverInfo;

  /**
   * Constructor for CentralOperations
   *
   * @param chatroomNodes list of all chatroomnodes.
   * @param chatroomNodeLock locks on individual chatroomnode.
   * @param dataNodesOperations operations on datanodes.
   * @param dataNodeOperationsLock locks on the operations.
   * @param dataNodesParticipants participants of datanodes.
   * @param dataNodeParticipantsLock locks on participants.
   * @param serverInfo information of the server.
   * @throws RemoteException handles exception while accessing these remote objects.
   */
  public CentralOperations(List<RMIAccess<IChatroomOperations>> chatroomNodes,
      Object chatroomNodeLock,
      List<RMIAccess<IDataOperations>> dataNodesOperations,
      Object dataNodeOperationsLock,
      List<RMIAccess<IDataParticipant>> dataNodesParticipants,
      Object dataNodeParticipantsLock,
      ServerInfo serverInfo) throws RemoteException {
    this.chatroomNodes = chatroomNodes;
    this.chatroomNodeLock = chatroomNodeLock;
    this.dataNodesOperations = dataNodesOperations;
    this.dataNodeOperationsLock = dataNodeOperationsLock;
    this.dataNodesParticipants = dataNodesParticipants;
    this.dataNodeParticipantsLock = dataNodeParticipantsLock;
    this.serverInfo = serverInfo;
  }

  /**
   * Registers a data node with the central server application. Spins up existing chatrooms tracked
   * by the data node prior to the application previously shutting down.
   *
   * @param hostname hostname for the machine supporting the data server
   * @param dataOperationsPort port where data node is accepting operations requests
   * @param dataParticipantPort port where data node is accepting participant requests in 2 phase commmit
   * @param chatrooms list of chat rooms previously recorded in the data server
   * @return a response containing the central server Coordinator port for 2 phase commit
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public RegisterResponse registerDataNode(String hostname, 
      int dataOperationsPort, int dataParticipantPort, 
      List<String> chatrooms) throws RemoteException {

    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Registering data node at \"%s\" with operations port \"%d\" and participant port \"%d\"",
        hostname,
        dataOperationsPort,
        dataParticipantPort
        ));

    // track the operations RMI interface for the data node
    synchronized (dataNodeOperationsLock) {
      dataNodesOperations.add(new RMIAccess<>(hostname, dataOperationsPort, "IDataOperations"));
    }

    // track the participants RMI interface for the data node
    synchronized (dataNodeParticipantsLock) {
      dataNodesParticipants.add(new RMIAccess<>(hostname, dataParticipantPort, "IDataParticipant"));
    }

    // use list of existing chatrooms tracked by data node to 
    //spin up chatrooms at available chat servers
    for (String room : chatrooms) {
      Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
          "Spinning up existing chatroom \"%s\" from data server...",
          room
          ));
      // create the chatroom
      ChatroomResponse r = CentralUserOperations.innerCreateChatroom(room, 
          this.chatroomNodeLock, this.chatroomNodes);
      // if chatroom cannot be spun up, log error 
      //(may be case that the charoom has already been spun up
      // from another data node
      if (r.getStatus() == ResponseStatus.FAIL) {
        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Unable to spin up chatroom \"%s\": \"%s\"",
            room,
            r.getMessage()
            ));
      }
    }

    // return response telling data node where it can contact 
    //the central servers coordinator RMI interface
    return new RegisterResponse(serverInfo.getCoordinatorPort());
  }

  /**
   * Registers a chat server node with the central server application
   *
   * @param hostname hostname of the machine supporting the chat server
   * @param port port where chat server is accepting operations requests from central server
   * @return a response containing port for chatroom operations registry at the central server
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public RegisterResponse registerChatNode(String hostname, int port) throws RemoteException {

    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Registering chat node at \"%s\" with operations port \"%d\"",
        hostname,
        port
        ));

    // track chatroom operations RMI interface for the chat server
    synchronized (chatroomNodeLock) {
      chatroomNodes.add(new RMIAccess<>(hostname, port, "IChatroomOperations"));
    }

    // return response telling the chat server node where it can forward chat messages to be logged
    return new RegisterResponse(serverInfo.getChatroomPort());
  }

  /**
   * Gets the master server time for Cristian's Algorithm
   *
   * @return the master server time in milliseconds
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public long getServerTime() throws RemoteException {

    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received request for master server time from node at \"%s\"",
        ClientIPUtil.getClientIP()
        ));

    return System.currentTimeMillis();
  }


}

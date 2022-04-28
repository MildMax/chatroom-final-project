package centralserver;

import data.IChatroomOperations;
import data.IDataOperations;
import data.IDataParticipant;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

/**
 * Resource cleaner is responsible for removing inactive chat servers and data servers
 * from the system. Runs parallel to the main application.
 */
public class ResourceCleaner implements Runnable {

  private final List<RMIAccess<IChatroomOperations>> chatroomNodes;
  private final Object chatroomNodeLock;
  private final List<RMIAccess<IDataOperations>> dataNodesOperations;
  private final Object dataNodeOperationsLock;
  private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
  private final Object dataNodeParticipantsLock;

  /**
   * Creates an instance of the ResourceCleaner
   *
   * @param chatroomNodes list of all chat server interfaces in the system
   * @param chatroomNodeLock locks operations on chatroomNodes resource
   * @param dataNodesOperations list of data server operations interfaces in the system
   * @param dataNodeOperationsLock locks operations on the dataNodesOperations resource
   * @param dataNodesParticipants list of data server participant interfaces in the system
   * @param dataNodeParticipantsLock locks operations on the dataNodesParticipants resource
   * @throws RemoteException if there is an error during remote communication
   */
  public ResourceCleaner(List<RMIAccess<IChatroomOperations>> chatroomNodes,
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

  /**
   * Initiates the resource cleaner thread.
   */
  @Override
  public void run() {

    // loop through cleaning functionality for duration of application
    while (true) {
      // sleep for 60 seconds and then clean up outstanding resources
      try {
        Thread.sleep(60000);
      } catch (InterruptedException e) {
        Logger.writeMessageToLog("Cleanup thread wait was interrupted; "
            + "performing cleanup of dead Chat and Data nodes");
      }

      Logger.writeMessageToLog("Starting cleanup for dead Chat and Data nodes");

      this.cleanChatroomNodes();
      this.cleanDataNodes();
    }

  }

  /**
   * Cleans up unavailable chat server nodes in the system
   */
  public void cleanChatroomNodes() {
    Logger.writeMessageToLog("Cleaning unavailable chatroom nodes...");
    synchronized (chatroomNodeLock) {
      // remove the downed chat server node from the list of nodes
      List<RMIAccess<IChatroomOperations>> downedChatServers = new LinkedList<>();
      for (RMIAccess<IChatroomOperations> chatNode : chatroomNodes) {
        try {
          chatNode.getAccess();
          // if chat node cannot be accessed, assume it is down and track the node for deletion
        } catch (NotBoundException | RemoteException e) {
          Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Unable to contact chat server node at \"%s:%d\"; "
                  + "removing from list of active chat server nodes",
                  chatNode.getHostname(),
                  chatNode.getPort()

              ));
          downedChatServers.add(chatNode);
        }
      }

      // remove dead nodes from the list of registered chat nodes at the central server
      for (RMIAccess<IChatroomOperations> chatNode : downedChatServers) {
        chatroomNodes.remove(chatNode);
      }
    }
  }

  /**
   * Cleans up unavailable data server nodes in the system
   */
  public void cleanDataNodes() {
    Logger.writeMessageToLog("Cleaning unavailable data nodes...");

    synchronized (dataNodeOperationsLock) {
      // remove the downed chat server node from the list of nodes
      List<RMIAccess<IDataOperations>> downedNodeServers = new LinkedList<>();
      for (RMIAccess<IDataOperations> dataNode : dataNodesOperations) {
        try {
          dataNode.getAccess();
          // if data node cannot be accessed, assume it is down and track the node for deletion
        } catch (NotBoundException | RemoteException e) {
          Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Unable to contact data server node at \"%s:%d\"; "
                  + "removing from list of active data server nodes",
                  dataNode.getHostname(),
                  dataNode.getPort()

              ));
          downedNodeServers.add(dataNode);
        }
      }

      // remove dead nodes from the list of registered data nodes at the central server
      for (RMIAccess<IDataOperations> dataNode : downedNodeServers) {
        dataNodesOperations.remove(dataNode);
      }
    }

    synchronized (dataNodeParticipantsLock) {
      // remove the downed chat server node from the list of nodes
      List<RMIAccess<IDataParticipant>> downedNodeServers = new LinkedList<>();
      for (RMIAccess<IDataParticipant> dataNode : dataNodesParticipants) {
        try {
          dataNode.getAccess();
          // if data node cannot be accessed, assume it is down and track the node for deletion
        } catch (NotBoundException | RemoteException e) {
          Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Unable to contact data server node at \"%s:%d\"; "
                  + "removing from list of active data server nodes",
                  dataNode.getHostname(),
                  dataNode.getPort()

              ));
          downedNodeServers.add(dataNode);
        }
      }

      // remove dead nodes from the list of registered data nodes at the central server
      for (RMIAccess<IDataParticipant> dataNode : downedNodeServers) {
        dataNodesParticipants.remove(dataNode);
      }
    }

  }
}

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

public class ResourceCleaner implements Runnable {

    private final List<RMIAccess<IChatroomOperations>> chatroomNodes;
    private final Object chatroomNodeLock;
    private final List<RMIAccess<IDataOperations>> dataNodesOperations;
    private final Object dataNodeOperationsLock;
    private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
    private final Object dataNodeParticipantsLock;

    // const message for existing chatrooms -- used during re-establish connection
    private static final String EXISTING_CHATROOM_MESSAGE = "A chatroom with this name already exists";

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

    @Override
    public void run() {

        // loop through cleaning functionality for duration of application
        while(true) {
            // sleep for 60 seconds and then clean up outstanding resources
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                Logger.writeMessageToLog("Cleanup thread wait was interrupted; performing cleanup of dead Chat and Data nodes");
            }

            Logger.writeMessageToLog("Starting cleanup for dead Chat and Data nodes");

            this.cleanChatroomNodes();
            this.cleanDataNodes();
        }

    }

    public void cleanChatroomNodes() {
        Logger.writeMessageToLog("Cleaning unavailable chatroom nodes...");
        synchronized (chatroomNodeLock) {
            // remove the downed chat server node from the list of nodes
            List<RMIAccess<IChatroomOperations>> downedChatServers = new LinkedList<>();
            for (RMIAccess<IChatroomOperations> chatNode : chatroomNodes) {
                try {
                    chatNode.getAccess();
                } catch (NotBoundException | RemoteException e) {
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
    }

    public void cleanDataNodes() {
        Logger.writeMessageToLog("Cleaning unavailable data nodes...");

        synchronized (dataNodeOperationsLock) {
            // remove the downed chat server node from the list of nodes
            List<RMIAccess<IDataOperations>> downedNodeServers = new LinkedList<>();
            for (RMIAccess<IDataOperations> dataNode : dataNodesOperations) {
                try {
                    dataNode.getAccess();
                } catch (NotBoundException | RemoteException e) {
                    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact data server node at \"%s:%d\"; removing from list of active data server nodes",
                            dataNode.getHostname(),
                            dataNode.getPort()

                    ));
                    downedNodeServers.add(dataNode);
                }
            }
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
                } catch (NotBoundException | RemoteException e) {
                    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact data server node at \"%s:%d\"; removing from list of active data server nodes",
                            dataNode.getHostname(),
                            dataNode.getPort()

                    ));
                    downedNodeServers.add(dataNode);
                }
            }
            for (RMIAccess<IDataParticipant> dataNode : downedNodeServers) {
                dataNodesParticipants.remove(dataNode);
            }
        }

    }
}

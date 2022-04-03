package centralserver;

import data.ICentralOperations;
import data.IChatroomOperations;
import data.IDataOperations;
import data.IDataParticipant;
import util.RMIAccess;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class CentralOperations extends UnicastRemoteObject implements ICentralOperations {

    private final List<RMIAccess<IChatroomOperations>> chatroomNodes;
    private final Object chatroomNodeLock;
    private final List<RMIAccess<IDataOperations>> dataNodesOperations;
    private final Object dataNodeOperationsLock;
    private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
    private final Object dataNodeParticipantsLock;

    public CentralOperations(List<RMIAccess<IChatroomOperations>> chatroomNodes,
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
    public void registerDataNode(String hostname, int dataOperationsPort, int dataParticipantPort) throws RemoteException {
        synchronized (dataNodeOperationsLock) {
            dataNodesOperations.add(new RMIAccess<>(hostname, dataOperationsPort, "IDataOperations"));
        }

        synchronized (dataNodeParticipantsLock) {
            dataNodesParticipants.add(new RMIAccess<>(hostname, dataParticipantPort, "IDataParticipant"));
        }
    }

    @Override
    public void registerChatNode(String hostname, int port) throws RemoteException {
        synchronized (chatroomNodeLock) {
            chatroomNodes.add(new RMIAccess<>(hostname, port, "IChatroomOperations"));
        }
    }
}

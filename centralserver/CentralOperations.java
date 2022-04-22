package centralserver;

import data.*;
import util.ClientIPUtil;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;
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
    private final ServerInfo serverInfo;

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

    @Override
    public RegisterResponse registerDataNode(String hostname, int dataOperationsPort, int dataParticipantPort) throws RemoteException {

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Registering data node at \"%s\" with operations port \"%d\" and participant port \"%d\"",
                hostname,
                dataOperationsPort,
                dataParticipantPort
        ));

        synchronized (dataNodeOperationsLock) {
            dataNodesOperations.add(new RMIAccess<>(hostname, dataOperationsPort, "IDataOperations"));
        }

        synchronized (dataNodeParticipantsLock) {
            dataNodesParticipants.add(new RMIAccess<>(hostname, dataParticipantPort, "IDataParticipant"));
        }

        return new RegisterResponse(serverInfo.getCoordinatorPort());
    }

    @Override
    public RegisterResponse registerChatNode(String hostname, int port) throws RemoteException {

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Registering chat node at \"%s\" with operations port \"%d\"",
                hostname,
                port
        ));

        synchronized (chatroomNodeLock) {
            chatroomNodes.add(new RMIAccess<>(hostname, port, "IChatroomOperations"));
        }

        return new RegisterResponse(serverInfo.getChatroomPort());
    }

    @Override
    public long getServerTime() throws RemoteException {

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received request for master server time from node at \"%s\"",
                ClientIPUtil.getClientIP()
        ));

        return System.currentTimeMillis();
    }


}

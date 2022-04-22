package centralserver;

import data.*;
import util.*;

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
    public RegisterResponse registerDataNode(String hostname, int dataOperationsPort, int dataParticipantPort, List<String> chatrooms) throws RemoteException {

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

        // use list of existing chatrooms tracked by data node to spin up chatrooms at available chat servers
        for (String room : chatrooms) {
            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                    "Spinning up existing chatroom \"%s\" from data server...",
                    room
            ));
            ChatroomResponse r = CentralUserOperations.innerCreateChatroom(room, this.chatroomNodeLock, this.chatroomNodes);
            if (r.getStatus() == ResponseStatus.FAIL) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to spin up chatroom \"%s\": \"%s\"",
                        room,
                        r.getMessage()
                ));
            }
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

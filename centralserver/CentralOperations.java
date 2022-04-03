package centralserver;

import data.ICentralOperations;
import data.IChatroomOperations;
import data.IDataOperations;
import data.ServerType;
import util.RMIAccess;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class CentralOperations extends UnicastRemoteObject implements ICentralOperations {

    private List<RMIAccess<IChatroomOperations>> chatroomNodes;
    private Object chatroomNodeLock;
    private List<RMIAccess<IDataOperations>> dataNodes;
    private Object dataNodeLock;

    public CentralOperations(List<RMIAccess<IChatroomOperations>> chatroomNodes,
                             Object chatroomNodeLock,
                             List<RMIAccess<IDataOperations>> dataNodes,
                             Object dataNodeLock) throws RemoteException {
        this.chatroomNodes = chatroomNodes;
        this.chatroomNodeLock = chatroomNodeLock;
        this.dataNodes = dataNodes;
        this.dataNodeLock = dataNodeLock;
    }

    @Override
    public void register(ServerType serverType, String hostname, int port) throws RemoteException {

        if (serverType == ServerType.CHATROOM) {
            synchronized (chatroomNodeLock) {
                chatroomNodes.add(new RMIAccess<>(hostname, port, "IChatroomOperations"));
            }
        } else {
            synchronized (dataNodeLock) {
                dataNodes.add(new RMIAccess<>(hostname, port, "IDataOperations"));
            }
        }

    }
}

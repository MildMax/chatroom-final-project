package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// chatroom node -> server node
// data node -> server node

public interface ICentralOperations extends Remote {

    void register(ServerType serverType, String hostname, int port) throws RemoteException;

}

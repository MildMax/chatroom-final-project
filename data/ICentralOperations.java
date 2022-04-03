package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// chatroom node -> server node
// data node -> server node

public interface ICentralOperations extends Remote {

    void registerDataNode(String hostname, int dataOperationsPort, int dataParticipantPort) throws RemoteException;
    void registerChatNode(String hostname, int port) throws RemoteException;

}

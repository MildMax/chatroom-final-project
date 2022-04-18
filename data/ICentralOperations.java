package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// chatroom node -> server node
// data node -> server node

public interface ICentralOperations extends Remote {

    RegisterResponse registerDataNode(String hostname, int dataOperationsPort, int dataParticipantPort) throws RemoteException;
    RegisterResponse registerChatNode(String hostname, int port) throws RemoteException;
    long getServerTime() throws RemoteException;

}

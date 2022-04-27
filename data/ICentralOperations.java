package data;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// chatroom node -> server node
// data node -> server node

public interface ICentralOperations extends Remote {

    RegisterResponse registerDataNode(String hostname, int dataOperationsPort, int dataParticipantPort, List<String> chatrooms) throws RemoteException;
    RegisterResponse registerChatNode(String hostname, int port) throws RemoteException;
    long getServerTime() throws RemoteException;

}

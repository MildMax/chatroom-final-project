package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// client -> chatroom

public interface IChatroomUserOperations extends Remote {

    void chat(String username, String message) throws RemoteException;

}

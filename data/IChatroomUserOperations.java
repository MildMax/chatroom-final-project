package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IChatroomUserOperations extends Remote {

    void chat(String username, String message) throws RemoteException;

}

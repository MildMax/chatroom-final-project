package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// centralized server -> data node

public interface IDataOperations extends Remote {
    Response verifyUser(String username, String password) throws RemoteException;
}

package data;

import java.rmi.RemoteException;

// centralized server -> data node

public interface IDataOperations {
    Response login(String username, String password) throws RemoteException;
    Response verifyUser(String username, String password) throws RemoteException;
}

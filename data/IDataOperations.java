package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// centralized server -> data node

public interface IDataOperations extends Remote {
    Response verifyUser(String username, String password) throws RemoteException;
    
    /**
     * Checks if a user already exists
     * @param username to search for
     * @return true or false
     * @throws RemoteException
     */
    boolean userExists(String username) throws RemoteException;
}

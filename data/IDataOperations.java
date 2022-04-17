package data;

import java.nio.file.Path;
import java.rmi.Remote;
import java.rmi.RemoteException;

// centralized server -> data node

public interface IDataOperations extends Remote {

    Response verifyUser(String username, String password) throws RemoteException;
    
    /**
     * Verifys that a specific user owns the chatroom
     * @param chatroomName chatroom name
     * @param username 
     * @return Response if its success or fail
     * @throws RemoteException
     */
    Response verifyOwnership(String chatroomName, String username) throws RemoteException;

    /**
     * Checks if a user already exists
     * @param username to search for
     * @return true or false
     * @throws RemoteException
     */
    boolean userExists(String username) throws RemoteException;
    
    /**
     * Checks if a chatroom already exists
     * @param chatroom to search for
     * @return true or false
     * @throws RemoteException
     */
    boolean chatroomExists(String chatroom) throws RemoteException;

}

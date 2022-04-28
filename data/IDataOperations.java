package data;

import java.nio.file.Path;
import java.rmi.Remote;
import java.rmi.RemoteException;

// centralized server -> data node
/**
 * Defines methods that a data server should implement to facilitate central server
 * requests
 */
public interface IDataOperations extends Remote {

  /**
   * Verifies that a user exists and that the provided password matches what is on file for the user
   *
   * @param username the user to verify
   * @param password the password to be checked against the user info on file
   * @return a response indicating whether the user has been verified or not
   * @throws RemoteException if there is an error during remote communication
   */
  Response verifyUser(String username, String password) throws RemoteException;

  /**
   * Verifies that a user is the original creater and owner of a chatroom
   *
   * @param chatroomName the name of the chatroom to check the owner of
   * @param username the username to be verified as the owner of the chatroom
   * @return a response indicating whether the user is the owner of the chatroom or not
   * @throws RemoteException if there is an error during remote communication
   */
  Response verifyOwnership(String chatroomName, String username) throws RemoteException;

  /**
   * Determines whether a username exists in the system
   *
   * @param username the username to check
   * @return true if user exists, false otherwise
   * @throws RemoteException if there is an error during remote communication
   */
  boolean userExists(String username) throws RemoteException;

  /**
   * Determines whether a chatroom exists in the system
   *
   * @param chatroom the chatroom to check
   * @return true if the chatroom exists, false otherwise
   * @throws RemoteException if there is an error during remote communication
   */
  boolean chatroomExists(String chatroom) throws RemoteException;

}

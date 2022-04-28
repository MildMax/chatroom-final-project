package data;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


/**
 * Defines methods that the central server should provide to facilitate user
 * requests
 */

public interface ICentralUserOperations extends Remote {

  /**
   * Registers a user with the system. Initiates two phase commit when trying to create user.
   *
   * @param username name of the user
   * @param password password for the user account
   * @return a response indicating whether the operation succeeded or failed
   * @throws RemoteException if there is an error during remote communication
   */
  Response registerUser(String username, String password) throws RemoteException;

  /**
   * Logs a user into the chatroom application
   *
   * @param username name of the user's account
   * @param password password for the user account
   * @return a response whether the login failed or succeeded
   * @throws RemoteException if there is an error during remote communication
   */
  Response login(String username, String password) throws RemoteException;

  /**
   * Gets a list of available chatrooms from chatroom servers in the system
   *
   * @return a list of available chatrooms in the system
   * @throws RemoteException if there is an error during remote communication
   */
  ChatroomListResponse listChatrooms() throws RemoteException;

  /**
   * Initiates the creation of a chatroom on behalf of a client. Creates chatroom using
   * two phase commit
   *
   * @param chatroomName name of the chatroom to create
   * @param username name of the user creating the chatroom
   * @return a response containing information about the server hosting the chatroom if the operation succeeds,
   *         otherwise indicates operation failed
   * @throws RemoteException if there is an error during remote communication
   */
  ChatroomResponse createChatroom(String chatroomName, String username) throws RemoteException;

  /**
   * Gets location and port information pertaining to a chatroom in the system
   *
   * @param chatroomName name of the chatroom to find
   * @return hostname and ports for the chatroom if it exists, otherwise indicates the operation failed
   * @throws RemoteException if there is an error during remote communication
   */
  ChatroomResponse getChatroom(String chatroomName) throws RemoteException;

  /**
   * Deletes a chatroom from the system. Uses two phase commit
   *
   * @param chatroomName name of the chatroom to delete
   * @param username name of the user requesting to delete the chatroom
   * @param password password for the provided user
   * @return a response indicating whether the operation succeeded or failed
   * @throws RemoteException if there is an error during remote communication
   */
  Response deleteChatroom(String chatroomName, String username, 
      String password) throws RemoteException;

  /**
   * Reestablishes a chatroom on an available server node if the original node has crashed
   *
   * @param chatroomName name of the chatroom to reestablish
   * @param username name of the user that lost connection to the chatroom server
   * @return address and port information for the new chat server if success, otherwise indicates process failed
   * @throws RemoteException if there is an error during remote communication
   */
  ChatroomResponse reestablishChatroom(String chatroomName, String username) throws RemoteException;

}

package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines operations that a chat server should provide to facilitate
 * central server operation requests
 */
public interface IChatroomOperations extends Remote {

  /**
   * Creates a chatroom at the local chat server
   *
   * @param name name of the chatroom to create
   * @return a response indicating whether the operation has succeeded or failed
   * @throws RemoteException if there is an error during remote communication
   */
  Response createChatroom(String name) throws RemoteException;

  /**
   * Deletes a chatroom from the local chat server
   *
   * @param name name of the chatroom to delete
   * @return a response indicating whether the operation has succeeded or failed
   * @throws RemoteException if there is an error during remote communication
   */
  Response deleteChatroom(String name) throws RemoteException;

  /**
   * Provides information regarding the number of chatrooms and users hosted on the current
   * chat server
   *
   * @return a response containing information regarding the number of chatrooms and users hosted on the
   *         current chat server
   * @throws RemoteException if there is an error during remote communication
   */
  ChatroomDataResponse getChatroomData() throws RemoteException;

  /**
   * Gets a list of names for chatrooms hosted at the local chat server
   *
   * @return a response containing a list of names for chatrooms hosted at the local chat server
   * @throws RemoteException if there is an error during remote communication
   */
  ChatroomListResponse getChatrooms() throws RemoteException;
}

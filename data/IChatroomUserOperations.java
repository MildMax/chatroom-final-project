package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// client -> chatroom

/**
 * Defines methods that a chat server should provide to facilitate user requests
 */
public interface IChatroomUserOperations extends Remote {

  /**
   * Publishes a message from a user to the appropriate chatroom
   *
   * @param chatroomName name of the chat room to publish the message to
   * @param username name of the user publishing the message
   * @param message the message to be published
   * @throws RemoteException if there is an error during remote communication
   */
  void chat(String chatroomName, String username, String message) throws RemoteException;

  /**
   * Indicates to a chatroom that a user has joined the chat
   *
   * @param chatroomName name of the chat room the user has joined
   * @param username name of the user that joined the chatroom
   * @throws RemoteException if there is an error during remote communication
   */
  void joinChatroom(String chatroomName, String username) throws RemoteException;

  /**
   * Unsubscribes a user from a chatroom and publishes a leave message to remaining users
   *
   * @param chatroomName name of the chat room the user is leaving
   * @param username name of the user leaving the chatroom
   * @throws RemoteException if there is an error during remote communication
   */
  void leaveChatroom(String chatroomName, String username) throws RemoteException;

}

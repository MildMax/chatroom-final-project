package data;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Defines methods that the central server should implement to receive chat server requests on
 */

public interface ICentralChatroomOperations extends Remote {

  /**
   * Logs a chatroom message to available data servers in the system using 2pc
   *
   * @param chatroom name of the chat room the message was sent to
   * @param message the message to be logged
   * @return a response indicating whether the operation succeeded or not
   * @throws RemoteException if there is an error during remote communication
   */
  Response logChatMessage(String chatroom, String message) throws RemoteException;
}

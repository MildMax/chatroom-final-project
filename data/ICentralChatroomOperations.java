package data;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Centrallized chatroom server Interface.
 *
 */

public interface ICentralChatroomOperations extends Remote {

  /**
   * logs the response message of the chatroom for fault tolerance.
   * @param chatroom name of the chat room.
   * @param message message 
   * @return the logs 
   * @throws RemoteException handles remote object invocations
   */
  Response logChatMessage(String chatroom, String message) throws RemoteException;
}

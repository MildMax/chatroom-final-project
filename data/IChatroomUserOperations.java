package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// client -> chatroom

/**
 * centralized inititation of chatroom user operations.
 *
 */
public interface IChatroomUserOperations extends Remote {

  /**
   * chat using tje chatroomname username message.
   * @param chatroomName name of the chat room
   * @param username name of the user
   * @param message message
   * @throws RemoteException handles remote object invocations.
   */
  void chat(String chatroomName, String username, String message) throws RemoteException;
  
  /**
   * 
   * @param chatroomName name of the chat room.
   * @param username name of the user
   * @throws RemoteException handles remote object invocations.
   */
  void joinChatroom(String chatroomName, String username) throws RemoteException;
  
  /**
   * 
   * @param chatroomName name of the chat room.
   * @param username name of the user
   * @throws RemoteException handles remote object invocations.
   */
  void leaveChatroom(String chatroomName, String username) throws RemoteException;

}

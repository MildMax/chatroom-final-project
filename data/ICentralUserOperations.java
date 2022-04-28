package data;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


/**
 * centralized initiation of all the user operations.
 *
 */

public interface ICentralUserOperations extends Remote {

  /**
   * register user if they are new users.
   * @param username name of the user
   * @param password password 
   * @return sucess or failure message
   * @throws RemoteException handles remote object invocations.
   */
  Response registerUser(String username, String password) throws RemoteException;

  /**
   * login the user by validating the username and password.
   * @param username name of the user
   * @param password password
   * @return sucess or failure message.
   * @throws RemoteException handles remote object invocations.
   */
  Response login(String username, String password) throws RemoteException;

  /**
   * list of chat rooms.
   * @return list of chat rooms
   * @throws RemoteException handles remote object invocations.
   */
  ChatroomListResponse listChatrooms() throws RemoteException;

  /**
   * creating a ne=w chatroom.
   * @param chatroomName name of the chatroom
   * @param username name of the user
   * @return sucess or faiure message
   * @throws RemoteException handles remote object invocations.
   */
  ChatroomResponse createChatroom(String chatroomName, String username) throws RemoteException;

  /**
   * the information about the chatroom.
   * @param chatroomName name of the chatroom
   * @return he details
   * @throws RemoteException handles remote object invocations.
   */
  ChatroomResponse getChatroom(String chatroomName) throws RemoteException;

  /**
   * deleting chat room.
   * @param chatroomName name of the chatroom
   * @param username name of the user
   * @param password password
   * @return the sucess or failure response
   * @throws RemoteException handles remote object invocations.
   */
  Response deleteChatroom(String chatroomName, String username, 
      String password) throws RemoteException;

  /**
   * reestablishing chatroom.
   * @param chatroomName name of the chatroom
   * @param username name of the user
   * @return newly established chatroom
   * @throws RemoteException handles remote object invocations.
   */
  ChatroomResponse reestablishChatroom(String chatroomName, String username) throws RemoteException;

}

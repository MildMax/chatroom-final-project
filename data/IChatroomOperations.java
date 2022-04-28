package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// centralized server -> central server
/**
 * centralized initialization of all the chatroomoperations.
 *
 */
public interface IChatroomOperations extends Remote {

  /**
   * create chat room.
   * @param name name of the chatroom
   * @return sucess or failure message.
   * @throws RemoteException handles remote object invocations.
   */
  Response createChatroom(String name) throws RemoteException;
  
  /**
   * delete chatroom.
   * @param name name of the chatroom
   * @return sucess or failure message.
   * @throws RemoteException handles remote object invocations.
   */
  Response deleteChatroom(String name) throws RemoteException;
  
  /**
   * chatroomdate.
   * @return reesponse of the chatroom data.
   * @throws RemoteException handles remote object invocations.
   */
  ChatroomDataResponse getChatroomData() throws RemoteException;
  
  /**
   * list of chatrooms.
   * @return list of chatrooms.
   */
  
  ChatroomListResponse getChatrooms() throws RemoteException;
}

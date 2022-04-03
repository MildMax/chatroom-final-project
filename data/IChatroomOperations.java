package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// centralized server -> central server

public interface IChatroomOperations extends Remote {

    Response createChatroom(String name, String username) throws RemoteException;
    Response deleteChatroom(String name) throws RemoteException;
    ChatroomDataResponse getChatroomData() throws RemoteException;
    ChatroomListResponse getChatrooms() throws RemoteException;
    ChatroomUserResponse getChatroom(String name) throws RemoteException;

}

package data;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// client -> centralized server

public interface ICentralUserOperations extends Remote {

    Response registerUser(String username, String password) throws RemoteException;
    Response login(String username, String password) throws RemoteException;
    ChatroomListResponse listChatrooms() throws RemoteException;
    ChatroomResponse createChatroom(String chatroomName, String username) throws RemoteException;
    ChatroomResponse getChatroom(String chatroomName) throws RemoteException;
    Response deleteChatroom(String chatroomName, String username, String password) throws RemoteException;
    ChatroomResponse reestablishChatroom(String chatroomName, String username) throws RemoteException;

}

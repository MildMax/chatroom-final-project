package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// client -> chatroom

public interface IChatroomUserOperations extends Remote {

    void chat(String chatroomName, String username, String message) throws RemoteException;
    void joinChatroom(String chatroomName, String username) throws RemoteException;
    void leaveChatroom(String chatroomName, String username) throws RemoteException;

}

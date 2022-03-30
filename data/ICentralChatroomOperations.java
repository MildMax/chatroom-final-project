package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICentralChatroomOperations extends Remote {

    void logChatMessage(String chatroom, String message) throws RemoteException;
    void logChatroom(String name) throws RemoteException;
    void logUser(String username) throws RemoteException;
}

package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// chatroom server -> centralized server

public interface ICentralChatroomOperations extends Remote {

    Response logChatMessage(String chatroom, String message) throws RemoteException;
}

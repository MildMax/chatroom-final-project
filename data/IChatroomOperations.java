package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// centralized server -> central server

public interface IChatroomOperations extends Remote {

    Response createChatroom(String name) throws RemoteException;
    ChatroomDataResponse getChatroomData() throws RemoteException;

}

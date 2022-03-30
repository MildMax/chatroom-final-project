package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IChatroomOperations extends Remote {

    Response createChatroom(String name) throws RemoteException;
    ChatroomDataResponse getChatroomData() throws RemoteException;

}

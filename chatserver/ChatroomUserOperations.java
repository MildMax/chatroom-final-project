package chatserver;

import data.ICentralChatroomOperations;
import data.IChatroomUserOperations;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class ChatroomUserOperations extends UnicastRemoteObject implements IChatroomUserOperations {

    private final Map<String, Chatroom> roomList;
    private final Object roomListLock;
    private final RMIAccess<ICentralChatroomOperations> centralServerAccessor;
    private final Object serverAccessorLock;

    public ChatroomUserOperations(Map<String, Chatroom> roomList, Object roomListLock, ServerInfo serverInfo, int centralServerPort) throws RemoteException {
        this.roomList = roomList;
        this.roomListLock = roomListLock;
        this.centralServerAccessor = new RMIAccess<>(serverInfo.getCentralServerHostname(), centralServerPort, "ICentralChatroomOperations");
        this.serverAccessorLock = new Object();
    }

    @Override
    public void chat(String chatroomName, String username, String message) throws RemoteException {
        synchronized (serverAccessorLock) {
            synchronized (roomListLock) {
                // do pub here
            }

            try {
                centralServerAccessor.getAccess().logChatMessage(chatroomName, message);
            } catch (NotBoundException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to contact central server at \"%s:%d\"",
                        centralServerAccessor.getHostname(),
                        centralServerAccessor.getPort()
                ));
            }
        }
    }

    @Override
    public void joinChatroom(String chatroomName, String username) throws RemoteException {

        synchronized (roomListLock) {
            // initiate join message here
            // just write "<username> has entered the chat"
            // use pub for join message
        }

    }

    @Override
    public void leaveChatroom(String chatroomName, String username) throws RemoteException {

        synchronized (roomListLock) {
            // initiate leave message here
            // just write "<username> has left the chat"
            // use pub for leave message

            // find a way to associate usernames with sockets
            // Maybe some kind of Pair class? An associator?
            // will have to implement on initial chatroom message via TCP connection from client
            // do later
        }

    }
}

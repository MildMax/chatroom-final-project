package chatserver;

import data.ICentralChatroomOperations;
import data.IChatroomUserOperations;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
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
                Chatroom chatroom = roomList.get(chatroomName);
                chatroom.Publish(message);
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
}

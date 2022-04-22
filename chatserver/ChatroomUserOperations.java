package chatserver;

import data.ICentralChatroomOperations;
import data.IChatroomUserOperations;
import data.Response;
import data.ResponseStatus;
import util.ClientIPUtil;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import javax.swing.plaf.TableHeaderUI;
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

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received chat message for chatroom \"%s\" from user \"%s\" on message \"%s\" at \"%s\"",
                chatroomName,
                username,
                message,
                ClientIPUtil.getClientIP()
        ));

        synchronized (serverAccessorLock) {
            synchronized (roomListLock) {
                Chatroom chatroom = roomList.get(chatroomName);
                chatroom.Publish(message);
            }

            try {

                Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "Attempting to log message \"%s\" from user \"%s\" at \"%s\" for chatroom \"%s\"",
                        message,
                        username,
                        ClientIPUtil.getClientIP(),
                        message
                ));

                boolean success = false;
                // retry log until it succeeds
                while (!success) {

                    Response r = centralServerAccessor.getAccess().logChatMessage(chatroomName, message);
                    if (r.getStatus() == ResponseStatus.OK) {
                        success = true;
                    } else {
                        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                "Failed to log message \"%s\" from user \"%s\" at \"%s\" for chatroom \"%s\", retrying...",
                                message,
                                username,
                                ClientIPUtil.getClientIP(),
                                message
                        ));
                    }
                }

                Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "Successfully logged message \"%s\" from user \"%s\" at \"%s\" for chatroom \"%s\"",
                        message,
                        username,
                        ClientIPUtil.getClientIP(),
                        message
                ));
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

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received joinChatroom message for user \"%s\" at \"%s\" in chatroom \"%s\"",
                username,
                ClientIPUtil.getClientIP(),
                chatroomName
        ));

        synchronized (roomListLock) {
            // initiate join message here
            // just write "<username> has entered the chat"
            // use pub for join message
        }

    }

    @Override
    public void leaveChatroom(String chatroomName, String username) throws RemoteException {

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received leaveChatroom message for user \"%s\" at \"%s\" in chatroom \"%s\"",
                username,
                ClientIPUtil.getClientIP(),
                chatroomName
        ));

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

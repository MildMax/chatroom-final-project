package chatserver;

import data.ICentralChatroomOperations;
import data.IChatroomUserOperations;
import data.Response;
import data.ResponseStatus;
import util.ClientIPUtil;
import util.CristiansLogger;
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
    private final Object logMessageLock;

    public ChatroomUserOperations(Map<String, Chatroom> roomList, Object roomListLock, ServerInfo serverInfo, int centralServerPort) throws RemoteException {
        this.roomList = roomList;
        this.roomListLock = roomListLock;
        this.centralServerAccessor = new RMIAccess<>(serverInfo.getCentralServerHostname(), centralServerPort, "ICentralChatroomOperations");
        this.serverAccessorLock = new Object();
        this.logMessageLock = new Object();
    }

    @Override
    public void chat(String chatroomName, String username, String message) throws RemoteException {

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received chat message for chatroom \"%s\" from user \"%s\" on message \"%s\" at \"%s\"",
                chatroomName,
                username,
                message,
                ClientIPUtil.getClientIP()
        ));

        // publish the message along with the user's name to all of the users subscribed to the given chatroom
        synchronized (serverAccessorLock) {
            synchronized (roomListLock) {
                // get the chatroom to publish the message to
                Chatroom chatroom = this.roomList.get(chatroomName);

                // if the  returned chatroom object is not null, publish the message
                if (chatroom != null) {
                    chatroom.publish(username + " >> " + message);
                }
                // otherwise, log that the chatroom the user is trying to publish to is nonexistent and return
                // out of the method
                else {
                    CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "User \"%s\" attempted to publish message \"%s\" to non-existent chatroom \"%s\"",
                            username,
                            message,
                            chatroomName
                    ));
                    return;
                }
            }
        }

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Attempting to log message \"%s\" from user \"%s\" at \"%s\" for chatroom \"%s\"",
                message,
                username,
                ClientIPUtil.getClientIP(),
                message
        ));

        // once the message has been published, log the message with the central server
        synchronized (this.logMessageLock) {
            try {
                boolean success = false;
                // retry logging the message until it succeeds
                while (!success) {

                    Response r = centralServerAccessor.getAccess().logChatMessage(chatroomName, username + " >> " + message);

                    // if the request succeeds, set success to true to break out of the retry loop
                    if (r.getStatus() == ResponseStatus.OK) {
                        success = true;
                    }
                    // otherwise log that the attempt failed and continue trying
                    else {
                        CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                "Failed to log message \"%s\" from user \"%s\" at \"%s\" for chatroom \"%s\", retrying...",
                                message,
                                username,
                                ClientIPUtil.getClientIP(),
                                message
                        ));
                    }
                }
            } catch (NotBoundException e) {
                CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to contact central server at \"%s:%d\"",
                        centralServerAccessor.getHostname(),
                        centralServerAccessor.getPort()
                ));
            }
        }

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully logged message \"%s\" from user \"%s\" at \"%s\" for chatroom \"%s\"",
                message,
                username,
                ClientIPUtil.getClientIP(),
                chatroomName
        ));
    }

    @Override
    public void joinChatroom(String chatroomName, String username) throws RemoteException {

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received joinChatroom message for user \"%s\" at \"%s\" in chatroom \"%s\"",
                username,
                ClientIPUtil.getClientIP(),
                chatroomName
        ));

        synchronized (roomListLock) {
            // get the chatroom to publish the message to
            Chatroom chatroom = this.roomList.get(chatroomName);
            // if the chatroom is not null, it exists, publish join message
            if (chatroom != null) {
                chatroom.publish("System >> " + username + " has joined the chat");
            }
            // otherwise log the failure
            else {
                CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "User \"%s\" attempted to issue a join chatroom message to non-existent chatroom \"%s\"",
                        username,
                        chatroomName
                ));
            }
        }

    }

    @Override
    public void leaveChatroom(String chatroomName, String username) throws RemoteException {

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received leaveChatroom message for user \"%s\" at \"%s\" in chatroom \"%s\"",
                username,
                ClientIPUtil.getClientIP(),
                chatroomName
        ));

        synchronized (roomListLock) {
            // get the chatroom that the user wishes to leave
            Chatroom chatroom = this.roomList.get(chatroomName);
            if (chatroom != null) {
                // if the chatroom is not null, unsubscribe the user from the chatroom
                // and publish the leave chatroom message to the remaining subscribers
                chatroom.unsubscribe(username);
                chatroom.publish("System >> " + username + " has left the chat");
            } else {
                CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "User \"%s\" attempted to leave non-existent chatroom \"%s\"",
                        username,
                        chatroomName
                ));
            }
        }

    }
}

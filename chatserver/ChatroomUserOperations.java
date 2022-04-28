package chatserver;

import data.ICentralChatroomOperations;
import data.IChatroomUserOperations;
import data.Response;
import data.ResponseStatus;
import util.ClientIPUtil;
import util.CristiansLogger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

/**
 * Implements the IChatroomUserOperations and is responsible
 * for fulfilling user requests
 */
public class ChatroomUserOperations extends UnicastRemoteObject implements IChatroomUserOperations {

  private final Map<String, Chatroom> roomMap;
  private final Object roomMapLock;
  private final RMIAccess<ICentralChatroomOperations> centralServerAccessor;
  private final Object serverAccessorLock;
  private final Object logMessageLock;

  /**
   * Creates an instance of the ChatroomUserOperations engine
   *
   * @param roomMap map of all the chatrooms and their names at the local chat server
   * @param roomMapLock locks the roomMap resource
   * @param serverInfo provides all addressing and port information for the local chat server
   * @param centralServerPort port where central server is accepting chatroom operation requests
   * @throws RemoteException if there is an error during remote communication
   */
  public ChatroomUserOperations(Map<String, Chatroom> roomMap,
                                Object roomMapLock, ServerInfo serverInfo, int centralServerPort) throws RemoteException {
    this.roomMap = roomMap;
    this.roomMapLock = roomMapLock;
    this.centralServerAccessor = new RMIAccess<>(serverInfo.getCentralServerHostname(), 
        centralServerPort, "ICentralChatroomOperations");
    this.serverAccessorLock = new Object();
    this.logMessageLock = new Object();
  }

  /**
   * Publishes a message from a user to the appropriate chatroom
   *
   * @param chatroomName name of the chat room to publish the message to
   * @param username name of the user publishing the message
   * @param message the message to be published
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public void chat(String chatroomName, String username, String message) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received chat message for chatroom \"%s\" from user \"%s\" on message \"%s\" at \"%s\"",
        chatroomName,
        username,
        message,
        ClientIPUtil.getClientIP()
        ));

    // publish the message along with the user's name to all of the 
    //users subscribed to the given chatroom
    synchronized (serverAccessorLock) {
      synchronized (roomMapLock) {
        // get the chatroom to publish the message to
        Chatroom chatroom = this.roomMap.get(chatroomName);

        // if the  returned chatroom object is not null, publish the message
        if (chatroom != null) {
          chatroom.publish(username + " >> " + message);
        } else {
          // otherwise, log that the chatroom the user is trying to publish 
          //to is nonexistent and return
          // out of the method
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

          Response r = centralServerAccessor.getAccess().logChatMessage(chatroomName, 
              username + " >> " + message);

          // if the request succeeds, set success to true to break out of the retry loop
          if (r.getStatus() == ResponseStatus.OK) {
            success = true;
            // otherwise log that the attempt failed and continue trying
          } else {
            CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                "Failed to log message \"%s\" from user \"%s\" at \"%s\" for chatroom \"%s\", "
                    + "retrying...",
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

  /**
   * Indicates to a chatroom that a user has joined the chat
   *
   * @param chatroomName name of the chat room the user has joined
   * @param username name of the user that joined the chatroom
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public void joinChatroom(String chatroomName, String username) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received joinChatroom message for user \"%s\" at \"%s\" in chatroom \"%s\"",
        username,
        ClientIPUtil.getClientIP(),
        chatroomName
        ));

    synchronized (roomMapLock) {
      // get the chatroom to publish the message to
      Chatroom chatroom = this.roomMap.get(chatroomName);
      // if the chatroom is not null, it exists, publish join message
      if (chatroom != null) {
        chatroom.publish("System >> " + username + " has joined the chat");
        // otherwise log the failure
      } else {
        CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "User \"%s\" attempted to issue a join chatroom message to "
                + "non-existent chatroom \"%s\"",
                username,
                chatroomName
            ));
      }
    }

  }

  /**
   * Unsubscribes a user from a chatroom and publishes a leave message to remaining users
   *
   * @param chatroomName name of the chat room the user is leaving
   * @param username name of the user leaving the chatroom
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public void leaveChatroom(String chatroomName, String username) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received leaveChatroom message for user \"%s\" at \"%s\" in chatroom \"%s\"",
        username,
        ClientIPUtil.getClientIP(),
        chatroomName
        ));

    synchronized (roomMapLock) {
      // get the chatroom that the user wishes to leave
      Chatroom chatroom = this.roomMap.get(chatroomName);
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

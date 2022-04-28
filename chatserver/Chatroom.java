package chatserver;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import util.CristiansLogger;
import util.ThreadSafeStringFormatter;

/**
 * Defines operations required to facilitate a chatroom on a chat server
 */
public class Chatroom {

  private final Map<String, Socket> socketMap;
  private final Object socketMapLock;
  private final String roomName;

  /**
   * constructor of Chatroom which accepts the name which is the unique
   * identifier for each room.
   *
   * @param roomName name of the chat room
   */
  public Chatroom(String roomName) {
    this.socketMap = new HashMap<>();
    this.socketMapLock = new Object();
    this.roomName = roomName;
  }

  /**
   * Subscribes a user to a chatroom using its username and an instance of the socket.
   *
   * @param s socket instance
   * @param username name of the user to associate with the socket
   */
  public void subscribe(Socket s, String username) {
    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Subscribing client \"%s\" to chatroom \"%s\"",
        username,
        this.roomName
        ));

    // associate the socket with the user's username so it can be 
    //retrieved when the user leaves the chatroom
    synchronized (socketMapLock) {
      this.socketMap.put(username, s);
    }
  }

  /**
   * Unsubscribes a user and their associated socket from the chatroom
   *
   * @param username name of the user to be unsubscribed from the chatroom
   */
  public void unsubscribe(String username) {
    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Unsubscribing client \"%s\" from chatroom \"%s\"",
        username,
        this.roomName
        ));

    // locate the socket associated with the user using their username and remove the socket
    // from the socket map so the user no longer receives published messages
    synchronized (socketMapLock) {
      Socket s = socketMap.get(username);
      try {
        s.close();
      } catch (IOException e) {
        CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "There was an error closing the socket for user \"%s\""
            ));
      }
      this.socketMap.remove(username);
    }
  }

  /**
   * Publishes a message to all subscribers in the chatroom
   *
   * @param message the message which should be published to all subscribers
   */
  public void publish(String message) {
    synchronized (socketMapLock) {
      // iterate through the socket map and notify each user via their dedicated socket
      for (String user : socketMap.keySet()) {
        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
            "Publishing message \"%s\" to user \"%s\"",
            message,
            user
            ));
        PrintWriter out = null;
        try {
          // write message to socket
          out = 
              new PrintWriter(new OutputStreamWriter(socketMap.get(user).getOutputStream()), true);
          out.println(message);
        } catch (IOException e) {
          CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
              "Unable to publish message \"%s\" to client \"%s\"",
              message,
              user
              ));
        }

      }
    }
  }

  /**
   * Closes the chatroom and informs connected clients the chatroom is no longer available.
   */
  public void closeRoom() {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Closing room \"%s\"",
        this.roomName
        ));

    synchronized (socketMapLock) {
      for (String user : socketMap.keySet()) {
        try {
          Socket s = socketMap.get(user);
          // if socket is null, do no send
          if (s == null) {
            continue;
          }

          CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Sending close instruction to user \"%s\"",
              user
              ));

          // send \c termination string to client to indicate the chatroom is closed
          PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
          out.println("\\c");
          // clean up resources
          out.close();
          s.close();
        } catch (IOException e) {
          CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
              "Unable to send close message to client \"%s\"",
              user
              ));
        }
      }
    }
  }

  /**
   * The count of users currently subscribed to the chatroom
   *
   * @return number of users subscribed to the chatroom
   */
  public int getUserCount() {
    // retrieve the number of users currently subscribed to this chatroom
    // used for load balancing purposes
    synchronized (socketMapLock) {
      return socketMap.size();
    }
  }
}

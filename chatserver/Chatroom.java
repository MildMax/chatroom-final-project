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
 * Each object of the chatroom is instance of the Chatroom class and shares
 * the following features among them.
 * @author hephzibahsaidu
 *
 */
public class Chatroom {

  private final Map<String, Socket> socketMap;
  private final Object socketMapLock;
  private final String roomName;

  /**
   * constructor of Chatroom which accepts the name which is the unique
   * identifier for each room.
   * @param roomName name of the chat room
   */
  public Chatroom(String roomName) {
    this.socketMap = new HashMap<>();
    this.socketMapLock = new Object();
    this.roomName = roomName;
  }

  /**
   * chatroom can subscribe using its username and an instance of the socket.
   * @param s socket instance
   * @param username name of the chatroom
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
   * in order to unsubscribe from the chatroom this methos is useful which.
   * accepts the unique idnetified of the chatroom
   * @param username name of the chatroom
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
   * this method is used to publish the communictaion among the chatroom participants
   * using sockets the messages are being published in the network such that
   * the messages are acessed by the participants.
   * @param message the message which should be published.
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
   * The count of users currently subscribed to this chatroom.
   * @return number of users.
   */
  public int getUserCount() {
    // retrieve the number of users currently subscribed to this chatroom
    // used for load balancing purposes
    synchronized (socketMapLock) {
      return socketMap.size();
    }
  }
}

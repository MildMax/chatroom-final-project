package chatserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import util.CristiansLogger;
import util.Logger;
import util.ThreadSafeStringFormatter;

/**
 * Accepts client TCP connections and associates them with the correct chatroom at the local chat server
 */
public class ConnectChatroom extends Thread {

  private final int tcpPort;
  private final Map<String, Chatroom> roomMap;
  private final Object roomMapLock;

  /**
   * Creates an instance of the ConnectChatroom thread
   *
   * @param tcpPort port number the chat server accepts client TCP connections on
   * @param roomMap a map containing available chatrooms at the server
   * @param roomMapLock locks the roomMap resource
   */
  public ConnectChatroom(int tcpPort, 
      Map<String, Chatroom> roomMap, Object roomMapLock) {
    this.tcpPort = tcpPort;
    this.roomMap = roomMap;
    this.roomMapLock = roomMapLock;
  }

  /**
   * Continuously accepts client TCP connections for the duration of the chat server's execution and
   * subscribes each client socket with the appropriate chatroom
   */
  @Override
  public void run() {

    // create a new server socket to accept incoming client TCP connections
    ServerSocket serverSocket;
    try {
      serverSocket = new ServerSocket(this.tcpPort);
    } catch (IOException e) {
      CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Unable to establish TCP server socket on port \"%d\"",
          this.tcpPort
          ));
      return;
    }

    // run for duration of program
    while (true) {

      Socket clientSocket;
      try {
        // accept the client connection and track the new client socket in clientSocket
        clientSocket = serverSocket.accept();
        // set up reader and writer for the socket for initial communication with client
        PrintWriter out = 
            new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
        BufferedReader in = 
            new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
            "Received TCP connection from client at \"%s:%d\"",
            clientSocket.getInetAddress().getHostAddress(),
            clientSocket.getPort()
            ));

        // get the inital client message
        // in the format <chatroom>:<username>
        // <chatroom> is used to find the correct chatroom
        // <username> is used to associate the client socket with the appropriate user
        String clientMessage = in.readLine();
        String[] vals = clientMessage.split(":");

        // if the length of vals is not 2, it is either missing or has extra argument
        // log the error and continue receiving new connections
        if (vals.length != 2) {

          CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
              "Initial client message \"%s\" did not meet <chatroom>:<user> format",
              clientMessage
              ));

          // indicate to the client that they cannot be subscribed to a chatroom
          out.println("fail");
          // close resources associated with the socket
          in.close();
          out.close();
          clientSocket.close();
          continue;
        }

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
            "Received subscribe request from user \"%s\" for chatroom \"%s\"",
            vals[1],
            vals[0]
            ));

        synchronized (this.roomMapLock) {
          // get the room the user wants to subscribe to
          Chatroom chatroom = roomMap.get(vals[0]);

          // if the chatroom is null, it does not exist, log the error and continue receiving
          // new client connections
          if (chatroom == null) {

            CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                "Client \"%s\" attempted to subscribe to non-existent chatroom \"%s\"",
                vals[1],
                vals[0]
                ));

            // indicate to client that the connection request failed
            out.println("fail");
            // close resources associated with the client socket
            in.close();
            out.close();
            clientSocket.close();
            continue;
          }
          // if chatroom is not null, subscribe the client to the chatroom
          chatroom.subscribe(clientSocket, vals[1]);
        }

        out.println("success");
      } catch (IOException e) {
        CristiansLogger.writeErrorToLog("Unable to receive client connection on server socket");
      }
    }

  }
}

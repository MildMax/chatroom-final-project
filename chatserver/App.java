package chatserver;

import data.ICentralOperations;
import data.IChatroomOperations;
import data.IChatroomUserOperations;
import data.RegisterResponse;
import util.CristiansLogger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Initiates the chat server
 */
public class App {

  private final Map<String, Chatroom> roomMap;
  private final Object roomMapLock;

  /**
   * Creates an instance of the chat server App object
   */
  public App() {
    this.roomMap = Collections.synchronizedMap(new HashMap<>());
    this.roomMapLock = new Object();
  }

  /**
   * Starts the chat server
   *
   * @param serverInfo information about RMI registry ports
   * @throws RemoteException if there is an error setting up RMI registries
   * @throws NotBoundException if there is an error contacting RMI registries
   */
  public void go(ServerInfo serverInfo) throws RemoteException, NotBoundException {

    // register Data node with the central server
    RMIAccess<ICentralOperations> centralServer = 
        new RMIAccess<>(serverInfo.getCentralServerHostname(),
        serverInfo.getCentralServerPort(), "ICentralOperations");

    CristiansLogger.writeMessageToLog("Initiating Cristian's algorithm...");
    // initiate Cristians algorithm thread
    CristiansLogger.setCentralAccessor(centralServer);
    Thread t = new Thread(new CristiansLogger());
    t.start();

    CristiansLogger.writeMessageToLog("Registering with central server...");
    // register response contains the Operations port for the Central Server
    RegisterResponse registerResponse = 
        centralServer.getAccess().registerChatNode(serverInfo.getHostname(),
        serverInfo.getOperationsPort());

    CristiansLogger.writeMessageToLog("Setting up chat server operations...");
    // start operations registry
    Registry operationsRegistry = LocateRegistry.createRegistry(serverInfo.getOperationsPort());
    IChatroomOperations operationsEngine = new ChatroomOperations(roomMap, roomMapLock, serverInfo);
    operationsRegistry.rebind("IChatroomOperations", operationsEngine);

    CristiansLogger.writeMessageToLog("Staring TCP socket connection thread...");
    // start receive thread for socket connections
    ConnectChatroom thread = 
        new ConnectChatroom(serverInfo.getTcpPort(), this.roomMap, this.roomMapLock);
    thread.start();

    CristiansLogger.writeMessageToLog("Setting up chatroom user operations...");
    // start RMI chat registry
    Registry userRegistry = LocateRegistry.createRegistry(serverInfo.getRmiPort());
    IChatroomUserOperations userOperationsEngine = 
        new ChatroomUserOperations(this.roomMap, 
            this.roomMapLock, serverInfo, registerResponse.getPort());
    userRegistry.rebind("IChatroomUserOperations", userOperationsEngine);

    // indicate the server is ready
    System.out.println(ThreadSafeStringFormatter.format(
        "Chat Server %s is ready",
        serverInfo.getId()
        ));
  }

  /**
   * This is the driver main class to start the server program which accepts
   * 7 specific values which are mapped to several server port numbers and host addresses
   * of components in the system
   *
   * @param args commandline arguments which are necessary to run the server
   */
  public static void main(String[] args) {

    ServerInfo serverInfo = null;
    try {
      serverInfo = App.parseCommandLineArgs(args);
    } catch (IllegalArgumentException e) {
      // print to command line since logger has not been initialized
      System.out.println(e.getMessage());
      return;
    }

    CristiansLogger.loggerSetup(ThreadSafeStringFormatter.format("ChatNode%s", serverInfo.getId()));

    App app = new App();
    try {
      app.go(serverInfo);
    } catch (RemoteException | NotBoundException e) {
      CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Chat node failed on startup with message: \"%s\"",
          e.getMessage()
          ));
    }

  }

  /**
   * Parses the commandline arguments values for the the centralServerPort, tcpPort, rmiPort
   * operations port as well as host address information for the local chat server and the
   * central server host
   *
   * @param args commandline arguments 
   * @return returns the validated ports and host addresses
   */
  public static ServerInfo parseCommandLineArgs(String[] args) {

    if (args.length != 7) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Expected 7 arguments <id> <central hostname> <register port> "
          + "<hostname> <tcp port> <rmi port> <operations port>, received \"%d\" arguments",
          args.length
          ));
    }

    // parse port data for the chat server

    int centralServerPort;
    try {
      centralServerPort = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <central server port> value, must be int, received \"%s\"",
          args[2]
          ));
    }

    int tcpPort;
    try {
      tcpPort = Integer.parseInt(args[4]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <tcp port> value, must be int, received \"%s\"",
          args[4]
          ));
    }

    int rmiPort;
    try {
      rmiPort = Integer.parseInt(args[5]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <rmi port> value, must be int, received \"%s\"",
          args[5]
          ));
    }

    int operationsPort;
    try {
      operationsPort = Integer.parseInt(args[6]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <operations port> value, must be int, received \"%s\"",
          args[6]
          ));
    }


    return new ServerInfo(args[0], args[1], 
        centralServerPort, args[3], tcpPort, rmiPort, operationsPort);
  }
}

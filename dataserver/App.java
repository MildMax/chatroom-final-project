package dataserver;

import data.ICentralOperations;
import data.IDataOperations;
import data.IDataParticipant;
import data.RegisterResponse;
import util.CristiansLogger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

/**
 * dataserver initiator which acts as the driver class for the data server
 * application.
 *
 */
public class App {

  private final Map<String, String> userMap;
  private final Map<String, String> channelMap;
  private final Object userMapLock;
  private final Object channelMapLock;

  /**
   * constructor of the class.
   */
  public App() {

    this.userMap = Collections.synchronizedMap(new HashMap<>());
    this.channelMap = Collections.synchronizedMap(new HashMap<>());
    this.userMapLock = new Object();
    this.channelMapLock = new Object();
  }
  /**
   * initiator with the server info which is required for the start of the 
   * data server.
   * @param serverInfo server information 
   * @throws RemoteException handles remote object invocations
   * @throws NotBoundException bounds exceptions
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


    // initialize users.txt file for tracking user data
    // if non exists, create the file
    // if the file exists, read existing users into the user map
    CristiansLogger.writeMessageToLog("Creating users.txt file if none exists");
    synchronized(userMapLock) {
      File users = new File("files_" + serverInfo.getId() + "/users.txt");
      // Read from users file
      try {
        // Create file if it doesn't exist yet.
        users.createNewFile();
        BufferedReader br = new BufferedReader(new FileReader(users));
        String user;
        CristiansLogger.writeMessageToLog("Reading existing users into memory...");
        while ((user = br.readLine()) != null) {
          String[] userpass = user.split(":");
          userMap.put(userpass[0], userpass[1]);
        }
        br.close();
      } catch (IOException e) {
        CristiansLogger.writeErrorToLog("Unable to find or create "
            + "users.txt for server; shutting down server");
        return;
      }
    }

    // initialize the chatrooms.txt file for tracking chatroom and username data
    // if non exists, create the file
    // if the file exists, read existing chatrooms into memory
    CristiansLogger.writeMessageToLog("Creating chatrooms.txt file if none exists");
    List<String> roomNames = new LinkedList<>();
    synchronized(channelMapLock) {
      File chatrooms = new File("files_" + serverInfo.getId() + "/chatrooms.txt");
      try {
        // Create file if it doesn't exist yet.
        chatrooms.createNewFile();
        BufferedReader br = new BufferedReader(new FileReader(chatrooms));
        String channel;
        CristiansLogger.writeMessageToLog("Reading existing chatrooms into memory...");
        // Read from chatrooms file
        while ((channel = br.readLine()) != null) {
          String[] channeluser = channel.split(":");
          channelMap.put(channeluser[0], channeluser[1]);
        }
        br.close();
      } catch (IOException e) {
        CristiansLogger.writeErrorToLog("Unable to find or "
            + "create chatrooms.txt for server; shutting down server");
        return;
      }
      roomNames.addAll(channelMap.keySet());
    }

    CristiansLogger.writeMessageToLog("Registering data node with central server...");
    // register response contains the Coordinator port for the Central Server
    RegisterResponse registerResponse = 
        centralServer.getAccess().registerDataNode(serverInfo.getHostname(),
        serverInfo.getOperationsPort(),
        serverInfo.getParticipantPort(),
        roomNames
        );


    // create directory for chatroom logs
    // if the directory does not exist, create the directory
    // contains logs for individual chatrooms
    File chatLogdir = new File("files_" + serverInfo.getId() + "/chatlogs");
    if (!chatLogdir.exists()) {
      CristiansLogger.writeMessageToLog("Creating chatlogs directory");
      if (!chatLogdir.mkdir()) {
        CristiansLogger.writeErrorToLog("Unable to create chatLogs subdirectory");
        return;
      }
    }

    CristiansLogger.writeMessageToLog("Setting up data operations...");
    // start the Data Operations registry
    Registry operationsRegistry = LocateRegistry.createRegistry(serverInfo.getOperationsPort());
    IDataOperations operationsEngine = 
        new DataOperations(this.userMap, this.userMapLock, 
            this.channelMap, this.channelMapLock, serverInfo);
    operationsRegistry.rebind("IDataOperations", operationsEngine);

    CristiansLogger.writeMessageToLog("Setting up participant operations...");
    // start the Data Participant registry
    Registry participantRegistry = LocateRegistry.createRegistry(serverInfo.getParticipantPort());
    IDataParticipant participantEngine = 
        new ParticipantOperations(serverInfo.getCentralServerHostname(), 
            registerResponse.getPort(), serverInfo.getId(), (DataOperations) operationsEngine);
    participantRegistry.rebind("IDataParticipant", participantEngine);

    System.out.println(ThreadSafeStringFormatter.format(
        "Data server %s is ready",
        serverInfo.getId()
        ));
  }

  /**
   * driver method for the start of data server which accepts commandline
   * arguments required for the application.
   * @param args command line arguments
   */
  public static void main(String[] args) {

    ServerInfo serverInfo = null;
    try {
      serverInfo = App.parseCommandLineArguments(args);
    } catch (IllegalArgumentException e) {
      // print to command line since logger has not yet been set up
      System.out.println(e.getMessage());
      return;
    }

    // set up unique log for this data node based on provided id
    CristiansLogger.loggerSetup(ThreadSafeStringFormatter.format("DataNode%s", serverInfo.getId()));

    // Create a directory for each server based on it's name 
    String fileDir = "files_" + serverInfo.getId() + "/";
    new File(fileDir).mkdir();

    App app = new App();
    try {
      app.go(serverInfo);
    } catch (RemoteException | NotBoundException e) {
      CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Data node failed on startup with message: \"%s\"",
          e.getMessage()
          ));
    }
  }

  /**
   * server infor needed by the data server.
   * @param args command line arguments
   * @return server info
   * @throws IllegalArgumentException invalid commandline arguments
   */
  public static ServerInfo parseCommandLineArguments(String[] args) 
      throws IllegalArgumentException {

    if (args.length != 6) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Expected 6 arguments <id> <central hostname> "
          + "<register port> <hostname> <operations port> "
          + "<participant port>, received \"%d\" arguments",
          args.length
          ));
    }

    int centralServerPort;
    try {
      centralServerPort = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <central server port> value, must be int, received \"%s\"",
          args[2]
          ));
    }

    int operationsPort;
    try {
      operationsPort = Integer.parseInt(args[4]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <operations port> value, must be int, received \"%s\"",
          args[4]
          ));
    }

    int participantPort;
    try {
      participantPort = Integer.parseInt(args[5]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <participant port> value, must be int, received \"%s\"",
          args[5]
          ));
    }


    return new ServerInfo(args[0], args[1], centralServerPort, 
        args[3], operationsPort, participantPort);

  }
}

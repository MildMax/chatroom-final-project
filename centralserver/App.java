package centralserver;

import data.ICentralChatroomOperations;
import data.ICentralOperations;
import data.ICentralUserOperations;
import data.IChatroomOperations;
import data.IDataOperations;
import data.IDataParticipant;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

/**
 * App class acts as the start point for the entire application, in a way it 
 * initiats all the entry ports, taking four  comandline arguments which
 * each represent different ports. In order to allow application follow the 
 * singleton principle only the instance created in this class is being forwarded 
 * for the entire application. Thus initializations made in this class are
 * being used through out the central server.
 * 
 */
public class App {

  private final List<RMIAccess<IChatroomOperations>> chatroomNodes;
  private final Object chatroomNodeLock;
  private final List<RMIAccess<IDataOperations>> dataNodesOperations;
  private final Object dataNodeOperationsLock;
  private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
  private final Object dataNodeParticipantsLock;

  /**
   * Constructor for App class which initiated objects for all the different
   * nodes of the entire central server application. These initiated 
   * instances are used further along in the ap[plication to follow
   * synchronization across the application.
   * 
   */
  public App() {
    this.chatroomNodeLock = new Object();
    this.dataNodeOperationsLock = new Object();
    this.dataNodeParticipantsLock = new Object();
    this.chatroomNodes = Collections.synchronizedList(new ArrayList<>());
    this.dataNodesOperations = Collections.synchronizedList(new ArrayList<>());
    this.dataNodesParticipants = Collections.synchronizedList(new ArrayList<>());
  }

  /**
   * This method gives a headstart to the application by using the initiated
   * instances of every node, it creates an instance for central operation
   * Registry. It also handles the local thread to perform node cleanup
   * which utilizes timer.
   * It is responsiblr for starting the registry for data, chatroom, client
   * @param serverInfo information required to run the Central Server
   * @throws RemoteException if there is an error generating RMI interfaces
   */
  public void go(ServerInfo serverInfo) throws RemoteException {
    // start registry for Register function
    Registry centralOperationsRegistry = 
        LocateRegistry.createRegistry(serverInfo.getRegisterPort());
    ICentralOperations centralOperationsEngine = new CentralOperations(
        this.chatroomNodes,
        this.chatroomNodeLock,
        this.dataNodesOperations,
        this.dataNodeOperationsLock,
        this.dataNodesParticipants,
        this.dataNodeParticipantsLock,
        serverInfo);
    centralOperationsRegistry.rebind("ICentralOperations", centralOperationsEngine);

    // start local thread for node cleanup -- runs on a timer
    ResourceCleaner cleaner = new ResourceCleaner(
        this.chatroomNodes,
        this.chatroomNodeLock,
        this.dataNodesOperations,
        this.dataNodeOperationsLock,
        this.dataNodesParticipants,
        this.dataNodeParticipantsLock);
    Thread cleanerThread = new Thread(cleaner);
    cleanerThread.start();

    Logger.writeMessageToLog("Setting up central coordinator interface...");
    // start registry for Data -> Central Coordinator operations
    Registry centralCoordinatorRegistry = 
        LocateRegistry.createRegistry(serverInfo.getCoordinatorPort());
    CentralCoordinator coordinatorEngine = new CentralCoordinator();
    centralCoordinatorRegistry.rebind("ICentralCoordinator", coordinatorEngine);

    Logger.writeMessageToLog("Setting up chatroom operations interface...");
    // start registry for Chatroom -> Central Server communication
    Registry centralChatroomOperationsRegistry = 
        LocateRegistry.createRegistry(serverInfo.getChatroomPort());
    ICentralChatroomOperations centralChatroomOperationsEngine = 
        new CentralChatroomOperations(this.dataNodesParticipants, 
            this.dataNodeParticipantsLock, coordinatorEngine);
    centralChatroomOperationsRegistry.rebind("ICentralChatroomOperations", 
        centralChatroomOperationsEngine);

    Logger.writeMessageToLog("Setting up user operations interface...");
    // start registry for Client -> Central Server communication
    Registry centralUserOperationsRegistry = 
        LocateRegistry.createRegistry(serverInfo.getUserPort());
    ICentralUserOperations centralUserOperationsEngine = new CentralUserOperations(
        this.chatroomNodes,
        this.chatroomNodeLock,
        this.dataNodesOperations,
        this.dataNodeOperationsLock,
        this.dataNodesParticipants,
        this.dataNodeParticipantsLock,
        coordinatorEngine,
        cleaner);
    centralUserOperationsRegistry.rebind("ICentralUserOperations", centralUserOperationsEngine);

    System.out.println("Central Server is ready");
  }

  /**
   * The main class which acts a driver for the entire application to start
   * This method accepts the commandline arguments  
   * such as register port, chatroom port, user port, coordinator port
   * respectively.
   * @param args the commandline arguments which are necessary for starting 
   *the application.
   */
  public static void main(String[] args) {

    // initialize the logger for the central server
    Logger.loggerSetup("CentralServer");

    // parse command line arguments
    ServerInfo serverInfo = null;
    try {
      serverInfo = App.parseCommandLineArguments(args);
    } catch (IllegalArgumentException e) {
      Logger.writeErrorToLog(e.getMessage());
      return;
    }

    // start the application
    App app = new App();
    try {
      app.go(serverInfo);
    } catch (RemoteException e) {
      Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Unable to start Central Server; failed with error: \"%s\"",
          e.getMessage()
          ));
    }
  }

  /**
   * This method is responsible for parsing the given commandline arguments.
   * and validating them before using and assigning to speciific uses which
   * they are intented for registerPort, chatroomPort, userPort, coordinatorPort
   * @param args commandline arguments taken in order to run the application.
   * @return the new validated port numbers.
   * @throws IllegalArgumentException if any of the input is invalid
   */

  public static ServerInfo parseCommandLineArguments(String[] args) 
      throws IllegalArgumentException {

    // if length of arguments is less than 4, throw error
    if (args.length != 4) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Expected 4 arguments <register port> <chatroom port> <user port> <coordinator port>, "
              + "received \"%d\" arguments",
              args.length
          ));
    }

    // parse port data for the central server

    int registerPort;
    try {
      registerPort = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <register port> value, must be int, received \"%s\"",
          args[0]
          ));
    }

    int chatroomPort;
    try {
      chatroomPort = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <chatroom port> value, must be int, received \"%s\"",
          args[1]
          ));
    }

    int userPort;
    try {
      userPort = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <user port> value, must be int, received \"%s\"",
          args[2]
          ));
    }

    int coordinatorPort;
    try {
      coordinatorPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <coordinator port> value, must be int, received \"%s\"",
          args[3]
          ));
    }

    return new ServerInfo(registerPort, chatroomPort, userPort, coordinatorPort);
  }
}

package centralserver;

import data.*;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class App {

    private final List<RMIAccess<IChatroomOperations>> chatroomNodes;
    private final Object chatroomNodeLock;
    private final List<RMIAccess<IDataOperations>> dataNodesOperations;
    private final Object dataNodeOperationsLock;
    private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
    private final Object dataNodeParticipantsLock;

    public App() {
        this.chatroomNodeLock = new Object();
        this.dataNodeOperationsLock = new Object();
        this.dataNodeParticipantsLock = new Object();
        this.chatroomNodes = Collections.synchronizedList(new ArrayList<>());
        this.dataNodesOperations = Collections.synchronizedList(new ArrayList<>());
        this.dataNodesParticipants = Collections.synchronizedList(new ArrayList<>());
    }

    public void go(ServerInfo serverInfo) throws RemoteException {
        // start registry for Register function
        Registry centralOperationsRegistry = LocateRegistry.createRegistry(serverInfo.getRegisterPort());
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
        Registry centralCoordinatorRegistry = LocateRegistry.createRegistry(serverInfo.getCoordinatorPort());
        CentralCoordinator coordinatorEngine = new CentralCoordinator();
        centralCoordinatorRegistry.rebind("ICentralCoordinator", coordinatorEngine);

        Logger.writeMessageToLog("Setting up chatroom operations interface...");
        // start registry for Chatroom -> Central Server communication
        Registry centralChatroomOperationsRegistry = LocateRegistry.createRegistry(serverInfo.getChatroomPort());
        ICentralChatroomOperations centralChatroomOperationsEngine = new CentralChatroomOperations(this.dataNodesParticipants, this.dataNodeParticipantsLock, coordinatorEngine);
        centralChatroomOperationsRegistry.rebind("ICentralChatroomOperations", centralChatroomOperationsEngine);

        Logger.writeMessageToLog("Setting up user operations interface...");
        // start registry for Client -> Central Server communication
        Registry centralUserOperationsRegistry = LocateRegistry.createRegistry(serverInfo.getUserPort());
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

    public static void main(String[] args) {

        Logger.loggerSetup("CentralServer");
        ServerInfo serverInfo = null;
        try {
            serverInfo = App.parseCommandLineArguments(args);
        } catch (IllegalArgumentException e) {
            Logger.writeErrorToLog(e.getMessage());
            return;
        }

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

    public static ServerInfo parseCommandLineArguments(String[] args) throws IllegalArgumentException {

        if (args.length != 4) {
            throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
                    "Expected 4 arguments <register port> <chatroom port> <user port> <coordinator port>, received \"%d\" arguments",
                    args.length
            ));
        }

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

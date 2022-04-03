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
import java.util.concurrent.ThreadLocalRandom;

public class App {

    private List<RMIAccess<IChatroomOperations>> chatroomNodes;
    private Object chatroomNodeLock;
    private List<RMIAccess<IDataOperations>> dataNodesOperations;
    private Object dataNodeOperationsLock;
    private List<RMIAccess<IDataParticipant>> dataNodesParticipants;
    private Object dataNodeParticipantsLock;

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
                this.dataNodeParticipantsLock);
        centralOperationsRegistry.rebind("ICentralOperations", centralOperationsEngine);

        // start registry for Chatroom -> Central Server communication
        Registry centralChatroomOperationsRegistry = LocateRegistry.createRegistry(serverInfo.getChatroomPort());
        ICentralChatroomOperations centralChatroomOperationsEngine = new CentralChatroomOperations(this.dataNodesParticipants, this.dataNodeParticipantsLock);
        centralChatroomOperationsRegistry.rebind("ICentralChatroomOperations", centralChatroomOperationsEngine);

        // start registry for Client -> Central Server communication
        Registry centralUserOperationsRegistry = LocateRegistry.createRegistry(serverInfo.getUserPort());
        ICentralUserOperations centralUserOperationsEngine = new CentralUserOperations(
                this.chatroomNodes,
                this.chatroomNodeLock,
                this.dataNodesOperations,
                this.dataNodeOperationsLock,
                this.dataNodesParticipants,
                this.dataNodeParticipantsLock);
        centralUserOperationsRegistry.rebind("ICentralUserOperations", centralChatroomOperationsEngine);

        System.out.println("Central Server is ready");

    }

    public static void main(String[] args) {

        Logger.serverLoggerSetup("CentralServer");
        ServerInfo serverInfo = App.parseCommandLineArguments(args);

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

    public static ServerInfo parseCommandLineArguments(String[] args) {

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

        return new ServerInfo(registerPort, chatroomPort, userPort);
    }

    static class ServerInfo {
        private final int registerPort;
        private final int chatroomPort;
        private final int userPort;

        ServerInfo(int registerPort, int chatroomPort, int userPort) {
            this.registerPort = registerPort;
            this.chatroomPort = chatroomPort;
            this.userPort = userPort;
        }

        int getRegisterPort() { return this.registerPort; }

        int getChatroomPort() { return this.chatroomPort; }

        int getUserPort() { return this.userPort; }
    }


}

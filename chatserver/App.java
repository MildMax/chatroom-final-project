package chatserver;

import data.ICentralOperations;
import data.IChatroomOperations;
import data.IChatroomUserOperations;
import data.RegisterResponse;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class App {

    private final Map<String, Chatroom> roomMap;
    private final Object roomMapLock;

    public App() {
        this.roomMap = Collections.synchronizedMap(new HashMap<>());
        this.roomMapLock = new Object();
    }

    public void go(ServerInfo serverInfo) throws RemoteException, NotBoundException {

        // register Data node with the central server
        RMIAccess<ICentralOperations> centralServer = new RMIAccess<>(serverInfo.getCentralServerHostname(), serverInfo.getCentralServerPort(), "ICentralOperations");

        // register response contains the Operations port for the Central Server
        RegisterResponse registerResponse = centralServer.getAccess().registerChatNode(serverInfo.getHostname(), serverInfo.getOperationsPort());

        // start operations registry
        Registry operationsRegistry = LocateRegistry.createRegistry(serverInfo.getOperationsPort());
        IChatroomOperations operationsEngine = new ChatroomOperations(roomMap, roomMapLock, serverInfo);
        operationsRegistry.rebind("IChatroomOperations", operationsEngine);

        // start RMI chat registry
        Registry userRegistry = LocateRegistry.createRegistry(serverInfo.getRmiPort());
        IChatroomUserOperations userOperationsEngine = new ChatroomUserOperations(this.roomMap, this.roomMapLock, serverInfo, registerResponse.getPort());
        userRegistry.rebind("IChatroomUserOperations", userOperationsEngine);

        // start TCP ports here for receiving client tcp connections for subs, likely in a new thread that can continually wait for new connections
    }

    public static void main(String[] args) {

        ServerInfo serverInfo = null;
        try {
            serverInfo = App.parseCommandLineArgs(args);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }

        Logger.serverLoggerSetup(ThreadSafeStringFormatter.format("ChatNode%s", serverInfo.getId()));

        App app = new App();
        try {
            app.go(serverInfo);
        } catch (RemoteException | NotBoundException e) {
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Chat node failed on startup with message: \"%s\"",
                    e.getMessage()
            ));
        }

    }

    public static ServerInfo parseCommandLineArgs(String[] args) {
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


        return new ServerInfo(args[0], args[1], centralServerPort, args[3], tcpPort, rmiPort, operationsPort);
    }
}

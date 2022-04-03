package dataserver;

import data.ICentralOperations;
import data.IDataOperations;
import data.IDataParticipant;
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

    private final Map<String, String> userMap;
    private final Object userMapLock;

    public App() {

        this.userMap = Collections.synchronizedMap(new HashMap<>());
        this.userMapLock = new Object();
    }

    public void go(ServerInfo serverInfo) throws RemoteException, NotBoundException {

        // register Data node with the central server
        RMIAccess<ICentralOperations> centralServer = new RMIAccess<>(serverInfo.getCentralServerHostname(), serverInfo.getCentralServerPort(), "ICentralOperations");

        // register response contains the Coordinator port for the Central Server
        RegisterResponse registerResponse = centralServer.getAccess().registerDataNode(serverInfo.getHostname(), serverInfo.getOperationsPort(), serverInfo.getParticipantPort());

        // populate the user map here

        // start the Data Operations registry
        Registry operationsRegistry = LocateRegistry.createRegistry(serverInfo.getOperationsPort());
        IDataOperations operationsEngine = new DataOperations(this.userMap, this.userMapLock);
        operationsRegistry.rebind("IDataOperations", operationsEngine);

        // start the Data Participant registry
        Registry participantRegistry = LocateRegistry.createRegistry(serverInfo.getParticipantPort());
        IDataParticipant participantEngine = new ParticipantOperations(serverInfo.getCentralServerHostname(), registerResponse.getPort());
        participantRegistry.rebind("IDataParticipant", participantEngine);
    }

    public static void main(String[] args) {

        ServerInfo serverInfo = null;
        try {
            serverInfo = App.parseCommandLineArguments(args);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }

        Logger.serverLoggerSetup(ThreadSafeStringFormatter.format("DataNode%s", serverInfo.getId()));

        App app = new App();
        try {
            app.go(serverInfo);
        } catch (RemoteException | NotBoundException e) {
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Data node failed on startup with message: \"%s\"",
                    e.getMessage()
            ));
        }
    }

    public static ServerInfo parseCommandLineArguments(String[] args) throws IllegalArgumentException {

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


        return new ServerInfo(args[0], args[1], centralServerPort, args[3], operationsPort, participantPort);

    }

    static class ServerInfo {

        private final String id;
        private final String centralServerHostname;
        private final int centralServerPort;
        private final String hostname;
        private final int operationsPort;
        private final int participantPort;

        ServerInfo(String id, String centralServerHostname, int centralServerPort, String hostname, int operationsPort, int participantPort) {
            this.id = id;
            this.centralServerHostname = centralServerHostname;
            this.centralServerPort = centralServerPort;
            this.hostname = hostname;
            this.operationsPort = operationsPort;
            this.participantPort = participantPort;
        }

        public String getId() {
            return id;
        }

        public String getCentralServerHostname() {
            return centralServerHostname;
        }

        public int getCentralServerPort() {
            return centralServerPort;
        }

        public String getHostname() {
            return hostname;
        }

        public int getOperationsPort() {
            return operationsPort;
        }

        public int getParticipantPort() {
            return participantPort;
        }
    }
}

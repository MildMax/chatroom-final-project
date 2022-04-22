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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class App {

    private final Map<String, String> userMap;
    private final Map<String, String> channelMap;
    private final Object userMapLock;
    private final Object channelMapLock;

    public App() {

        this.userMap = Collections.synchronizedMap(new HashMap<>());
        this.channelMap = Collections.synchronizedMap(new HashMap<>());
        this.userMapLock = new Object();
        this.channelMapLock = new Object();
    }

    public void go(ServerInfo serverInfo) throws RemoteException, NotBoundException {

        // register Data node with the central server
        RMIAccess<ICentralOperations> centralServer = new RMIAccess<>(serverInfo.getCentralServerHostname(), serverInfo.getCentralServerPort(), "ICentralOperations");

        // initiate Cristians algorithm thread
        CristiansLogger.setCentralAccessor(centralServer);
        Thread t = new Thread(new CristiansLogger());
        t.start();

        // register response contains the Coordinator port for the Central Server
        RegisterResponse registerResponse = centralServer.getAccess().registerDataNode(serverInfo.getHostname(), serverInfo.getOperationsPort(), serverInfo.getParticipantPort());

        synchronized(userMapLock) {
        		File users = new File("files_" + serverInfo.getId() + "/users.txt");
        		// Read from users file
        		try {
        			// Create file if it doesn't exist yet.
        			users.createNewFile();
					BufferedReader br = new BufferedReader(new FileReader(users));
					String user;
					while ((user = br.readLine()) != null) {
						String[] userpass = user.split(":");
						userMap.put(userpass[0], userpass[1]);
					}
					br.close();
				} catch (IOException e) {
					CristiansLogger.writeErrorToLog("Unable to find or create users.txt for server; shutting down server");
					return;
				}
        }

        synchronized(channelMapLock) {
    		File chatrooms = new File("files_" + serverInfo.getId() + "/chatrooms.txt");
    		// Read from chatrooms file
    		try {
    			// Create file if it doesn't exist yet.
    			chatrooms.createNewFile();
				BufferedReader br = new BufferedReader(new FileReader(chatrooms));
				String channel;
				while ((channel = br.readLine()) != null) {
					String[] channeluser = channel.split(":");
					channelMap.put(channeluser[0], channeluser[1]);
				}
				br.close();
			} catch (IOException e) {
                CristiansLogger.writeErrorToLog("Unable to find or create chatrooms.txt for server; shutting down server");
                return;
			}
        }

        // create directory for chatroom logs
        File chatLogdir = new File("files_" + serverInfo.getId() + "/chatlogs");
        if (!chatLogdir.exists()) {
            if (!chatLogdir.mkdir()) {
                CristiansLogger.writeErrorToLog("Unable to create chatLogs subdirectory");
                return;
            }
        }
        
        // start the Data Operations registry
        Registry operationsRegistry = LocateRegistry.createRegistry(serverInfo.getOperationsPort());
        IDataOperations operationsEngine = new DataOperations(this.userMap, this.userMapLock, this.channelMap, this.channelMapLock, serverInfo);
        operationsRegistry.rebind("IDataOperations", operationsEngine);

        // start the Data Participant registry
        Registry participantRegistry = LocateRegistry.createRegistry(serverInfo.getParticipantPort());
        IDataParticipant participantEngine = new ParticipantOperations(serverInfo.getCentralServerHostname(), registerResponse.getPort(), serverInfo.getId(), (DataOperations) operationsEngine);
        participantRegistry.rebind("IDataParticipant", participantEngine);
        
        System.out.println(ThreadSafeStringFormatter.format(
                "Data server %s is ready",
                serverInfo.getId()
        ));
    }

    public static void main(String[] args) {

        ServerInfo serverInfo = null;
        try {
            serverInfo = App.parseCommandLineArguments(args);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }

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

    public static ServerInfo parseCommandLineArguments(String[] args) throws IllegalArgumentException {

        if (args.length != 6) {
            throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
                    "Expected 6 arguments <id> <central hostname> <register port> <hostname> <operations port> <participant port>, received \"%d\" arguments",
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


        return new ServerInfo(args[0], args[1], centralServerPort, args[3], operationsPort, participantPort);

    }
}

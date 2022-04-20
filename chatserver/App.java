package chatserver;

import data.ChatroomUserResponse;
import data.ICentralOperations;
import data.IChatroomOperations;
import data.IChatroomUserOperations;
import data.RegisterResponse;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
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

    public void go(ServerInfo serverInfo) throws IOException, NotBoundException {

        // register Data node with the central server
        RMIAccess<ICentralOperations> centralServer = new RMIAccess<>(serverInfo.getCentralServerHostname(),
                serverInfo.getCentralServerPort(), "ICentralOperations");

        // register response contains the Operations port for the Central Server
        RegisterResponse registerResponse = centralServer.getAccess().registerChatNode(serverInfo.getHostname(),
                serverInfo.getOperationsPort());

        // start operations registry
        Registry operationsRegistry = LocateRegistry.createRegistry(serverInfo.getOperationsPort());
        IChatroomOperations operationsEngine = new ChatroomOperations(roomMap, roomMapLock, serverInfo);
        operationsRegistry.rebind("IChatroomOperations", operationsEngine);

        // start RMI chat registry
        Registry userRegistry = LocateRegistry.createRegistry(serverInfo.getRmiPort());
        IChatroomUserOperations userOperationsEngine = new ChatroomUserOperations(this.roomMap, this.roomMapLock, serverInfo, registerResponse.getPort());
        userRegistry.rebind("IChatroomUserOperations", userOperationsEngine);

        // start TCP ports here for receiving client tcp connections for subs,g
        // likely in a new thread that can continually wait for new connections
        ServerSocket chatserver = new ServerSocket(serverInfo.getTcpPort());

        while(true){
            Socket client = chatserver.accept();


            //Need to identify packet that carries client's chosen chatroom
            ConnectChatroom connectChatroom = new ConnectChatroom(roomMap, roomMapLock, operationsEngine,
                    client);

            connectChatroom.start();

        }



        System.out.println(ThreadSafeStringFormatter.format(
                "Chat Server %s is ready",
                serverInfo.getId()
        ));
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

        if (args.length != 7) {
            throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
                    "Expected 7 arguments <id> <central hostname> <register port> <hostname> <tcp port> <rmi port> <operations port>, received \"%d\" arguments",
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


    //Thread to spin chatrooms
    private class ConnectChatroom extends Thread {
        Object roomLock;
        Map<String, Chatroom> roomMap;
        IChatroomOperations operations;
        Socket client;

        public ConnectChatroom(Map<String, Chatroom> roomMap, Object roomLock,
                               IChatroomOperations operations, Socket client){

            this.roomMap = roomMap;
            this.roomLock = roomLock;
            this.operations = operations;
            this.client = client;
        }

        public void run(){

            String chatroomName = "";
            //Find chatroom client is looking to join
            ChatroomUserResponse chosenChatroom = null;
            try {
            //Use chatroom response to reference chatroom
                chosenChatroom = operations.getChatroom(chatroomName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            if(chosenChatroom == null){

            }else{

                Chatroom chatroomReference = chosenChatroom.getChatroomCarrier().getChatroom();
                synchronized (roomMapLock){
                    //Subscribe client socket to matching chatroom
                    chatroomReference.Subscribe(client);
                }

            }
        }
    }
}

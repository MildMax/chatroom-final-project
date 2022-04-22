package chatserver;

import util.CristiansLogger;
import util.Logger;
import util.ThreadSafeStringFormatter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class ConnectChatroom extends Thread {

    private final int tcpPort;
    private final Map<String, Chatroom> roomMap;
    private final Object roomMapLock;

    public ConnectChatroom(int tcpPort , Map<String, Chatroom> roomMap, Object roomMapLock) {
        this.tcpPort = tcpPort;
        this.roomMap = roomMap;
        this.roomMapLock = roomMapLock;
    }

    @Override
    public void run() {

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
                clientSocket = serverSocket.accept();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "Received TCP connection from client at \"%s:%d\"",
                        clientSocket.getInetAddress().getHostAddress(),
                        clientSocket.getPort()
                ));


                String clientMessage = in.readLine();
                String[] vals = clientMessage.split(":");

                if (vals.length != 2) {
                    out.println("fail");
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
                    Chatroom chatroom = roomMap.get(vals[0]);
                    if (chatroom == null) {
                        out.println("fail");
                        in.close();
                        out.close();
                        clientSocket.close();
                        continue;
                    }
                    chatroom.subscribe(clientSocket, vals[1]);
                }

                out.println("success");
            } catch (IOException e) {
                CristiansLogger.writeErrorToLog("Unable to receive client connection on server socket");
            }
        }

    }
}

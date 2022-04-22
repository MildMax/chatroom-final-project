package chatserver;

import java.io.IOException;
import java.io.OutputStreamWriter;
import util.CristiansLogger;
import util.ThreadSafeStringFormatter;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Chatroom {

    private final Map<String, Socket> socketMap;
    private final Object socketMapLock;
    private final String roomName;

    public Chatroom(String roomName) {
        this.socketMap = new HashMap<>();
        this.socketMapLock = new Object();
        this.roomName = roomName;
    }

    public void subscribe(Socket s, String username) {
        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Subscribing client \"%s\" to chatroom \"%s\"",
                username,
                this.roomName
        ));
        synchronized (socketMapLock) {
            this.socketMap.put(username, s);
        }
    }

    public void unsubscribe(String username) {
        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Unsubscribing client \"%s\" from chatroom \"%s\"",
                username,
                this.roomName
        ));

        synchronized (socketMapLock) {
            Socket s = socketMap.get(username);
            try {
                s.close();
            } catch (IOException e) {
                CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "There was an error closing the socket for user \"%s\""
                ));
            }
            this.socketMap.remove(username);
        }
    }

    public void publish(String message) {
        synchronized (socketMapLock) {
            for (String user : socketMap.keySet()) {
                CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                        "Publishing message \"%s\" to user \"%s\"",
                        message,
                        user
                ));
                PrintWriter out = null;
                try {
                    out = new PrintWriter(new OutputStreamWriter(socketMap.get(user).getOutputStream()), true);
                    out.println(message);
                } catch (IOException e) {
                    CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Unable to publish message \"%s\" to client \"%s\"",
                            message,
                            user
                    ));
                }

            }
        }
    }

    public int getUserCount() {
        synchronized (socketMapLock) {
            return socketMap.size();
        }
    }
}

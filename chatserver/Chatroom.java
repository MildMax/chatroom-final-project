package chatserver;

import util.CristiansLogger;
import util.ThreadSafeStringFormatter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
        synchronized (socketMapLock) {
            this.socketMap.put(username, s);
        }
    }

    public void unsubscribe(String username) {
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
                        "Publishing message to user \"%s\"",
                        user
                ));
                PrintWriter out = null;
                try {
                    out = new PrintWriter(new OutputStreamWriter(socketMap.get(user).getOutputStream()), true);
                    out.println(message);
                } catch (IOException e) {
                    CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            ""
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

    public String getRoomName() {
        return roomName;
    }
}

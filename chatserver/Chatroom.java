package chatserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class Chatroom {

    private final List<Socket> subscriberList;
    private final Object subscriberListLock;
    private final String creatorUsername;
    private final String roomName;

    public Chatroom(String creatorUsername, String roomName) {
        this.subscriberList = new LinkedList<>();
        this.subscriberListLock = new Object();
        this.creatorUsername = creatorUsername;
        this.roomName = roomName;
    }

    public void Subscribe(Socket s) {
        synchronized (subscriberListLock) {
            subscriberList.add(s);
        }

    }

    public void Publish(String message) {
        synchronized (subscriberListLock) {
            // Pattern: Publisher send via Input Channel/RMI Registry -> Message Broker(Chatroom Server) -> Message Broker sends messages via Ouput Channel to Subsribers

            for (Socket client : subscriberList) {

                try {
                    OutputStreamWriter outputWriter = new OutputStreamWriter(client.getOutputStream());
                    BufferedWriter bufferWriter = new BufferedWriter(outputWriter);
                    bufferWriter.write(creatorUsername + ": " + message);

                } catch(IOException e){
                    e.printStackTrace();
                }
            }


            //Send Message to Input Channel (Socket)
           

            //Have Output Channel Send Out messages to various subscribers.
        }

    }

    public String getCreatorUsername() {
        return this.creatorUsername;
    }

    public int getUserCount() {
        synchronized (subscriberListLock) {
            return subscriberList.size();
        }
    }

    public String getRoomName() {
        return roomName;
    }
}

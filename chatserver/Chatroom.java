package chatserver;

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
            //Connect Subscriber message input to Socket passed in. 
            
            subscriberList.add(s);
        }

    }

    public void Publish(String message) {
        synchronized (subscriberListLock) {
            // do pub here
            
            // Pattern: Publisher send via Input Channel/RMI Registry -> Message Broker(Chatroom Server) -> Message Broker sends messages via Ouput Channel to Subsribers
            
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

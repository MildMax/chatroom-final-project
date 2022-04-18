package chatserver;

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class Chatroom {

    private final List<Socket> subscriberList;
    private final Object subscriberListLock;
    private final String roomName;

    public Chatroom(String roomName) {
        this.subscriberList = new LinkedList<>();
        this.subscriberListLock = new Object();
        this.roomName = roomName;
    }

    public void Subscribe(Socket s) {
        synchronized (subscriberListLock) {
            // do sub here
        }
    }

    public void Publish(String message) {
        synchronized (subscriberListLock) {
            // do pub here
        }

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

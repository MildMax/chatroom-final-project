package data;

import java.io.Serializable;

public class ChatroomDataResponse implements Serializable {

    private final int chatrooms;
    private final int users;
    private final String hostname;
    private final int rmiPort;
    private final int tcpPort;

    public ChatroomDataResponse(int chatrooms, int users, String hostname, int rmiPort, int tcpPort) {
        this.chatrooms = chatrooms;
        this.users = users;
        this.hostname = hostname;
        this.rmiPort = rmiPort;
        this.tcpPort = tcpPort;
    }

    public int getChatrooms() {
        return this.chatrooms;
    }

    public int getUsers() {
        return this.users;
    }

    public String getHostname() { return this.hostname; }

    public int getRmiPort() { return this.rmiPort; }

    public int getTcpPort() { return this.tcpPort; }
}

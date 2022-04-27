package data;

import java.io.Serializable;

import chatserver.Chatroom;

public class ChatroomResponse extends Response implements Serializable {
    private String name;
    private String address;
    private int tcpPort;
    private int registryPort;
    private Chatroom chatroom;

    public ChatroomResponse(ResponseStatus status, String message, String name, String address, int tcpPort, int registryPort) {
        super(status, message);
        this.name = name;
        this.address = address;
        this.tcpPort = tcpPort;
        this.registryPort = registryPort;
    }

    public ChatroomResponse(ResponseStatus status, String message) {
        super(status, message);
    }


    public String getName() {
        return this.name;
    }

    public String getAddress() {
        return this.address;
    }

    public int getTcpPort() { return this.tcpPort; }

    public int getRegistryPort() { return this.registryPort; }

}

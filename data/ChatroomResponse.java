package data;

import java.io.Serializable;

public class ChatroomResponse implements Serializable {
    private String name;
    private String address;
    private int tcpPort;
    private int registryPort;

    public ChatroomResponse(String name, String address, int tcpPort, int registryPort) {
        this.name = name;
        this.address = address;
        this.tcpPort = tcpPort;
        this.registryPort = registryPort;
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

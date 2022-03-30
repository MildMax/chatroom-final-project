package data;

import java.io.Serializable;

public class ChatroomResponse implements Serializable {
    private String name;
    private String address;
    private int port;

    public ChatroomResponse(String name, String address, int port) {
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public String getName() {
        return this.name;
    }

    public String getAddress() {
        return this.address;
    }

}

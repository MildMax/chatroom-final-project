package data;

import java.io.Serializable;

public class RegisterResponse implements Serializable {

    private int port;

    public RegisterResponse(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}

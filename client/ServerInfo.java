package client;

public class ServerInfo {

    private final String centralHost;
    private final int centralPort;

    public ServerInfo(String centralHost, int centralPort) {
        this.centralHost = centralHost;
        this.centralPort = centralPort;
    }

    public String getCentralHost() {
        return centralHost;
    }

    public int getCentralPort() {
        return centralPort;
    }
}

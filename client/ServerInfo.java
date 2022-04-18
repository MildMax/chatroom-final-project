package client;

public class ServerInfo {

    private final String centralHost;
    private final int centralPort;
    private final boolean isTest;

    public ServerInfo(String centralHost, int centralPort, boolean isTest) {
        this.centralHost = centralHost;
        this.centralPort = centralPort;
        this.isTest = isTest;
    }

    public String getCentralHost() {
        return centralHost;
    }

    public int getCentralPort() {
        return centralPort;
    }

    public boolean getIsTest() { return this.isTest; }
}

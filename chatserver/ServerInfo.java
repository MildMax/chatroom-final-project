package chatserver;

public class ServerInfo {

    private final String id;
    private final String centralServerHostname;
    private final int centralServerPort;
    private final String hostname;
    private final int tcpPort;
    private final int rmiPort;
    private final int operationsPort;

    ServerInfo(String id, String centralServerHostname, int centralServerPort, String hostname, int tcpPort, int rmiPort, int operationsPort) {
        this.id = id;
        this.centralServerHostname = centralServerHostname;
        this.centralServerPort = centralServerPort;
        this.hostname = hostname;
        this.tcpPort = tcpPort;
        this.rmiPort = rmiPort;
        this.operationsPort = operationsPort;
    }

    public String getId() {
        return id;
    }

    public String getCentralServerHostname() {
        return centralServerHostname;
    }

    public int getCentralServerPort() {
        return centralServerPort;
    }

    public String getHostname() {
        return hostname;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public int getRmiPort() {
        return rmiPort;
    }

    public int getOperationsPort() {
        return operationsPort;
    }
}

package dataserver;

public class ServerInfo {

    private final String id;
    private final String centralServerHostname;
    private final int centralServerPort;
    private final String hostname;
    private final int operationsPort;
    private final int participantPort;

    ServerInfo(String id, String centralServerHostname, int centralServerPort, String hostname, int operationsPort, int participantPort) {
        this.id = id;
        this.centralServerHostname = centralServerHostname;
        this.centralServerPort = centralServerPort;
        this.hostname = hostname;
        this.operationsPort = operationsPort;
        this.participantPort = participantPort;
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

    public int getOperationsPort() {
        return operationsPort;
    }

    public int getParticipantPort() {
        return participantPort;
    }
}
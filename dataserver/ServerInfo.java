package dataserver;

/**
 * helper class to give server info needed by the data server.
 *
 */

public class ServerInfo {

  private final String id;
  private final String centralServerHostname;
  private final int centralServerPort;
  private final String hostname;
  private final int operationsPort;
  private final int participantPort;

  /**
   * constructor of the server info.
   * @param id unique id of the server
   * @param centralServerHostname hostname of the server
   * @param centralServerPort port number of the server
   * @param hostname hostname
   * @param operationsPort port number of the operation
   * @param participantPort port number of the participant
   */
  ServerInfo(String id, String centralServerHostname, 
      int centralServerPort, String hostname, int operationsPort, int participantPort) {
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
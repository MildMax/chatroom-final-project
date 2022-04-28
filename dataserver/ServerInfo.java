package dataserver;

/**
 * Provides port and addressing information required to run the data server
 */
public class ServerInfo {

  private final String id;
  private final String centralServerHostname;
  private final int centralServerPort;
  private final String hostname;
  private final int operationsPort;
  private final int participantPort;

  /**
   * Creates an instance of the ServerInfo object
   *
   * @param id unique id of the data server
   * @param centralServerHostname hostname of the machine supporting the central server
   * @param centralServerPort port the central server is accepting registration requests on
   * @param hostname hostname of the machine supporting the local data server
   * @param operationsPort port the local data server should accept central server requests on
   * @param participantPort port the local data server should accept coordinator requests on
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

  /**
   * Get the unique ID for this data server
   *
   * @return the unique ID for this data server
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the hostname for the machine supporting the central server
   *
   * @return the hostname for the machine supporting the central server
   */
  public String getCentralServerHostname() {
    return centralServerHostname;
  }

  /**
   * Gets the port the central server is accepting registration requests on
   *
   * @return the port the central server is accepting registration requests on
   */
  public int getCentralServerPort() {
    return centralServerPort;
  }

  /**
   * Gets the hostname of the machine supporting the local data server
   *
   * @return the hostname of the machine supporting the local data server
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Gets the port the local data server should accept central server requests on
   *
   * @return the port the local data server should accept central server requests on
   */
  public int getOperationsPort() {
    return operationsPort;
  }

  /**
   * Gets the port the local data server should accept coordinator requests on
   *
   * @return the port the local data server should accept coordinator requests on
   */
  public int getParticipantPort() {
    return participantPort;
  }
}
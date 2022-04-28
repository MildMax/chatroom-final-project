package chatserver;

/**
 * Contains port and addressing information for the local chat server and the central server in the
 * application
 */
public class ServerInfo {

  private final String id;
  private final String centralServerHostname;
  private final int centralServerPort;
  private final String hostname;
  private final int tcpPort;
  private final int rmiPort;
  private final int operationsPort;

  /**
   * Creates an instance of the ServerInfo object
   *
   * @param id the unique identifier for this chat server
   * @param centralServerHostname the host address supporting the central server
   * @param centralServerPort the port the central server is accepting registration requests on
   * @param hostname the hostname of the local chat server machine
   * @param tcpPort the port the local chat server should accept client TCP connections on
   * @param rmiPort the port the local chat server should accept client RMI requests on
   * @param operationsPort the port that the local chat server should accept central server requests on
   */
  ServerInfo(String id, String centralServerHostname, int centralServerPort,
      String hostname, int tcpPort, int rmiPort, int operationsPort) {
    this.id = id;
    this.centralServerHostname = centralServerHostname;
    this.centralServerPort = centralServerPort;
    this.hostname = hostname;
    this.tcpPort = tcpPort;
    this.rmiPort = rmiPort;
    this.operationsPort = operationsPort;
  }

  /**
   * Gets the unique identifier for this chat server
   *
   * @return the unique identifier for this chat server
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the hostname for the central server
   *
   * @return the hostname for the central server
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
   * Gets the hostname for the local chat server
   *
   * @return the hostname for the local chat server
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Gets the TCP port the local chat server should accept client TCP connections on
   *
   * @return the TCP port the local chat server should accept client TCP connections on
   */
  public int getTcpPort() {
    return tcpPort;
  }

  /**
   * Gets the port that the local chat server should accept client RMI requests on
   *
   * @return the port that the local chat server should accept client RMI requests on
   */
  public int getRmiPort() {
    return rmiPort;
  }

  /**
   * Gets the port that the local chat server should accept central server RMI requests on
   *
   * @return the port that the local chat server should accept central server RMI requests on
   */
  public int getOperationsPort() {
    return operationsPort;
  }
}

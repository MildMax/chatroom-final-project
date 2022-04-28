package client;

/**
 * Serverinfo class which holds the necessary port and addressing information for
 * the central server
 */
public class ServerInfo {

  private final String centralHost;
  private final int centralPort;
  private final boolean isTest;

  /**
   * Creates an instance of the ServerInfo object
   *
   * @param centralHost hostname of the machine supporting the central server
   * @param centralPort port the central server is accepting client requests on
   * @param isTest indicates if the client should run its local test file
   */
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

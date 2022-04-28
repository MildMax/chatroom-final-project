package client;

/**
 * Serverinfo class which holds the necessary information required by the 
 * client.
 *
 */
public class ServerInfo {

  private final String centralHost;
  private final int centralPort;
  private final boolean isTest;

  /**
   * server infor which is needed for the client.
   * @param centralHost host name
   * @param centralPort port number
   * @param isTest  boolean flag
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

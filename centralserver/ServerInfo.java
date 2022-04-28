package centralserver;

/**
 *  Contains port information used to start central server registries
 */
public class ServerInfo {
  private final int registerPort;
  private final int chatroomPort;
  private final int userPort;
  private final int coordinatorPort;

  /**
   * Initializes an instance of the ServerInfo object
   *
   * @param registerPort the port the central server accepts register requests from data and chat servers on
   * @param chatroomPort the port the central server accepts chatroom operation requests on
   * @param userPort the port the central server accepts user requests on
   * @param coordinatorPort the port that the central server accepts coordinator requests on
   */
  ServerInfo(int registerPort, int chatroomPort, int userPort, int coordinatorPort) {
    this.registerPort = registerPort;
    this.chatroomPort = chatroomPort;
    this.userPort = userPort;
    this.coordinatorPort = coordinatorPort;
  }

  /**
   * Returns the port used to register chat and server nodes
   *
   * @return the port used to register chat and server nodes
   */
  int getRegisterPort() { 
    return this.registerPort; 
  }

  /**
   * Returns the port used to facilitate chat server requests
   *
   * @return the port used to facilitate chat server requests
   */
  int getChatroomPort() { 
    return this.chatroomPort; 
    }

  /**
   * Returns the port used to facilitate user requests
   *
   * @return the port used to facilitate user requests
   */
  int getUserPort() { 
    return this.userPort; 
    }

  /**
   * Returns the port used to facilitate participant requests during 2 phase commit
   *
   * @return the port used to facilitate participant requests during 2 phase commit
   */
  public int getCoordinatorPort() {
    return coordinatorPort;
  }
}
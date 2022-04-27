package centralserver;

/**
 * information about the server like the prot number chatroom port number
 * the user port number all these are provided by this helper class for server.
 *
 */
public class ServerInfo {
  private final int registerPort;
  private final int chatroomPort;
  private final int userPort;
  private final int coordinatorPort;

  ServerInfo(int registerPort, int chatroomPort, int userPort, int coordinatorPort) {
    this.registerPort = registerPort;
    this.chatroomPort = chatroomPort;
    this.userPort = userPort;
    this.coordinatorPort = coordinatorPort;
  }

  int getRegisterPort() { 
    return this.registerPort; 
  }

  int getChatroomPort() { 
    return this.chatroomPort; 
    }

  int getUserPort() { 
    return this.userPort; 
    }

  public int getCoordinatorPort() {
    return coordinatorPort;
  }
}
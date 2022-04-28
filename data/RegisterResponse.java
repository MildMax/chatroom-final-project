package data;

import java.io.Serializable;

/**
 * Describes the response for a registration request from a data or server node
 */
public class RegisterResponse implements Serializable {

  private int port;

  /**
   * Creates an instance of the RegisterResponse object
   *
   * @param port a port that can be used by the chat or server node to contact the central server
   */
  public RegisterResponse(int port) {
    this.port = port;
  }

  /**
   * Get the port used to contact the central server
   *
   * @return the port number used to contact the central server
   */
  public int getPort() {
    return port;
  }
}

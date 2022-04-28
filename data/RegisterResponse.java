package data;

import java.io.Serializable;

/**
 * register response for the centralized application.
 *
 */
public class RegisterResponse implements Serializable {

  private int port;

  /**
   * port number of the register.
   * @param port port number
   */
  public RegisterResponse(int port) {
    this.port = port;
  }

  /**
   * to obtain current port number.
   * @return the current port number
   */
  public int getPort() {
    return port;
  }
}

package data;

import java.io.Serializable;

/**
 * Contains information pertaining to a chatroom on a specific chat server
 */
public class ChatroomResponse extends Response implements Serializable {
  private String name;
  private String address;
  private int tcpPort;
  private int registryPort;

  /**
   * Creates an instance of the ChatroomResponse object
   *
   * @param status status of the response
   * @param message describing the result of the operation
   * @param name name of the chatroom
   * @param address host address of the server hosting the provided chatroom
   * @param tcpPort port the chat server is accepting TCP connections on
   * @param registryPort port the chat server is accepting RMI requests on
   */
  public ChatroomResponse(ResponseStatus status, String message, 
      String name, String address, int tcpPort, int registryPort) {
    super(status, message);
    this.name = name;
    this.address = address;
    this.tcpPort = tcpPort;
    this.registryPort = registryPort;
  }

  /**
   * Creates an instance of the ChatroomResponse object when address and port data cannot
   * be determined
   *
   * @param status status of the chatroom response
   * @param message message describing the result of the operation
   */
  public ChatroomResponse(ResponseStatus status, String message) {
    super(status, message);
  }


  /**
   * Gets the name of the current chatroom.
   *
   * @return name of the current chatroom
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the host address of the chat server hosting the current chatroom
   *
   * @return the host address of the chat server hosting the current chatroom
   */
  public String getAddress() {
    return this.address;
  }

  /**
   * Gets the port that the chat server is accepting TCP connections on
   *
   * @return the port that the chat server is accepting TCP connections on
   */
  public int getTcpPort() { 
    return this.tcpPort; 
  }

  /**
   * Gets the port the chat server is accepting RMI requests on
   *
   * @return the port the chat server is accepting RMI requests on
   */
  public int getRegistryPort() {
    return this.registryPort; 
  }

}

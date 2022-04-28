package data;

import java.io.Serializable;

/**
 * Contains information regarding the address and host information for a server as well as the number
 * of users and chatrooms on that server
 */
public class ChatroomDataResponse implements Serializable {

  private final int chatrooms;
  private final int users;
  private final String hostname;
  private final int rmiPort;
  private final int tcpPort;

  /**
   * Creates an instance of the ChatroomDataResponse
   *
   * @param chatrooms number of chat rooms at a chat server
   * @param users number of users at a chat serer
   * @param hostname hostname of the machine supporting the chat server
   * @param rmiPort rmi port the chat server is accepting client RMI requests on
   * @param tcpPort tcp port the chat server is accepting client connections on
   */
  public ChatroomDataResponse(int chatrooms, int users, String hostname, int rmiPort, int tcpPort) {
    this.chatrooms = chatrooms;
    this.users = users;
    this.hostname = hostname;
    this.rmiPort = rmiPort;
    this.tcpPort = tcpPort;
  }

  /**
   * The number of chat rooms at the chat server
   *
   * @return the current number of chatrooms at the chat server
   */
  public int getChatrooms() {
    return this.chatrooms;
  }

  /**
   * The number of users at the chat server
   *
   * @return the current number of users at the chat server
   */
  public int getUsers() {
    return this.users;
  }

  /**
   * Get the hostname of the machine supporting the chat server
   *
   * @return the hostname of the machine supporting the chat server
   */
  public String getHostname() {
    return this.hostname; 
  }

  /**
   * Get the port the chat server is accepting client RMI requests on
   *
   * @return the port the chat server is accepting client RMI requests on
   */
  public int getRmiPort() { 
    return this.rmiPort; 
  }

  /**
   * Gets the port the chat server is accepting client TCP connections on
   *
   * @return the port the chat server is accepting client TCP connections on
   */
  public int getTcpPort() { 
    return this.tcpPort; 
  }
}

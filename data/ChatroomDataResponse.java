package data;

import java.io.Serializable;

/**
 * chatroomDataResponse implements serializable ans is responsbile
 * for all the chatroomresponses like hostname, port number.
 *
 */
public class ChatroomDataResponse implements Serializable {

  private final int chatrooms;
  private final int users;
  private final String hostname;
  private final int rmiPort;
  private final int tcpPort;

  /**
   * constructor which initiates all the objects used in the application.
   * @param chatrooms number of chat rooms
   * @param users number of users
   * @param hostname name of the host
   * @param rmiPort rmi port number 
   * @param tcpPort tcp port number
   */
  public ChatroomDataResponse(int chatrooms, int users, String hostname, int rmiPort, int tcpPort) {
    this.chatrooms = chatrooms;
    this.users = users;
    this.hostname = hostname;
    this.rmiPort = rmiPort;
    this.tcpPort = tcpPort;
  }

  /**
   * the number of chat rooms.
   * @return the current number of chatrooms.
   */
  public int getChatrooms() {
    return this.chatrooms;
  }

  /**
   * current number of users.
   * @return the current number of users.
   */
  public int getUsers() {
    return this.users;
  }

  /**
   * current host name.
   * @return the current host name.
   */
  public String getHostname() {
    return this.hostname; 
  }

  /**
   * current rmi port number.
   * @return the current rmi port number.
   */
  public int getRmiPort() { 
    return this.rmiPort; 
  }

  /**
   * current tcp port number.
   * @return the current tcp port number
   */
  public int getTcpPort() { 
    return this.tcpPort; 
  }
}

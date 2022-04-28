package data;


import chatserver.Chatroom;
import java.io.Serializable;

/**
 * chatroomResponse class which is responsible for responses 
 * of the chat rooms.
 *
 */
public class ChatroomResponse extends Response implements Serializable {
  private String name;
  private String address;
  private int tcpPort;
  private int registryPort;
  private Chatroom chatroom;

  /**
   * constructor of chatroomResponse which accepts the paremeters.
   * like response status, message, name, address and so on to initiate
   * single instance so as to maintain singleton property of design pattern.
   * @param status status of the response
   * @param message message of the response
   * @param name name of the chatroom
   * @param address address of the chta room
   * @param tcpPort port number of chatroom
   * @param registryPort registry port number 
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
   * chatroomresponse status and message.
   * @param status status of the chatroom response
   * @param message message of the chatroom response
   */
  public ChatroomResponse(ResponseStatus status, String message) {
    super(status, message);
  }


  /**
   * name of the current chatroom.
   * @return name of the current chatroom
   */
  public String getName() {
    return this.name;
  }

  /**
   * address of the current chatroom
   * @return address of the current chatroom
   */
  public String getAddress() {
    return this.address;
  }

  /**
   * port number of the current chatroom
   * @return port number of the current chatroom
   */
  public int getTcpPort() { 
    return this.tcpPort; 
  }

  /**
   * registry port of the current chatroom
   * @return registry port of the current chatroom
   */
  public int getRegistryPort() {
    return this.registryPort; 
  }

}

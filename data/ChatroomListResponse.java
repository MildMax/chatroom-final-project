package data;

import java.io.Serializable;
import java.util.List;

/**
 * An object containing a list of the names of available chatrooms in the system
 */

public class ChatroomListResponse implements Serializable {

  private final List<String> chatroomNames;

  /**
   * Creates an instance of the ChatroomListResponse object
   *
   * @param chatroomNames list of names of chatrooms in the system
   */
  public ChatroomListResponse(List<String> chatroomNames) {
    this.chatroomNames = chatroomNames;
  }

  /**
   * Gets the list of chatroom names in the system
   *
   * @return current list of chat room names
   */
  public List<String> getChatroomNames() { 
    return this.chatroomNames; 
  }
}

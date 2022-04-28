package data;

import java.io.Serializable;
import java.util.List;

/**
 * chatroomListResponses is used to get list of all chatroom names.
 *
 */

public class ChatroomListResponse implements Serializable {

  private final List<String> chatroomNames;

  /**
   * list of chat room names
   * @param chatroomNames list of chat room names
   */
  public ChatroomListResponse(List<String> chatroomNames) {
    this.chatroomNames = chatroomNames;
  }

  /**
   * current list of chat room names
   * @return current list of chat room names
   */
  public List<String> getChatroomNames() { 
    return this.chatroomNames; 
  }
}

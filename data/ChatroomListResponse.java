package data;

import java.io.Serializable;
import java.util.List;

public class ChatroomListResponse implements Serializable {

    private final List<String> chatroomNames;

    public ChatroomListResponse(List<String> chatroomNames) {
        this.chatroomNames = chatroomNames;
    }

    public List<String> getChatroomNames() { return this.chatroomNames; }
}

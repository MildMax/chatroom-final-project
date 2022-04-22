package data;

import java.io.Serializable;

import chatserver.ChatroomOperations;

public class ChatroomUserResponse implements Serializable {

    private final String username;
    private final String chatroomName;
    private final ChatroomCarrier chatroomCarrier;

    public ChatroomUserResponse(String username, String chatroomName) {
        this.username = username;
        this.chatroomName = chatroomName;
        this.chatroomCarrier = null;
    }

    public ChatroomUserResponse(String username, String chatroomName, ChatroomCarrier chatroomCarrier) {
        this.username = username;
        this.chatroomName = chatroomName;
        this.chatroomCarrier = chatroomCarrier;
    }

    public ChatroomCarrier getChatroomCarrier() { return chatroomCarrier; }

    public String getUsername() { return this.username; }

    public String getChatroomName() { return this.chatroomName; }

}

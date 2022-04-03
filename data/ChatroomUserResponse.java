package data;

import java.io.Serializable;

public class ChatroomUserResponse implements Serializable {

    private final String username;
    private final String chatroomName;

    public ChatroomUserResponse(String username, String chatroomName) {
        this.username = username;
        this.chatroomName = chatroomName;
    }

    public String getUsername() { return this.username; }

    public String getChatroomName() { return this.chatroomName; }

}

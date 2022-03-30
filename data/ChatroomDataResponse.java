package data;

import java.io.Serializable;

public class ChatroomDataResponse implements Serializable {

    private int chatrooms;
    private int users;

    public ChatroomDataResponse(int chatrooms, int users) {
        this.chatrooms = chatrooms;
        this.users = users;
    }

    public int getChatrooms() {
        return this.chatrooms;
    }

    public int getUsers() {
        return this.users;
    }
}

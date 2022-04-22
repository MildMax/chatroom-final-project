package data;

import java.io.Serializable;

import chatserver.Chatroom;


//Created a way to retrieve individual chatroom

public class ChatroomCarrier implements Serializable {
  private final Chatroom chatroom;

  public ChatroomCarrier(Chatroom chatroom){
    this.chatroom = chatroom;
  }

  public Chatroom getChatroom() {
    return chatroom;
  }
}


package chatserver;

import data.*;
import util.ThreadSafeStringFormatter;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ChatroomOperations extends UnicastRemoteObject implements IChatroomOperations {

    private final Map<String, Chatroom> roomList;
    private final Object roomListLock;
    private final ServerInfo serverInfo;

    public ChatroomOperations(Map<String, Chatroom> roomList, Object roomListLock, ServerInfo serverInfo) throws RemoteException {
        this.roomList = roomList;
        this.roomListLock = roomListLock;
        this.serverInfo = serverInfo;
    }

    @Override
    public Response createChatroom(String name, String username) throws RemoteException {
        synchronized (roomListLock) {
            for (String roomName : roomList.keySet()) {
                if (roomName.compareTo(name) == 0) {
                    return new Response(ResponseStatus.FAIL, "A room with the provided name already exists");
                }
            }

            roomList.put(name, new Chatroom(username, name));
            return new Response(ResponseStatus.OK, "success");
        }
    }

    @Override
    public Response deleteChatroom(String name) throws RemoteException {
        synchronized (roomListLock) {
            roomList.remove(name);
            return new Response(ResponseStatus.OK, "success");
        }
    }

    @Override
    public ChatroomDataResponse getChatroomData() throws RemoteException {
        synchronized (roomListLock) {
            int chatrooms = roomList.size();
            int users = 0;
            for (String roomName : roomList.keySet()) {
                users = users + roomList.get(roomName).getUserCount();
            }
            return new ChatroomDataResponse(chatrooms, users, serverInfo.getHostname(), serverInfo.getRmiPort(), serverInfo.getTcpPort());
        }
    }

    @Override
    public ChatroomListResponse getChatrooms() throws RemoteException {
        synchronized (roomListLock) {
            List<String> chatroomNames = new LinkedList<>();
            for (String roomName : roomList.keySet()) {
                chatroomNames.add(roomName);
            }
            return new ChatroomListResponse(chatroomNames);
        }
    }

    @Override
    public ChatroomUserResponse getChatroom(String name) throws RemoteException {
        synchronized (roomListLock) {
            Chatroom room = null;
            for (String roomName : roomList.keySet()) {
                if (roomName.compareTo(name) == 0) {
                    room = roomList.get(roomName);
                    break;
                }
            }
            if (room == null) {
                throw new RemoteException(ThreadSafeStringFormatter.format(
                        "Unable to get chatroom data for chatroom \"%s\"",
                        name
                ));
            }
            return new ChatroomUserResponse(room.getCreatorUsername(), room.getRoomName());
        }
    }
}

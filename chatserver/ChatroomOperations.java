package chatserver;

import data.*;
import util.ClientIPUtil;
import util.CristiansLogger;
import util.ThreadSafeStringFormatter;

import java.io.Serializable;
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
    public Response createChatroom(String name) throws RemoteException {

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received createChatroom request for chatroom \"%s\"",
                name
        ));

        synchronized (roomListLock) {
            for (String roomName : roomList.keySet()) {
                if (roomName.compareTo(name) == 0) {
                    CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Chatroom with name \"%s\" already exists",
                            name
                    ));
                    return new Response(ResponseStatus.FAIL, "A room with the provided name already exists");
                }
            }

            roomList.put(name, new Chatroom(name));
        }

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully created chatroom \"%s\"",
                name
        ));

        return new Response(ResponseStatus.OK, "success");
    }

    @Override
    public Response deleteChatroom(String name) throws RemoteException {

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received deleteChatroom request for chatroom \"%s\" from node at \"%s\"",
                name,
                ClientIPUtil.getClientIP()
        ));

        synchronized (roomListLock) {
            roomList.remove(name);
        }
        return new Response(ResponseStatus.OK, "success");
    }

    @Override
    public ChatroomDataResponse getChatroomData() throws RemoteException {

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received getChatroomData request from node at \"%s\"",
                ClientIPUtil.getClientIP()
        ));
        int chatrooms = roomList.size();
        int users = 0;

        synchronized (roomListLock) {
            for (String roomName : roomList.keySet()) {
                users = users + roomList.get(roomName).getUserCount();
            }
        }

        return new ChatroomDataResponse(chatrooms, users, serverInfo.getHostname(), serverInfo.getRmiPort(), serverInfo.getTcpPort());
    }

    @Override
    public ChatroomListResponse getChatrooms() throws RemoteException {

        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Received getChatrooms request from node at \"%s\"",
                ClientIPUtil.getClientIP()
        ));

        List<String> chatroomNames = new LinkedList<>();
        synchronized (roomListLock) {
            chatroomNames.addAll(roomList.keySet());
        }
        return new ChatroomListResponse(chatroomNames);
    }

}
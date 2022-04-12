package dataserver;

import data.IDataOperations;
import data.Response;
import data.ResponseStatus;
import util.Logger;
import util.ThreadSafeStringFormatter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class DataOperations extends UnicastRemoteObject implements IDataOperations {

    private final Map<String, String> userMap;
    private final Map<String, String> chatroomMap;
    private final Object chatroomMapLock;
    private final Object userMapLock;

    public DataOperations(Map<String, String> userMap, Object userMapLock, Map<String, String> chatroomMap, Object channelMapLock) throws RemoteException {
        this.userMap = userMap;
		this.chatroomMap = chatroomMap;
		this.chatroomMapLock = channelMapLock;
        this.userMapLock = userMapLock;
    }

    @Override
    public Response verifyUser(String username, String password) throws RemoteException {
        synchronized (userMapLock) {
            if (!userMap.containsKey(username)) {
                return new Response(ResponseStatus.FAIL, "User does not exist");
            }

            if (userMap.get(username).compareTo(password) != 0) {
                return new Response(ResponseStatus.FAIL, "User provided an invalid password");
            }

            return new Response(ResponseStatus.OK, "success");
        }
    }
    
    @Override
    public Response verifyOwnership(String chatroomName, String username) throws RemoteException {
    	synchronized (chatroomMapLock) {
            if (chatroomMap.get(chatroomName).compareTo(username) != 0) {
                return new Response(ResponseStatus.FAIL, "You are not the owner of this chatroom");
            }

            return new Response(ResponseStatus.OK, "success");
        }
    }

	@Override
	public boolean userExists(String username) throws RemoteException {
		return userMap.containsKey(username);
	}
	
	@Override
	public boolean chatroomExists(String chatroom) throws RemoteException {
		return chatroomMap.containsKey(chatroom);
	}
	
	@Override
	public synchronized boolean deleteChatroom(String chatroomName, Path dir) throws RemoteException {
		chatroomMap.remove(chatroomName);
		try {
			// Creates the file if it doesn't exist, if it does exist it will append to the file.
			FileWriter file = new FileWriter(dir.resolve("chatrooms.txt").toString());
			BufferedWriter writer = new BufferedWriter(file);
			for (Map.Entry<String, String> chatroom : chatroomMap.entrySet()) {
				writer.write(chatroom.getKey() + ":" + chatroom.getValue());
				writer.newLine(); 
			}
			writer.close();
			return true;
		} catch (IOException e) {
			Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
					"Something went very wrong deleting %s", 
					chatroomName
					));
			return false;
		}
	}

}

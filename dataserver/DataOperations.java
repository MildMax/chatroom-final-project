package dataserver;

import data.IDataOperations;
import data.Response;
import data.ResponseStatus;
import util.Logger;
import util.ThreadSafeStringFormatter;

import javax.swing.plaf.TableHeaderUI;
import java.io.*;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class DataOperations extends UnicastRemoteObject implements IDataOperations {

    private final Map<String, String> userMap;
    private final Map<String, String> chatroomMap;
    private final Object chatroomMapLock;
    private final Object userMapLock;
    private final ServerInfo serverInfo;

    public DataOperations(Map<String, String> userMap, Object userMapLock, Map<String, String> chatroomMap, Object channelMapLock, ServerInfo serverInfo) throws RemoteException {
        this.userMap = userMap;
		this.chatroomMap = chatroomMap;
		this.chatroomMapLock = channelMapLock;
        this.userMapLock = userMapLock;
        this.serverInfo = serverInfo;
    }

    @Override
    public Response verifyUser(String username, String password) throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Verifying user \"%s\"...",
				username
		));

        synchronized (userMapLock) {
            if (!userMap.containsKey(username)) {
            	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
            			"Unable to verify user \"%s\"",
						username
				));
                return new Response(ResponseStatus.FAIL, "User does not exist");
            }

            if (userMap.get(username).compareTo(password) != 0) {
				Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
						"Unable to verify user \"%s\"",
						username
				));
                return new Response(ResponseStatus.FAIL, "User provided an invalid password");
            }

            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
            		"Verified user \"%s\"",
					username
			));

            return new Response(ResponseStatus.OK, "success");
        }
    }
    
    @Override
    public Response verifyOwnership(String chatroomName, String username) throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Verifying ownership of chatroom \"%s\" for user \"%s\"",
				chatroomName,
				username
		));

    	synchronized (chatroomMapLock) {
            if (chatroomMap.get(chatroomName).compareTo(username) != 0) {
            	Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
            			"Unable to verify user \"%s\" owns chatroom \"%s\"",
						username,
						chatroomName
				));
                return new Response(ResponseStatus.FAIL, "You are not the owner of this chatroom");
            }

			Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
					"Successfully verified user \"%s\" owns chatroom \"%s\"",
					username,
					chatroomName
			));

            return new Response(ResponseStatus.OK, "success");
        }
    }

	@Override
	public boolean userExists(String username) throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Verifying that user \"%s\" exists...",
				username
		));

		return userMap.containsKey(username);
	}
	
	@Override
	public boolean chatroomExists(String chatroom) throws RemoteException {

		Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
				"Verifying that chatroom \"%s\" exists...",
				chatroom
		));

		return chatroomMap.containsKey(chatroom);
	}

	public void deleteChatroom(String chatroomName) {
		synchronized (chatroomMapLock) {
			chatroomMap.remove(chatroomName);
		}
	}

	public void createUser(String username, String password) {
		synchronized (userMapLock) {
			if (!userMap.containsKey(username)) {
				userMap.put(username, password);
			}  
		}
		
	}

	public void createChatroom(String chatroomName, String username) {
		synchronized(chatroomMapLock) {
			if (!chatroomMap.containsKey(username)) {
				chatroomMap.put(chatroomName, username);
			}
		}
	}
}

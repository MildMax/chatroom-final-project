package dataserver;

import data.IDataOperations;
import data.Response;
import data.ResponseStatus;
import util.CristiansLogger;
import util.ThreadSafeStringFormatter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class DataOperations extends UnicastRemoteObject implements IDataOperations {

    private final Map<String, String> userMap;
    private final Map<String, String> chatroomMap;
    private final Object chatroomMapLock;
    private final Object userMapLock;
    private final ServerInfo serverInfo;
    private final Path dir;

    public DataOperations(Map<String, String> userMap, Object userMapLock, Map<String, String> chatroomMap, Object channelMapLock, ServerInfo serverInfo) throws RemoteException {
        this.userMap = userMap;
		this.chatroomMap = chatroomMap;
		this.chatroomMapLock = channelMapLock;
        this.userMapLock = userMapLock;
        this.serverInfo = serverInfo;
		this.dir =  Paths.get("files_" + serverInfo.getId() + "/");
    }

    @Override
    public Response verifyUser(String username, String password) throws RemoteException {

    	CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Verifying user \"%s\"...",
				username
		));

        synchronized (userMapLock) {
            if (!userMap.containsKey(username)) {
            	CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
            			"Unable to verify user \"%s\"",
						username
				));
                return new Response(ResponseStatus.FAIL, "User does not exist");
            }

            if (userMap.get(username).compareTo(password) != 0) {
				CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
						"Unable to verify user \"%s\"",
						username
				));
                return new Response(ResponseStatus.FAIL, "User provided an invalid password");
            }

            CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
            		"Verified user \"%s\"",
					username
			));

            return new Response(ResponseStatus.OK, "success");
        }
    }
    
    @Override
    public Response verifyOwnership(String chatroomName, String username) throws RemoteException {

    	CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Verifying ownership of chatroom \"%s\" for user \"%s\"",
				chatroomName,
				username
		));

    	synchronized (chatroomMapLock) {
    		if (!chatroomMap.containsKey(chatroomName)) {
    			CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
    					"Unable to verify ownership of non-existent chatroom \"%s\" for user \"%s\"",
						chatroomName,
						username
				));
    			return new Response(ResponseStatus.FAIL, "Cannot verify ownership of non-existent chatroom");
			}
            if (chatroomMap.get(chatroomName).compareTo(username) != 0) {
            	CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
            			"Unable to verify user \"%s\" owns chatroom \"%s\"",
						username,
						chatroomName
				));
                return new Response(ResponseStatus.FAIL, "You are not the owner of this chatroom");
            }

			CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
					"Successfully verified user \"%s\" owns chatroom \"%s\"",
					username,
					chatroomName
			));

            return new Response(ResponseStatus.OK, "success");
        }
    }

	@Override
	public boolean userExists(String username) throws RemoteException {

    	CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Verifying that user \"%s\" exists...",
				username
		));

		return userMap.containsKey(username);
	}
	
	@Override
	public boolean chatroomExists(String chatroom) throws RemoteException {

		CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
				"Verifying that chatroom \"%s\" exists...",
				chatroom
		));

		return chatroomMap.containsKey(chatroom);
	}

	public void deleteChatroom(String chatroomName) {
		synchronized (chatroomMapLock) {
			chatroomMap.remove(chatroomName);

			String filename = dir.resolve("chatrooms.txt").toString();

			try {
				// Creates the file if it doesn't exist, if it does exist it will append to the file.
				FileWriter file = new FileWriter(filename, false);
				BufferedWriter writer = new BufferedWriter(file);
				for (String cName : chatroomMap.keySet()) {
					writer.write(ThreadSafeStringFormatter.format(
							"%s:%s",
							cName,
							chatroomMap.get(cName)
					));
					writer.newLine();
				}
				writer.close();
			} catch (IOException e) {
				CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
						"Something went very wrong writing to file %s",
						filename
				));
			}
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
			if (!chatroomMap.containsKey(chatroomName)) {
				chatroomMap.put(chatroomName, username);
				File chatLog = new File(dir.toString() + "/chatlogs/" + chatroomName + ".txt");
				try {
					if (chatLog.createNewFile()) {
						CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
								"Successfully created new chat log file for chatroom \"%s\"",
								chatroomName
						));
					} else {
						CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
								"Unable to create new chat log file for chatroom \"%s\"",
								chatroomName
						));
					}
				} catch (IOException e) {
					CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
							"There was an error when creating chat log file for chatroom \"%s\"",
							chatroomName
					));
				}
			}
		}
	}
}

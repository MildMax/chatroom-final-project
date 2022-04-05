package dataserver;

import data.Ack;
import data.IDataParticipant;
import data.Transaction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public class ParticipantOperations extends UnicastRemoteObject implements IDataParticipant {

    private final String coordinatorHostname;
    private final int coordinatorPort;
    private final String serverId;
    private final Path dir;
    private ConcurrentHashMap<Integer, Transaction> transactionMap;

    public ParticipantOperations(String coordinatorHostname, int coordinatorPort, String serverId) throws RemoteException {
        this.coordinatorHostname = coordinatorHostname;
        this.coordinatorPort = coordinatorPort;
        this.serverId = serverId;
        dir =  Paths.get("files_" + serverId + "/");
        transactionMap = new ConcurrentHashMap<Integer, Transaction>();
    }

    @Override
    public Ack canCommit(Transaction t) throws RemoteException {
    	// check if current node is committing on same key
    	int transactionKey = t.getTransactionIndex();
    	String key = t.getKey();
    	for (Transaction tx : transactionMap.values()) {
    		if (tx.getKey().equals(key)) {
    			return Ack.NO;
    		}
    	}
    	// We didn't find that key, so we are good to proceed.
		transactionMap.put(transactionKey, t);
		return Ack.YES;

        
    }

    @Override
    public void doCommit(Transaction t) throws RemoteException {
    	// Write to physical file (call have committed) (only if transaction op is create chatroom)
    	switch (t.getOp()) {
    		case CREATEUSER:
    			// Usernames and passwords stored in the format username:password 
    			writeFile("users.txt", t.getKey() + ":" + t.getValue());
    			break;
    		case CREATECHATROOM:
    			// Nothing actually needs to be done here, since the first message to a chat room will create the file.
    			break;
    		case DELETECHATROOM:
				File chatroom = new File(dir.resolve(t.getKey()).toString() + ".txt");
				if (chatroom.delete()) {
					//TODO what to log here? 
					System.out.println(String.format("Deleted file %s", t.getKey()));
				} else {
					System.err.format("Can't delete file");
				}
    			break;
    		case LOGMESSAGE:
    			writeFile(t.getKey() + ".txt", t.getValue());
    			System.out.println(coordinatorPort);
    			break;
    		default:
    			// TODO just log an error?
    			break;
    	}
    	
    	// TODO Contact central coordinator (get help)
    	
    	transactionMap.remove(t.getTransactionIndex());
    }

    @Override
    public void doAbort(Transaction t) throws RemoteException {
    	// check if index exists before removing (if it matches)
    	int transactionKey = t.getTransactionIndex();
    	if (transactionMap.containsKey(transactionKey)) {
    		transactionMap.remove(transactionKey);
    	}

    }

	@Override
	public synchronized void writeFile(String fileName, String data) throws RemoteException {
		try {
			// Creates the file if it doesn't exist, if it does exist it will append to the file.
			FileWriter file = new FileWriter(dir.resolve(fileName).toString(), true);
			BufferedWriter writer = new BufferedWriter(file);
			writer.write(data);
			writer.newLine();
			writer.close();
		} catch (IOException e) {
			System.err.format("Something went very wrong %s%n", e);
		}
	}
}

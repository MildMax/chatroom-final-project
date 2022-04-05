package centralserver;

import data.ICentralChatroomOperations;
import data.IDataOperations;
import data.IDataParticipant;
import util.RMIAccess;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class CentralChatroomOperations extends UnicastRemoteObject implements ICentralChatroomOperations {

    private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
    private final Object dataNodeParticipantsLock;

    public CentralChatroomOperations(List<RMIAccess<IDataParticipant>> dataNodesParticipants, Object dataNodeParticipantsLock) throws RemoteException {
        this.dataNodesParticipants = dataNodesParticipants;
        this.dataNodeParticipantsLock = dataNodeParticipantsLock;

    }

    @Override
    public void logChatMessage(String chatroom, String message) throws RemoteException {


        synchronized (dataNodeParticipantsLock) {
        	// TODO cancommit from here for all of the nodes (responds yes or no) 2only one key modified at a time
        	// if no retry or abort 
        	// if yes do commit 
            // do 2 pc here for chat messages
            // write to text file for the chatroom
        }
    }
}

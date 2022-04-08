package centralserver;

import data.Ack;
import data.ICentralChatroomOperations;
import data.IDataParticipant;
import data.Operations;
import data.Transaction;
import util.RMIAccess;

import java.rmi.NotBoundException;
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

    	Transaction t = new Transaction(Operations.LOGMESSAGE, chatroom, message);
    	int numDataNodes = dataNodesParticipants.size();
    	int votesYes = 0;
    	// TODO maybe do a retry?
        synchronized (dataNodeParticipantsLock) {
        	try {
	        	for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
	        		IDataParticipant dataNode;
					dataNode = participant.getAccess();
					
	        		if (dataNode == null) {
	        			return;
	        		}
	        		// Make sure everyone votes yes.
	        		if (dataNode.canCommit(t) == Ack.YES) {
	        			votesYes++;
	        		} else {
	        			// If we get a no or NA vote, just stop looping
	        			break;
	        		}
	        	}
	        	
	        	if (votesYes == numDataNodes) {
	        		for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
	            		IDataParticipant dataNode = participant.getAccess();
	            		dataNode.doCommit(t, participant);
	        		}
	        	} else {
	        		for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
	            		IDataParticipant dataNode = participant.getAccess();
	            		dataNode.doAbort(t);
	        		}
	        	}
        	} catch (RemoteException | NotBoundException e) {
				// TODO figure out logging
				e.printStackTrace();
        	}  
        }
    }
}

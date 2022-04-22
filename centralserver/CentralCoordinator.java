package centralserver;

import data.Ack;
import data.ICentralCoordinator;
import data.IDataParticipant;
import data.Transaction;
import util.ClientIPUtil;
import util.ThreadSafeStringFormatter;
import util.Logger;
import util.RMIAccess;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CentralCoordinator extends UnicastRemoteObject implements ICentralCoordinator {
	
	private final Map<Integer, Integer> commitMap;
	private final Map<Integer, Object> objectMap;
	private final Map<Integer, Ack> transactionDecisions;
    
	public CentralCoordinator() throws RemoteException {
    	commitMap = Collections.synchronizedMap(new HashMap<>());
    	objectMap = Collections.synchronizedMap(new HashMap<>());
    	transactionDecisions = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Sets the Coordinator decision for a transaction
     *
     * @param t transaction Coordinator sets decision on
     * @param coordinatorDecision the decision of the coordinator for the transaction (YES for doCommit, NO for doAbort, NA for no decision)
     */
    public void setCoordinatorDecision(Transaction t, Ack coordinatorDecision) {
    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Setting coordinator decision \"%s\" for transaction \"%s\"",
				coordinatorDecision,
				t.toString()
		));
        transactionDecisions.put(t.getTransactionIndex(), coordinatorDecision);
    }

    /**
     * Removes a decision made by a Coordinator
     *
     * @param t transaction that Coordinator has previously made a decision on
     */
    public void removeCoordinatorDecision(Transaction t) {
		Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
				"Removing coordinator decision for transaction \"%s\"",
				t.toString()
		));
    	transactionDecisions.remove(t.getTransactionIndex());
    }
	
	
    @Override
    public void haveCommitted(Transaction t, RMIAccess<IDataParticipant> p) throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Received haveCommitted request on transaction \"%s\" from participant at \"%s:%d\"",
				t.toString(),
				p.getHostname(),
				p.getPort()
		));

    	int count;

		int transactionId = t.getTransactionIndex();
		count = commitMap.get(transactionId);
		count--;

		if (count == 0) {
			commitMap.remove(transactionId);

			Object waitObject = objectMap.get(transactionId);
			if (waitObject != null) {
				synchronized (waitObject) {
					waitObject.notify();
				}
			}
			objectMap.remove(transactionId);

			Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
					"All participants have committed on transaction \"%s\"",
					t.toString()
			));
		} else {
			commitMap.put(transactionId, count);
		}
    }

    @Override
    public Ack getDecision(Transaction t) throws RemoteException {
    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"A participant at \"%s\" has requested a decision on transaction \"%s\"",
				ClientIPUtil.getClientIP(),
				t.toString()
		));
        return transactionDecisions.get(t.getTransactionIndex());
    }

    /**
     * Adds a wait object and a transaction id to maps for 2pc
     * 
     * @param t Transaction
     * @param waitObject Wait object
     */
	public void addWaitCommit(Transaction t, Object waitObject) {
		int transactionId = t.getTransactionIndex();
		int count = 0;
		if (commitMap.containsKey(transactionId)) {
			count = commitMap.get(transactionId);
		}
		
		if (!objectMap.containsKey(transactionId)) {
			objectMap.put(transactionId, waitObject);
		}
		commitMap.put(t.getTransactionIndex(), ++count);
	}
}

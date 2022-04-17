package centralserver;

import data.Ack;
import data.ICentralCoordinator;
import data.IDataParticipant;
import data.Transaction;
import util.ThreadSafeStringFormatter;
import util.Logger;
import util.RMIAccess;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CentralCoordinator extends UnicastRemoteObject implements ICentralCoordinator {
	
	private Map<Integer, Integer> commitMap;
	private Map<Integer, Object> objectMap;
	private Map<Integer, Ack> transactionDecisions;
    
	public CentralCoordinator() throws RemoteException {
    	commitMap = new ConcurrentHashMap<Integer, Integer>();
    	objectMap = new ConcurrentHashMap<Integer, Object>();
    	transactionDecisions = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Sets the Coordinator decision for a transaction
     *
     * @param t transaction Coordinator sets decision on
     * @param coordinatorDecision the decision of the coordinator for the transaction (YES for doCommit, NO for doAbort, NA for no decision)
     */
    public void setCoordinatorDecision(Transaction t, Ack coordinatorDecision) {
        transactionDecisions.put(t.getTransactionIndex(), coordinatorDecision);
    }

    /**
     * Removes a decision made by a Coordinator
     *
     * @param t transaction that Coordinator has previously made a decision on
     */
    public void removeCoordinatorDecision(Transaction t) {
        transactionDecisions.remove(t.getTransactionIndex());
    }
	
	
    @Override
    public void haveCommitted(Transaction t, RMIAccess<IDataParticipant> p) throws RemoteException {
    	int count;
    	Object waitObject;
		try {
			int transactionId = t.getTransactionIndex();
			waitObject = objectMap.get(transactionId);
			synchronized(waitObject) {
				count = commitMap.get(transactionId);
				count--;
				
				if (count == 0) {	
					waitObject.notify();
					commitMap.remove(transactionId);
					objectMap.remove(transactionId);
					Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
	                        "All participants have committed on transaction \"%s\"",
	                        t.toString()
	                ));
				} else {
					commitMap.put(transactionId, count);
				}
			}
		} catch (NullPointerException e) {
			Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "A key was not found, addWaitCommit wasnt called"
					));
		}
    }

    @Override
    public Ack getDecision(Transaction t) throws RemoteException {
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
		
		if (!objectMap.containsKey(waitObject)) {
			objectMap.put(transactionId, waitObject);
		}
		commitMap.put(t.getTransactionIndex(), ++count);
	}
}

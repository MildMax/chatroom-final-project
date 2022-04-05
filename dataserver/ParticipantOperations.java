package dataserver;

import data.Ack;
import data.IDataParticipant;
import data.Transaction;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ParticipantOperations extends UnicastRemoteObject implements IDataParticipant {

    private final String coordinatorHostname;
    private final int coordinatorPort;

    public ParticipantOperations(String coordinatorHostname, int coordinatorPort) throws RemoteException {
        this.coordinatorHostname = coordinatorHostname;
        this.coordinatorPort = coordinatorPort;
    }

    @Override
    public Ack canCommit(Transaction t) throws RemoteException {
    	// check if current node is commiting on same key (hashmap of transactions) remove on do or abort
        return null;
        
    }

    @Override
    public void doCommit(Transaction t) throws RemoteException {
    	// Write to physical file (call havecommited) (only if transaction op is create chatroom)
    }

    @Override
    public void doAbort(Transaction t) throws RemoteException {
    	// check if index lines up before removing (if ti matches)

    }
}

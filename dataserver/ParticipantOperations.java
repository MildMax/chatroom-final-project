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
        return null;
    }

    @Override
    public void doCommit(Transaction t) throws RemoteException {

    }

    @Override
    public void doAbort(Transaction t) throws RemoteException {

    }
}

package centralserver;

import data.Ack;
import data.ICentralCoordinator;
import data.IDataParticipant;
import data.Transaction;
import util.RMIAccess;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class CentralCoordinator extends UnicastRemoteObject implements ICentralCoordinator {

    public CentralCoordinator() throws RemoteException {
    }

    @Override
    public void haveCommitted(Transaction t, RMIAccess<IDataParticipant> p) throws RemoteException {

    }

    @Override
    public Ack getDecision(Transaction t) throws RemoteException {
        return null;
    }
}

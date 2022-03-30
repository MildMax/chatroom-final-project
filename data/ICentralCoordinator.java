package data;

import util.RMIAccess;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines operations supported by 2 Phase Commit Coordinator
 */
public interface ICentralCoordinator extends Remote {

    /**
     * Indicates a Participant has acted on a transaction
     *
     * @param t transaction Participant has acted on
     * @param p Participant that has acted on the transaction
     * @throws RemoteException if there is an error with RPC communication
     */
    void haveCommitted(Transaction t, RMIAccess<IDataParticipant> p) throws RemoteException;

    /**
     * Gets the decision made by a Coordinator on a transaction
     *
     * @param t transaction Coordinator has made a decision on
     * @return decision made by Coordinator on a transaction
     * @throws RemoteException if there is an error with RPC communication
     */
    Ack getDecision(Transaction t) throws RemoteException;

}

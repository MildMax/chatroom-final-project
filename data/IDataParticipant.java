package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// centralized server -> data node

/**
 * Defines operations supported by 2 Phase Commit Participant
 */
public interface IDataParticipant extends Remote {

    /**
     * Determines whether or not Participant has the resources to run 2 Phase Commit on a transaction
     *
     * @param t transaction to be committed
     * @return YES if the transaction can be committed, NO if transaction cannot be committed
     * @throws RemoteException if there is an error during RPC communication
     */
    Ack canCommit(Transaction t) throws RemoteException;

    /**
     * Commits a transaction to the local Participant's KeyValue store
     *
     * @param t transaction to commit
     * @throws RemoteException if there is an error during RPC communication
     */
    void doCommit(Transaction t) throws RemoteException;

    /**
     * Aborts commit on a transaction on the local Participant's KeyValue store
     *
     * @param t transaction to be aborted
     * @throws RemoteException if there is an error during RPC communication
     */
    void doAbort(Transaction t) throws RemoteException;
    
    /**
     * Does the commit of actually writing to the file system.
     * @param fileName filename of file to edit
     * @param data to write
     * @throws RemoteException if there is an error during RPC communication
     */
    void writeFile(String fileName, String data) throws RemoteException;

}
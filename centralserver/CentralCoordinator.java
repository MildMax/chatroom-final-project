package centralserver;

import data.Ack;
import data.ICentralCoordinator;
import data.IDataParticipant;
import data.Transaction;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import util.ClientIPUtil;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

/**
 * CentralCoordinator class which implements ICentralCoordinator interface.
 * which is responsible for setting coordinator decision for a transaction
 * removing coordinator decision for a transaction, getting decision for 
 * a transaction , waiting and commiting a transaction, checking is the 
 * transaction is commited by all the oparticipants
 */
public class CentralCoordinator extends 
    UnicastRemoteObject implements ICentralCoordinator {

  private final Map<Integer, Integer> commitMap;
  private final Map<Integer, Object> objectMap;
  private final Map<Integer, Ack> transactionDecisions;

  /**
   * Constructor of CentralCoordinator which is used to initiate object instances
   * such that the class maintains the singletone principle design pattern./
   * @throws RemoteException handles remote exception on initializing several objects.
   */
  public CentralCoordinator() throws RemoteException {
    commitMap = Collections.synchronizedMap(new HashMap<>());
    objectMap = Collections.synchronizedMap(new HashMap<>());
    transactionDecisions = Collections.synchronizedMap(new HashMap<>());
  }

  /**
   * Sets the Coordinator decision when ever a transaction is being carried out
   * and logs the respective message to the logger maintaining transparency.
   * @param t transaction Coordinator sets decision on
   * @param coordinatorDecision the decision of the coordinator for the transaction 
   * (YES for doCommit, NO for doAbort, NA for no decision)
   */
  public void setCoordinatorDecision(Transaction t, Ack coordinatorDecision) {
    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Setting coordinator decision \"%s\" for transaction \"%s\"",
        coordinatorDecision,
        t.toString()
        ));

    // track decision on transaction using unique transaction index
    transactionDecisions.put(t.getTransactionIndex(), coordinatorDecision);
  }

  /**
   * removeCoordinatorDecision is responsible for removing the decision
   * which is made by the coordinator on completing the transaction decision.
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

  /**
   * In order to maintain synchronixation among all participants.
   * it is necessary to know if all the participants have commited their transaction
   * and hence this method haveCommited checks if the transaction is commited by
   * all the participants
   */
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

    // if the count is 0, then all participants have committed
    if (count == 0) {
      commitMap.remove(transactionId);

      // notify the wait object in the doCommit thread to complete transaction
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
      // otherwise, replace count to indicate another participant has committed
    } else {
      commitMap.put(transactionId, count);
    }
  }

  /**
   * returns the transaction decision for the given transaction.
   */
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
   * Adds a wait object and a transaction id to maps for 2pc.
   * This method plays a prominent role in identifying every transaction 
   * with its unique id.
   * @param t Transaction
   * @param waitObject Wait object
   */
  public void addWaitCommit(Transaction t, Object waitObject) {
    int transactionId = t.getTransactionIndex();
    int count = 0;

    // if transaction is already being tracked, pull the current number of outstanding participants
    // awaiting commit
    if (commitMap.containsKey(transactionId)) {
      count = commitMap.get(transactionId);
    }
    // if there is no wait object associated with the transaction, 
    //associate wait object with transaction id
    if (!objectMap.containsKey(transactionId)) {
      objectMap.put(transactionId, waitObject);
    }

    // track transaction and the number of participants awaiting commit
    commitMap.put(t.getTransactionIndex(), ++count);
  }
}

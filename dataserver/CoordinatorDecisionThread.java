package dataserver;

import data.Ack;
import data.ICentralCoordinator;
import data.IDataParticipant;
import data.Transaction;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Manages operations for 2 Phase Commit at participant to retrieve Coordinator status on outstanding transactions
 */
public class CoordinatorDecisionThread extends Thread {

    private final RMIAccess<IDataParticipant> participant;
    private final String coordinatorHostname;
    private final int coordinatorPort;
    private final int decisionWaitTime;
    private final Transaction t;
    private boolean finished;

    public CoordinatorDecisionThread(String coordinatorHost, int coordinatorPort, Transaction t, RMIAccess<IDataParticipant> participant) {
        this.coordinatorHostname = coordinatorHost;
        this.coordinatorPort = coordinatorPort;
        this.decisionWaitTime = 1000;
        this.t = t;
        this.finished = false;
        this.participant = participant;
    }

    /**
     * Indicates that the Transaction has been completed at the replica server external from the decision thread
     */
    public void setFinished() {
        this.finished = true;
    }

    /**
     * Runs the CoordinatorDecisionThread. Requests state of transaction from Coordinator and operates on Coordinator
     * decision after specified timeout.
     */
    @Override
    public void run() {
        // initialize registry/interface to interact with coordinator
        RMIAccess<ICentralCoordinator> c = new RMIAccess<>(this.coordinatorHostname, this.coordinatorPort, "ICentralCoordinator");
        // set timeout to wait for coordinator to follow up on transaction
        // interrupted in either doCommit or doAbort operations in Participant object
        try {
            Thread.sleep(this.decisionWaitTime);
        } catch (InterruptedException e) {
            // if interrupt is received, simply exit the thread
            return;
        }

        // if finished value has been set, terminate thread
        if (this.finished) {
            return;
        }

        // get the decision from the Coordinator for the transaction
        // set default to NA so participant does not errantly perform action without explicitly
        // direction from the coordinator
        Ack coordinatorDecision;
        try {
            coordinatorDecision = c.getAccess().getDecision(this.t);
        } catch (RemoteException | NotBoundException e) {
            // if error looking up decision, log error and terminate decision thread
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Unable to retrieve decision on transaction \"%s\" from coordinator at \"%s:%d\"; terminating decision thread",
                    t.toString(),
                    this.coordinatorHostname,
                    this.coordinatorPort
            ));
            return;
        }

        // if decision from server is NO, run abort on transaction
        if (coordinatorDecision == Ack.NO) {
            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                    "Received doAbort decision from \"%s:%d\" for %s",
                    this.coordinatorHostname,
                    this.coordinatorPort,
                    t.toString()
            ));

            try {
                this.participant.getAccess().doAbort(t);
            } catch (RemoteException | NotBoundException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to abort on coordinator decision for transaction \"%s\"",
                        t.toString()
                ));
            }
        }
        // if server is YES, run commit on transaction
        else if (coordinatorDecision == Ack.YES) {
            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                    "Received doCommit decision from \"%s:%d\" for %s",
                    coordinatorHostname,
                    coordinatorPort,
                    this.t.toString()
            ));

            try {
                this.participant.getAccess().doCommit(t, this.participant);
            } catch (RemoteException | NotBoundException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to commit on coordinator decision for transaction \"%s\"",
                        t.toString()
                ));
            }

            // inform Coordinator transaction has been committed
            try {
                c.getAccess().haveCommitted(this.t, this.participant);
            } catch (RemoteException | NotBoundException e) {
                // do not terminate on error, proceed to remove operation from local transaction log
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to inform Coordinator at \"%s:%d\" of commit for transaction \"%s\"",
                        this.coordinatorHostname,
                        this.coordinatorPort,
                        this.t.toString()
                ));
            }
        }
        // otherwise, do nothing without decision from coordinator
        else {
            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                    "Coordinator at \"%s:%d\" has not made a decision about transaction \"%s\"",
                    this.coordinatorHostname,
                    this.coordinatorPort,
                    t.toString()
            ));
        }
    }

}

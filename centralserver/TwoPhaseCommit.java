package centralserver;

import data.Ack;
import data.IDataParticipant;
import data.Response;
import data.Transaction;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

public class TwoPhaseCommit {

    /**
     * A generic two phase commit function that doesn't require additional resources to be generated at external
     * nodes
     *
     * @param dataNodeParticipantsLock
     * @param dataNodesParticipants
     * @param t
     * @param coordinator
     * @return
     */
    public boolean GenericCommit(Object dataNodeParticipantsLock, List<RMIAccess<IDataParticipant>> dataNodesParticipants, Transaction t, CentralCoordinator coordinator) {

        coordinator.setCoordinatorDecision(t, Ack.NA);

        boolean success = canCommit(t, dataNodesParticipants, dataNodeParticipantsLock);

        if (success) {
            coordinator.setCoordinatorDecision(t, Ack.YES);
            doCommit(t, dataNodesParticipants, dataNodeParticipantsLock, coordinator);
            coordinator.removeCoordinatorDecision(t);
            return true;
        } else {
            coordinator.setCoordinatorDecision(t, Ack.NO);
            doAbort(t, dataNodesParticipants, dataNodeParticipantsLock);
            coordinator.removeCoordinatorDecision(t);
            return false;
        }
    }

    public boolean canCommit(Transaction t, List<RMIAccess<IDataParticipant>> dataNodesParticipants, Object dataNodeParticipantsLock) {

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Initiating canCommit request on transaction \"%s\"",
                t.toString()
        ));

        // create a canCommit thread for each participant data node in the system
        List<CanCommitThread> commitThreads = new LinkedList<>();
        synchronized (dataNodeParticipantsLock) {
            for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
                CanCommitThread thread = new CanCommitThread(participant, t);
                commitThreads.add(thread);
            }
        }

        // initiate canCommit for each data node in the system
        for (CanCommitThread commitThread : commitThreads) {
            commitThread.start();
        }

        // if all participants do not indicate NO, the "true" value will indicate to the coordinator that
        // all participants are ready and should issue a doCommit request to each participant
        boolean success = true;

        // collect the thread from each data node
        for (CanCommitThread thread : commitThreads) {
            // collect thread
            try {
                thread.join();
            } catch (InterruptedException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Unable to join canCommit thread for participant \"%s:%d\" on transaction \"%s\"",
                        thread.getParticipant().getHostname(),
                        thread.getParticipant().getPort(),
                        thread.getTransaction().toString()
                ));
                success = false;
                continue;
            }
            // if the result is no, set success to false to indicate to central coordinator that it should issue
            // a doAbort request to all participant nodes
            if (thread.getResult() == Ack.NO) {
                success = false;
            }
            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                    "Participant at \"%s:%d\" voted \"%s\" on transaction \"%s\"",
                    thread.getParticipant().getHostname(),
                    thread.getParticipant().getPort(),
                    thread.getResult(),
                    t.toString()
            ));
        }

        return success;
    }

    public void doCommit (Transaction t, List<RMIAccess<IDataParticipant>> dataNodesParticipants, Object dataNodeParticipantsLock, CentralCoordinator coordinator) {

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Initiating doCommit request on transaction \"%s\"",
                t.toString()
        ));

        // create a wait object that will be notified after all contacted participants indicate haveCommitted
        Object waitObject = new Object();

        // iterate through the list of data node participants and issue a doCommit request for the provided transaction
        synchronized (dataNodeParticipantsLock) {
            for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
                Thread commitThread = null;
                try {

                    // create a thread that issues a doCommit request to the participant
                    commitThread = new Thread(new Runnable() {
                        IDataParticipant dataNode = participant.getAccess();

                        @Override
                        public void run() {
                            try {
                                dataNode.doCommit(t, participant);
                            } catch (RemoteException e) {
                                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                        "Something went wrong starting a thread at %s",
                                        participant.getHostname()
                                ));
                            }
                        }
                    });
                }
                // if there is an error starting the thread, log the error and continue iterating through data nodes
                catch (Exception e) {
                    Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact data node at \"%s:%d\" during doCommit, skipping...",
                            participant.getHostname(),
                            participant.getPort()
                    ));
                    continue;
                }

                // start thread and indicate to the central coordinator that it should wait for this participant
                // to indicate haveCommitted before continuing execution via the addWaitCommit method
                commitThread.start();
                coordinator.addWaitCommit(t, waitObject);
            }
        }

        // wait for all participants to indicate haveCommitted before finishing the transaction
        synchronized (waitObject) {
            try {
                // set wait object to timeout after 1 second to promote liveness in the system
                waitObject.wait(1000);
            } catch (InterruptedException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Something went wrong with the doCommit wait on transaction \"%s\": \"%s\"",
                        t.toString(),
                        e.getMessage()
                ));
            }
        }
    }

    public void doAbort(Transaction t, List<RMIAccess<IDataParticipant>> dataNodesParticipants, Object dataNodeParticipantsLock) {

        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Initiating doAbort on transaction \"%s\"",
                t.toString()
        ));

        synchronized (dataNodeParticipantsLock) {
            // iterate through data node participants and issue a doAbort request to each one
            for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
                try {

                    // create a thread for that issues a doAbort request to the data node participant
                    Thread abortThread = new Thread(new Runnable() {
                        IDataParticipant dataNode = participant.getAccess();

                        @Override
                        public void run() {
                            try {
                                dataNode.doAbort(t);
                            } catch (RemoteException e) {
                                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                        "Something went wrong starting a thread at %s",
                                        participant.getHostname()
                                ));
                            }
                        }
                    });
                    abortThread.start();
                } catch (RemoteException | NotBoundException e) {
                    Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact data node at \"%s:%d\", skipping...",
                            participant.getHostname(),
                            participant.getPort()
                    ));
                }

            }
        }
    }

    private static class CanCommitThread extends Thread {

        private final RMIAccess<IDataParticipant> participant;
        private final Transaction t;
        private Ack result = Ack.NA;

        CanCommitThread(RMIAccess<IDataParticipant> participant, Transaction t) {
            this.participant = participant;
            this.t = t;
        }

        @Override
        public void run() {
            // issue a canCommit request to the provided participant and set the result of the request
            // for this thread object
            try {
                this.result = this.participant.getAccess().canCommit(t, participant);
            } catch (RemoteException | NotBoundException e) {
                Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                        "Something went wrong during canCommit for node \"%s\"",
                        participant.getHostname()
                ));
            }
        }

        public Ack getResult() { return this.result; }

        public RMIAccess<IDataParticipant> getParticipant() { return this.participant; }

        public Transaction getTransaction() {
            return t;
        }
    }
}

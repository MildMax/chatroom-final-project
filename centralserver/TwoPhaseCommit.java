package centralserver;

import data.Ack;
import data.IDataParticipant;
import data.Transaction;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
    public static synchronized boolean GenericCommit(Object dataNodeParticipantsLock, List<RMIAccess<IDataParticipant>> dataNodesParticipants, Transaction t, CentralCoordinator coordinator) {

        coordinator.setCoordinatorDecision(t, Ack.NA);

        int votesYes = 0;
        int nodesContacted = 0;
        // TODO maybe do a retry?
        // if there's an error, create a string for errorMessage to send back to the client
        synchronized (dataNodeParticipantsLock) {
            for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
                IDataParticipant dataNode = null;
                // TODO we don't want to fail if we can't access node
                // TODO move try catch for remote exception/host exception here and continue attempting on additional nodes
                try {
                    dataNode = participant.getAccess();
                } catch (NotBoundException | RemoteException e) {
                    Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                            "Unable to reach data node at \"%s:%d\" during canCommit, skipping...",
                            participant.getHostname(),
                            participant.getPort()
                    ));
                    continue;
                }

                // Make sure everyone votes yes.
                try {
                    if (dataNode.canCommit(t, participant) == Ack.YES) {
                        nodesContacted++;
                        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                                "Participant node at \"%s:%d\" voted YES",
                                participant.getHostname(),
                                participant.getPort()
                        ));
                        votesYes++;
                    } else {
                        nodesContacted++;
                        // If we get a no or NA vote, just stop looping
                        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                "Participant node at \"%s:%d\" voted NO",
                                participant.getHostname(),
                                participant.getPort()
                        ));
                    }
                } catch (RemoteException e) {
                    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                            "Unable to contact participant node at \"%s:%d\", skipping..."
                    ));
                }
            }

            if (votesYes == nodesContacted) {
                coordinator.setCoordinatorDecision(t, Ack.YES);
                Object waitObject = new Object();
                for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
                    Thread commitThread = null;
                    try {
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
                    } catch (Exception e) {
                        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                "Unable to contact data node at \"%s:%d\" during doCommit, skipping...",
                                participant.getHostname(),
                                participant.getPort()
                        ));
                        continue;
                    }

                    commitThread.start();
                    coordinator.addWaitCommit(t, waitObject);

                }
                synchronized(waitObject) {
                    try {
                        waitObject.wait();
                    } catch (InterruptedException e) {
                        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                "Something went wrong with the wait \"%s\"",
                                e
                        ));
                    }
                }
                coordinator.removeCoordinatorDecision(t);
                return true;
            } else {
                coordinator.setCoordinatorDecision(t, Ack.NO);
                forceAbort(t, dataNodesParticipants);
                coordinator.removeCoordinatorDecision(t);
                return false;
            }
        }


    }

    /**
     * A helper function to force an abort to be called on all nodes.
     * @param t Transaction
     */
    public static void forceAbort(Transaction t, List<RMIAccess<IDataParticipant>> dataNodesParticipants) {
        for (RMIAccess<IDataParticipant> participant : dataNodesParticipants) {
            IDataParticipant dataNode;
            try {
                dataNode = participant.getAccess();
                dataNode.doAbort(t);
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

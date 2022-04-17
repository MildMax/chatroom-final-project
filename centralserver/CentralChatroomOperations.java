package centralserver;

import data.*;
import util.ClientIPUtil;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class CentralChatroomOperations extends UnicastRemoteObject implements ICentralChatroomOperations {

    private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
    private final Object dataNodeParticipantsLock;
	private final CentralCoordinator coordinator;

    public CentralChatroomOperations(List<RMIAccess<IDataParticipant>> dataNodesParticipants, Object dataNodeParticipantsLock, CentralCoordinator coordinator) throws RemoteException {
        this.dataNodesParticipants = dataNodesParticipants;
        this.dataNodeParticipantsLock = dataNodeParticipantsLock;
        this.coordinator = coordinator;

    }

    @Override
    public Response logChatMessage(String chatroom, String message) throws RemoteException {

    	Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
    			"Received log chat request for chatroom \"%s\" on message \"%s\" from chat node at \"%s\"",
				chatroom,
				message,
				ClientIPUtil.getClientIP()
		));

    	Transaction t = new Transaction(Operations.LOGMESSAGE, chatroom, message);
		int nodesContacted = 0;
    	int votesYes = 0;
    	String errorMessage = "";
    	boolean success = false;
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

				nodesContacted++;

				// Make sure everyone votes yes.
				if (dataNode.canCommit(t) == Ack.YES) {
					Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
							"Participant node at \"%s:%d\" voted YES",
							participant.getHostname(),
							participant.getPort()
					));
					votesYes++;
				} else {
					// If we get a no or NA vote, just stop looping
					errorMessage = "Please try again";
					Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
							"Participant node at \"%s:%d\" voted NO",
							participant.getHostname(),
							participant.getPort()
					));
				}
			}

			if (votesYes == nodesContacted) {
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
				success = true;
			} else {
				forceAbort(t);
			}
		}

		if (success) {
			Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
					"Successfully logged chat message for chatroom \"%s\"",
					chatroom
			));
			return new Response(ResponseStatus.OK, "success");
		} else {
			Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
					"Failed to log chat message for chatroom \"%s\"",
					chatroom
			));
			return new Response(ResponseStatus.FAIL, errorMessage);
		}
    }

	/**
	 * A helper function to force an abort to be called on all nodes.
	 * @param t Transaction
	 */
	public void forceAbort(Transaction t) {
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

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
		TwoPhaseCommit committer = new TwoPhaseCommit();
		boolean success = committer.GenericCommit(dataNodeParticipantsLock, dataNodesParticipants, t, coordinator);

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
			return new Response(ResponseStatus.FAIL, "Unable to log chat message");
		}
    }
}

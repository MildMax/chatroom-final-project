package centralserver;

import data.ICentralChatroomOperations;
import data.IDataParticipant;
import data.Operations;
import data.Response;
import data.ResponseStatus;
import data.Transaction;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import util.ClientIPUtil;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

/**
 * CentralChatroomOperations class acts as a initiator central server
 * operations pertaining to existing chatrooms. Uses two phase commit for logging the
 * messages from the central chat room.
 */
public class CentralChatroomOperations extends 
    UnicastRemoteObject implements ICentralChatroomOperations {

  private final List<RMIAccess<IDataParticipant>> dataNodesParticipants;
  private final Object dataNodeParticipantsLock;
  private final CentralCoordinator coordinator;

  /**
   * Constructor for CentralChatroomOperations class which accepts the paremeters
   * for participants list, locks of participants data, coordinator object along
   * side handling the remote exceptions.
   *
   * @param dataNodesParticipants list of participants of data nodes.
   * @param dataNodeParticipantsLock locks on the given dataNodeParticipants list.
   * @param coordinator central server coordinator for all the data nodes.
   * @throws RemoteException handles any exception cause by the remote access of these nodes.
   */
  public CentralChatroomOperations(List<RMIAccess<IDataParticipant>> 
      dataNodesParticipants, Object dataNodeParticipantsLock, 
        CentralCoordinator coordinator) throws RemoteException {
    this.dataNodesParticipants = dataNodesParticipants;
    this.dataNodeParticipantsLock = dataNodeParticipantsLock;
    this.coordinator = coordinator;

  }

  /**
   * Logs a chatroom message to available data servers in the system using 2pc
   *
   * @param chatroom name of the chat room the message was sent to
   * @param message the message to be logged
   * @return a response indicating whether the operation succeeded or not
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public Response logChatMessage(String chatroom, String message) throws RemoteException {

    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received log chat request for chatroom \"%s\" on message \"%s\" from chat node at \"%s\"",
        chatroom,
        message,
        ClientIPUtil.getClientIP()
        ));

    // create a transaction to be committed containing the message received from the chat server
    Transaction t = new Transaction(Operations.LOGMESSAGE, chatroom, message);

    // run two phase commit
    TwoPhaseCommit committer = new TwoPhaseCommit();
    boolean success = 
        committer.GenericCommit(dataNodeParticipantsLock, dataNodesParticipants, t, coordinator);

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

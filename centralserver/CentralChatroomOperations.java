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
 * CentralChatroomOperations class acts as a initiator for all the operations
 * which are performed in a chatroom. Using two phase commit for logging the 
 * mesages from the central chat room.
 *
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
   * @param dataNodesParticipants list of participants of datanodes.
   * @param dataNodeParticipantsLock locks on the given dataNodeParticipants list.
   * @param coordinator coordinator of all the datanodees.
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
   * logChatMessage is a method which is being overriden from ICentralCharoomOperations
   * interface. This accepts chatroom identifier as a parameter and the message 
   * which is being sent to the respective chatroom and using two phase commit it
   * is being logged.
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

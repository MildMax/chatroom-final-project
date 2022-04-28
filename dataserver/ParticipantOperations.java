package dataserver;

import data.*;
import util.CristiansLogger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
/**
 * participant operation class which implements IDataParticipant
 * is responsible for all the operations which can be oerformed by participants
 * commiting, aborting and writing files.
 *
 */
public class ParticipantOperations extends UnicastRemoteObject implements IDataParticipant {

  private final String coordinatorHostname;
  private final int coordinatorPort;
  private final DataOperations operationsEngine;
  private final Path dir;
  private final Map<Integer, Transaction> transactionMap;
  private final Map<Integer, CoordinatorDecisionThread> decisionThreadMap;

  /**
   * constructor of the participantoperations class.
   * @param coordinatorHostname central cooridinator name
   * @param coordinatorPort port number of coordinato
   * @param serverId id of the server.
   * @param operationsEngine operations enginer
   * @throws RemoteException handles remote object invocations
   */
  
  public ParticipantOperations(String coordinatorHostname, 
      int coordinatorPort, String serverId, 
      DataOperations operationsEngine) throws RemoteException {
    this.coordinatorHostname = coordinatorHostname;
    this.coordinatorPort = coordinatorPort;
    this.operationsEngine = operationsEngine;
    this.dir =  Paths.get("files_" + serverId + "/");
    this.transactionMap = Collections.synchronizedMap(new HashMap<>());
    this.decisionThreadMap = Collections.synchronizedMap(new HashMap<>());
  }

  @Override
  public Ack canCommit(Transaction t, RMIAccess<IDataParticipant> p) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received canCommit on transaction \"%s\"",
        t.toString()
        ));

    // specific to create user -- handle here since resource is tracked locally
    // if user exists, must say no
    if (t.getOp() == Operations.CREATEUSER && operationsEngine.userExists(t.getKey())) {
      return Ack.NO;
    }

    // check if current node is committing on same key
    int transactionKey = t.getTransactionIndex();
    String key = t.getKey();
    for (Transaction tx : transactionMap.values()) {
      if (tx.getKey().equals(key)) {
        return Ack.NO;
      }
    }
    // We didn't find that key, so we are good to proceed.
    transactionMap.put(transactionKey, t);
    CoordinatorDecisionThread thread = 
        new CoordinatorDecisionThread(this.coordinatorHostname, this.coordinatorPort, t, p);
    thread.start();
    decisionThreadMap.put(transactionKey, thread);
    return Ack.YES;
  }

  @Override
  public void doCommit(Transaction t, RMIAccess<IDataParticipant> p) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received doCommit on transaction \"%s\"",
        t.toString()
        ));

    // set the decision thread associated to the transaction to be finished
    // so the local data participant does not call getDecision on a transaction
    // it has already committed
    CoordinatorDecisionThread th = decisionThreadMap.get(t.getTransactionIndex());
    if (th != null) {
      th.setFinished();
    }
    decisionThreadMap.remove(t.getTransactionIndex());

    // determine the type of operation provided in the transaction
    switch (t.getOp()) {
      // if create user, verify that user does not exist and then write the
      // username:password combination to the users.txt file and create the user
      // in local memory
      case CREATEUSER:
        if (operationsEngine.userExists(t.getKey())) {
          // enforce at most once semantics if multiple concurrent requests are received
          CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "User \"%s\" already been created in concurrent transaction",
              t.getKey()
              ));
          break;
        }
        // Usernames and passwords stored in the format username:password 
        writeFile("users.txt", t.getKey() + ":" + t.getValue());
        operationsEngine.createUser(t.getKey(), t.getValue());
        break;
        // if create chatroom, verify that the chatroom des not exist, and then write the
        // chatroomname:username combination to the chatrooms.txt file and track
        // the chatroom in local memory
      case CREATECHATROOM:
        if (operationsEngine.chatroomExists(t.getKey())) {
          // enforce at most once semantics if multiple concurrent requests are received
          CristiansLogger.writeMessageToLog("Chatroom \"%s\" has already "
              + "been created in concurrent transaction");
          break;
        }
        // Chatroom ownership is stored in the format chatroom:user
        writeFile("chatrooms.txt", t.getKey() + ":" + t.getValue());
        operationsEngine.createChatroom(t.getKey(), t.getValue());
        break;
        // if delete chatroom, ensure that the chatroom does not exist
        // then, delete the log file associated with the chatroom
        // and delete the chatroom from local memory
      case DELETECHATROOM:
        if (!operationsEngine.chatroomExists(t.getKey())) {
          // enforce at most once semantics if multiple concurrent requests are received
          CristiansLogger.writeMessageToLog("Chatroom \"%s\" has already "
              + "been deleted in concurrent transaction");
          break;
        }
        File chatroom = new File(dir.resolve("chatlogs/" + t.getKey()).toString() + ".txt");
        operationsEngine.deleteChatroom(t.getKey());
        if (chatroom.delete()) {
          CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Deleted chatroom file for chatroom \"%s\"",
              t.getKey()
              ));
        } else {
          CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
              "Failed to delete chatroom file \"%s\"",
              t.getKey()
              ));
        }
        break;
        // if log message, write the message store in the transaction value to the
        // chatlog file for the chatroom
      case LOGMESSAGE:
        writeFile("chatlogs/" + t.getKey() + ".txt", t.getValue());
        break;
        // otherwise, log that an invalid command has been received
      default:
        CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Unable to operate on invalid command \"%s\"",
            t.getOp().toString()
            ));
        break;
    }

    // after operation has been run, contact coordinator and indicate 
    //that the local data participant node
    // haveCommitted on the provided transaction
    RMIAccess<ICentralCoordinator> coordinator = 
        new RMIAccess<>(coordinatorHostname, coordinatorPort, "ICentralCoordinator");
    ICentralCoordinator coord;
    try {
      coord = coordinator.getAccess();
      coord.haveCommitted(t, p);
    } catch (RemoteException | NotBoundException e) {
      CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Unable to contact coordinator for haveCommitted at \"%s:%d\"",
          coordinatorHostname,
          coordinatorPort
          ));
    }

    // remove the transaction from the local transaction map
    transactionMap.remove(t.getTransactionIndex());
  }

  @Override
  public void doAbort(Transaction t) throws RemoteException {

    // set the decision thread associated to the transaction to be finished
    // so the local data participant does not call getDecision on a transaction
    // it has already aborted
    CoordinatorDecisionThread th = decisionThreadMap.get(t.getTransactionIndex());
    if (th != null) {
      th.setFinished();
    }
    decisionThreadMap.remove(t.getTransactionIndex());

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Received doAbort on transaction \"%s\"",
        t.toString()
        ));

    // check if index exists before removing (if it matches)
    int transactionKey = t.getTransactionIndex();
    if (transactionMap.containsKey(transactionKey)) {
      transactionMap.remove(transactionKey);
    }

  }

  /**
   * fulfills the functionality of writing all the data into the file.
   * taking file name as input and the data to be written.
   * @param fileName name of the file
   * @param data data to be entered
   * @return the boolen of sucess or failure.
   * @throws RemoteException handles remote object invocations
   */
  public synchronized boolean writeFile(String fileName, String data) throws RemoteException {
    try {
      // Creates the file if it doesn't exist, if it does exist it will append to the file.
      FileWriter file = new FileWriter(dir.resolve(fileName).toString(), true);
      BufferedWriter writer = new BufferedWriter(file);
      // write data
      writer.write(data);
      // write newline
      writer.newLine();
      // clean resources
      writer.close();
      return true;
    } catch (IOException e) {
      CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "Something went very wrong writing to file \"%s\"",
          fileName
          ));
      return false;
    }
  }


}

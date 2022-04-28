package dataserver;

import data.IDataOperations;
import data.Response;
import data.ResponseStatus;
import util.CristiansLogger;
import util.ThreadSafeStringFormatter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

/**
 * DataOperations class which implements IDataOperations and which fulfills the
 * responsibility of data server operations on local server resources
 */
public class DataOperations extends UnicastRemoteObject implements IDataOperations {

  private final Map<String, String> userMap;
  private final Map<String, String> chatroomMap;
  private final Object chatroomMapLock;
  private final Object userMapLock;
  private final ServerInfo serverInfo;
  private final Path dir;

  /**
   * Creates an instance of the DataOperations engine
   *
   * @param userMap a map of users registered at the data server
   * @param userMapLock locks the userMap resource
   * @param chatroomMap a map of chatrooms in the system and their owners
   * @param channelMapLock locks the chatroomMap resource
   * @param serverInfo port and addressing information required to run the data server
   * @throws RemoteException if there is an error during remote communication
   */
  public DataOperations(Map<String, String> userMap, 
      Object userMapLock, Map<String, String> chatroomMap, 
      Object channelMapLock, ServerInfo serverInfo) throws RemoteException {
    this.userMap = userMap;
    this.chatroomMap = chatroomMap;
    this.chatroomMapLock = channelMapLock;
    this.userMapLock = userMapLock;
    this.serverInfo = serverInfo;
    this.dir =  Paths.get("files_" + serverInfo.getId() + "/");
  }

  /**
   * Verifies that a user exists and that the provided password matches what is on file for the user
   *
   * @param username the user to verify
   * @param password the password to be checked against the user info on file
   * @return a response indicating whether the user has been verified or not
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public Response verifyUser(String username, String password) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Verifying user \"%s\"...",
        username
        ));

    synchronized (userMapLock) {
      // if the map of users does not contain the user, then user does not exist
      // indicate user cannot be verified
      if (!userMap.containsKey(username)) {
        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
            "Unable to verify user \"%s\"",
            username
            ));
        return new Response(ResponseStatus.FAIL, "User does not exist");
      }

      // if the password provided does not match the user's password, indicate
      // that the user cannot be verified
      if (userMap.get(username).compareTo(password) != 0) {
        CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
            "Unable to verify user \"%s\"",
            username
            ));
        return new Response(ResponseStatus.FAIL, "User provided an invalid password");
      }

      // otherwise, username and password are correct, indicate user has been
      // successfully verified
      CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
          "Verified user \"%s\"",
          username
          ));

      return new Response(ResponseStatus.OK, "success");
    }
  }

  /**
   * Verifies that a user is the original creater and owner of a chatroom
   *
   * @param chatroomName the name of the chatroom to check the owner of
   * @param username the username to be verified as the owner of the chatroom
   * @return a response indicating whether the user is the owner of the chatroom or not
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public Response verifyOwnership(String chatroomName, String username) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Verifying ownership of chatroom \"%s\" for user \"%s\"",
        chatroomName,
        username
        ));

    synchronized (chatroomMapLock) {
      // ensure that the chatroom exists
      // if not, indicate chatroom does not exist and return fail response
      if (!chatroomMap.containsKey(chatroomName)) {
        CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Unable to verify ownership of non-existent chatroom \"%s\" for user \"%s\"",
            chatroomName,
            username
            ));
        return new Response(ResponseStatus.FAIL, "Cannot verify "
            + "ownership of non-existent chatroom");
      }
      // if the username associated with the chatroom does not match the username provided,
      // indicate user does not own chatroom and cannot delete the chatroom
      if (chatroomMap.get(chatroomName).compareTo(username) != 0) {
        CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Unable to verify user \"%s\" owns chatroom \"%s\"",
            username,
            chatroomName
            ));
        return new Response(ResponseStatus.FAIL, "You are not the owner of this chatroom");
      }

      // otherwise, indicate that the user does own the chatroom and is allowed
      // to delete resources related to the chatroom from the application
      CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
          "Successfully verified user \"%s\" owns chatroom \"%s\"",
          username,
          chatroomName
          ));

      return new Response(ResponseStatus.OK, "success");
    }
  }

  /**
   * Determines whether a username exists in the system
   *
   * @param username the username to check
   * @return true if user exists, false otherwise
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public boolean userExists(String username) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Verifying that user \"%s\" exists...",
        username
        ));

    // verify that the user exists by determining if their username is currently
    // tracked in the user map
    return userMap.containsKey(username);
  }

  /**
   * Determines whether a chatroom exists in the system
   *
   * @param chatroom the chatroom to check
   * @return true if the chatroom exists, false otherwise
   * @throws RemoteException if there is an error during remote communication
   */
  @Override
  public boolean chatroomExists(String chatroom) throws RemoteException {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Verifying that chatroom \"%s\" exists...",
        chatroom
        ));

    // verify the chatroom exists by checking that there is a key in the chatroom map corresponding
    // to the provided chatroom name
    return chatroomMap.containsKey(chatroom);
  }

  /**
   * Deletes a chatroom and associated entries and files from the local data server
   *
   * @param chatroomName name of the chatroom to delete
   */
  public void deleteChatroom(String chatroomName) {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Attempting to delete chatroom \"%s\"",
        chatroomName
        ));

    synchronized (chatroomMapLock) {
      // remove the chatroom from the chatroom map
      chatroomMap.remove(chatroomName);

      // access the chatrooms.txt file responsible for tracking existing chatrooms in the system
      String filename = dir.resolve("chatrooms.txt").toString();
      try {
        // Creates the file if it doesn't exist, if it does exist it will append to the file.
        FileWriter file = new FileWriter(filename, false);
        BufferedWriter writer = new BufferedWriter(file);
        // write the chatrooms that are still tracked by the data server into the chatrooms.txt file
        // write in format <chatroom name>:<username>
        for (String cName : chatroomMap.keySet()) {
          writer.write(ThreadSafeStringFormatter.format(
              "%s:%s",
              cName,
              chatroomMap.get(cName)
              ));
          writer.newLine();
        }
        writer.close();
      } catch (IOException e) {
        // if there is an error writing to the file, log the error and return
        CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "Something went very wrong writing to file %s",
            filename
            ));
        return;
      }
    }

    // if all operations succeed, log success
    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Successfully deleted chatroom \"%s\"",
        chatroomName
        ));
  }

  /**
   * Creates a user with the given name and password.
   *
   * @param username username of the created account
   * @param password password associated with the username
   */
  public void createUser(String username, String password) {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Creating user \"%s\"",
        username
        ));

    // if the provided username does not exist, place username and password pair into the
    // local usermap
    synchronized (userMapLock) {
      if (!userMap.containsKey(username)) {
        userMap.put(username, password);
      }  
    }

  }

  /**
   * Creates a chat room and associated files at the local data server
   *
   * @param chatroomName name of the chatroom to create
   * @param username username to be associated as owner and creator of the chatroom
   */
  public void createChatroom(String chatroomName, String username) {

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Attempting to create chatroom \"%s\" with owner \"%s\"",
        chatroomName,
        username
        ));

    synchronized (chatroomMapLock) {
      // if the provided chatroom is not being checked, add the chatroom name and the user that
      // created the chatroom to the local chatroom map
      if (!chatroomMap.containsKey(chatroomName)) {
        chatroomMap.put(chatroomName, username);
        //create a log file for the chatroom to track logged messages for the chatroom
        File chatLog = new File(dir.toString() + "/chatlogs/" + chatroomName + ".txt");
        try {
          if (chatLog.createNewFile()) {
            CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully created new chat log file for chatroom \"%s\"",
                chatroomName
                ));
          } else {
            CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
                "Unable to create new chat log file for chatroom \"%s\"",
                chatroomName
                ));
          }
        } catch (IOException e) {
          CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
              "There was an error when creating chat log file for chatroom \"%s\"",
              chatroomName
              ));
          return;
        }
      }
    }

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Successfully created chatroom \"%s\" with owner \"%s\"",
        chatroomName,
        username
        ));
  }
}

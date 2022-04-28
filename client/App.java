package client;

import data.ChatroomListResponse;
import data.ChatroomResponse;
import data.ICentralUserOperations;
import data.Response;
import data.ResponseStatus;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

/**
 * Initializes the client application
 */
public class App {

  public App(){}

  /**
   * Initiates the client using the provided ServerInfo object. Runs the client application.
   *
   * @param serverInfo provides server port and addressing information
   * @throws RemoteException  exceptions on invoking remote objects
   * @throws NotBoundException if there is an error locating RMI registries
   */
  public void go(ServerInfo serverInfo) throws RemoteException, NotBoundException {

    // used for debugging purposes
    if (serverInfo.getIsTest()) {
      Test t = new Test();
      t.go(serverInfo);
      return;
    }

    RMIAccess<ICentralUserOperations> centralServerAccessor = new RMIAccess<>(
        serverInfo.getCentralHost(),
        serverInfo.getCentralPort(),
        "ICentralUserOperations");

    // print welcome statement at begining of the program
    System.out.println("Welcome to the Chatroom App!!!\n");

    // tracks if the user wishes to continue interacting with the application
    boolean isActive = true;

    // indicates whether user is logged in or not
    boolean isLoggedIn = false;

    // holds username and password after user has been logged in to interact with the application
    String username = "";
    String password = "";

    // collects user input
    Scanner input = new Scanner(System.in);

    // continue prompting user for input as long as they are still interacting with the application
    while (isActive) {

      // if the user is not logged in, prompt them to either log in 
      //or create a new username and password
      if (!isLoggedIn) {
        System.out.println();
        System.out.println("Enter 1 to log in\nEnter 2 to create a "
            + "user profile\nEnter 'exit' to terminate program");
        System.out.println();
        System.out.print("Enter an option: ");

        String in = input.nextLine();
        System.out.println();

        // if option 1 is selected, log an existing user into the application
        if (in.compareTo("1") == 0) {

          System.out.print("Enter username: ");
          String loginUsername = input.nextLine();
          System.out.print("Enter password: ");
          String loginPassword = input.nextLine();
          System.out.println();

          Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Attempting to log into application with username \"%s\"",
              loginUsername
              ));

          // attempt to log in via the central server
          Response r = centralServerAccessor.getAccess().login(loginUsername, loginPassword);

          // if the login request fails, log the error and print the error to the user
          if (r.getStatus() == ResponseStatus.FAIL) {
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                "Login attempt for username \"%s\" failed",
                loginUsername
                ));
            System.out.println("Login failed");
          } else {
            // otherwise, log the success and print success to the screen
            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully logged in with username \"%s\"",
                loginUsername
                ));
            System.out.println("Success!");

            // set username and password to the username and password used to log in with the
            // central server
            username = loginUsername;
            password = loginPassword;
            // indicate user is logged in
            isLoggedIn = true;
          }

        } else if (in.compareTo("2") == 0) {
          // if option 2 is selected, attempt to create a new user
          System.out.print("Enter username: ");
          String createUsername = input.nextLine();
          System.out.print("Enter password: ");
          String createPassword = input.nextLine();
          System.out.println();

          Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Attempting to register user \"%s\"",
              createUsername
              ));

          // attempt to create new user via the central server
          Response r = centralServerAccessor.getAccess().registerUser(createUsername, 
              createPassword);

          // if the create user operation fails, log the error and print 
          //an error message to the client
          if (r.getStatus() == ResponseStatus.FAIL) {
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                "Failed to register user \"%s\": \"%s\"",
                createUsername,
                r.getMessage()
                ));
            System.out.println(String.format(
                "Create user failed: %s",
                r.getMessage()
                ));
          } else {
            // otherwise log success and print success to the user
            System.out.println("Success!");

            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully registered user \"%s\"",
                createUsername
                ));

            // set username and password to the username and password 
            //used to create the account with the
            // central server
            username = createUsername;
            password = createPassword;
            // indicate user is logged in
            isLoggedIn = true;
          }

        } else if (in.compareTo("exit") == 0) {
          // if the exit command is given, indicate user no longer wishes 
          //to interact with the application
          isActive = false;
        } else {
          // otherwise, indicate that the user has selected an invalid option
          System.out.println("Invalid option selected");
        }

      } else {
        // if the user is logged in, prompt the user for input to start interacting with chatrooms
        System.out.println();
        System.out.println("Enter 1 to join a chatroom\n"
            + "Enter 2 to get a list of available chatrooms\n"
            + "Enter 3 to create a chatroom\n"
            + "Enter 4 to delete a chatroom you own\n"
            + "Enter 5 to log out\n"
            + "Enter 'exit' to terminate program");
        System.out.println();
        System.out.print("Enter an option: ");

        String in = input.nextLine();
        System.out.println();

        // if option 1 is selected, prompt user for chatroom they want to join and attempt to join
        // chatroom
        if (in.compareTo("1") == 0) {

          // collect the name of the chatroom the user wants to join
          System.out.print("Enter the name of the chatroom to join: ");
          String chatroomName = input.nextLine();
          System.out.println();

          Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Attempting to join chatroom \"%s\"",
              chatroomName
              ));

          // attempt to get information regarding the requested chatroom so the user
          // can begin interacting with the chat server that supports the provided chatroom
          ChatroomResponse r = centralServerAccessor.getAccess().getChatroom(chatroomName);

          // if there is an error getting the chatroom, log the error and print
          // an error message to the client indicating what went wrong
          if (r.getStatus() == ResponseStatus.FAIL) {

            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                "User was unable to join chatroom \"%s\": \"%s\"",
                chatroomName,
                r.getMessage()
                ));

            System.out.println(String.format(
                "Join chatroom failed: \"%s\"",
                r.getMessage()
                ));
          } else {
            // otherwise, indicate chatroom has been located
            // initiate joining the chatroom
            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully found chatroom \"%s\"",
                chatroomName
                ));

            System.out.println("Joining chatroom...");
            // create an object that prevents the prompt from continuing to accept user input
            // until the client leaves the chatroom
            // prevents the user from joining multiple chatrooms at the same time
            Object chatWait = new Object();

            // initialize the JSwing chat window as well as the TCP 
            //connection and RMI interface used to interact
            // with the chatroom
            Chat chat = new Chat(username, chatroomName, 
                r.getAddress(), r.getTcpPort(), 
                r.getRegistryPort(), centralServerAccessor, chatWait);
            chat.start();
            // wait for the user to leave the chatroom before continuing to prompt user for input
            synchronized (chatWait) {
              try {
                chatWait.wait();
              } catch (InterruptedException e) {
                Logger.writeErrorToLog("Wait while chat window was active was interrupted");
              }
            }
          }
        } else if (in.compareTo("2") == 0) {
          // if option 2 is selected, gather a list of available chatrooms in the system and display
          // them to the user
          Logger.writeMessageToLog("Attempting to gather list of available chatrooms...");

          ChatroomListResponse r = centralServerAccessor.getAccess().listChatrooms();

          Logger.writeMessageToLog("Successfully gathered list of available chatrooms");

          System.out.println("Available chatrooms:");

          // print chatrooms to screen so user can use them to join one of the chatrooms
          for (String roomName : r.getChatroomNames()) {
            System.out.println(roomName);
          }

        } else if (in.compareTo("3") == 0) {
          // if option 3 is selected, attempt to create a new chatroom on behalf of the user
          // collect the name of the chatroom to be created
          System.out.print("Enter the name of the chatroom to create: ");
          String chatroomName = input.nextLine();
          System.out.println();

          Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Attempting to create new chatroom \"%s\"",
              chatroomName
              ));

          // attempt to create a new chatroom using the name provided by the user
          ChatroomResponse r = centralServerAccessor.getAccess().createChatroom(chatroomName,
              username);

          // if the create chatroom request fails, log the failure and print the error message
          // to the client
          if (r.getStatus() == ResponseStatus.FAIL) {

            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                "Creation of chatroom \"%s\" failed: \"%s\"",
                chatroomName,
                r.getMessage()
                ));

            System.out.println(String.format(
                "Create chatroom failed: %s",
                r.getMessage()
                ));
          } else {
            // otherwise, log the success and initialize the chat window 
            // to interact with the chatroom
            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully created new chatroom \"%s\"",
                chatroomName
                ));

            System.out.println("Joining new chatroom...");

            // create a wait object that prevents the command line from 
            //prompting the user for additional
            // input until the user leaves the chatroom
            Object chatWait = new Object();

            // initialize the chat window and TCP connection and RMI 
            //interface generation for the chat server
            // supporting the chatroom
            Chat chat = new Chat(username, chatroomName, r.getAddress(),
                r.getTcpPort(), r.getRegistryPort(), centralServerAccessor, chatWait);
            chat.start();

            // wait for the user to leave the chatroom before continuing 
            //to prompt the user for input
            synchronized (chatWait) {
              try {
                chatWait.wait();
              } catch (InterruptedException e) {
                Logger.writeErrorToLog("Wait while chat window was active was interrupted");
              }
            }
          }
        } else if (in.compareTo("4") == 0) {
          // if option 4 is selected, prompt user for the name of the chatroom they wish to
          // delete
          // collect the name of the chatroom to delete
          System.out.print("Enter the name of the chatroom to delete: ");
          String chatroomName = input.nextLine();
          System.out.println();

          Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Attempting to delete chatroom \"%s\"",
              chatroomName
              ));

          // attempt to delete the chatroom
          Response r = centralServerAccessor.getAccess().deleteChatroom(chatroomName, 
              username, password);

          // if the delete request failed, log failure and print fail message to the client
          if (r.getStatus() == ResponseStatus.FAIL) {

            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                "Failed to delete chatroom \"%s\": \"%s\"",
                chatroomName,
                r.getMessage()
                ));

            System.out.println(String.format(
                "Delete chatroom failed: %s",
                r.getMessage()
                ));
          } else {

            // otherwise, delete request succeeded, log success and print success message
            // to the client
            Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                "Successfully deleted chatroom \"%s\"",
                chatroomName
                ));

            System.out.println("Chatroom successfully deleted!");
          }
        } else if (in.compareTo("5") == 0) {
          // if option 5 is selected, log user out by setting isLoggedIn to false and forgetting the
          // username and password for the client
          Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
              "Logging out user \"%s\"",
              username
              ));

          isLoggedIn = false;
          username = "";
          password = "";

        } else if (in.compareTo("exit") == 0) {
          // if exist is selected, set isActive to false to terminate client application
          isActive = false;
        } else {
          // otherwise, invalid option selected, indicate to client they 
          //have selected invalid option
          System.out.println("Invalid option selected");
        }
      }
    }

    // print goodby message, clean up input resource and exit the application
    System.out.println("Goodbye!");

    input.close();
    System.exit(0);
  }

  /**
   * main method for the driver class which accepts the command line
   * arguments required to run the client
   *
   * @param args commandline arguments
   */
  public static void main(String[] args) {

    Logger.loggerSetup("Client");

    ServerInfo serverInfo = null;
    try {
      serverInfo = App.parseCommandLineArgs(args);
    } catch (IllegalArgumentException e) {
      Logger.writeErrorToLog(e.getMessage());
      return;
    }

    App app = new App();
    try {
      app.go(serverInfo);
    } catch (RemoteException | NotBoundException e) {
      Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
          "An error occurred while starting the client: \"%s\"",
          e.getMessage()
          ));
    }
  }



  /**
   * Parses command line arguments into address and port information for the central server
   *
   * @param args commandline arguments
   * @return ServerInfo object containing port and address information
   * @throws IllegalArgumentException in case of invalid arguments
   */

  public static ServerInfo parseCommandLineArgs(String[] args) 
      throws IllegalArgumentException {

    boolean isTest = false;

    // if -t is provided with required 2 args, run basic tests on 
    //operations to verify the application is performing
    // normal operation
    if (args.length == 3 && args[2].compareTo("-t") == 0) {
      isTest = true;
    } else if (args.length != 2) {
      // otherwise, require 2 arguments <central hostname> and <central port>
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Expected 2 arguments <central hostname> <central port>, received \"%d\" arguments",
          args.length
          ));
    }

    // parse port information

    int centralPort;
    try {
      centralPort = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Received illegal <central port> value, expected int, received \"%s\"",
          args[1]
          ));
    }

    return new ServerInfo(args[0], centralPort, isTest);

  }

}

package client;

import data.*;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import javax.swing.plaf.TableHeaderUI;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class App {

    public App(){}

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

        System.out.println("Welcome to the Chatroom App!!!\n");

        boolean isActive = true;
        boolean isLoggedIn = false;
        String username = "";
        String password = "";

        Scanner input = new Scanner(System.in);

        while(isActive) {
            if (!isLoggedIn) {
                System.out.println();
                System.out.println("Enter 1 to log in\nEnter 2 to create a user profile\nEnter 'exit' to terminate program");
                System.out.println();
                System.out.print("Enter an option: ");

                String in = input.nextLine();
                System.out.println();

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

                    Response r = centralServerAccessor.getAccess().login(loginUsername, loginPassword);

                    if (r.getStatus() == ResponseStatus.FAIL) {
                        Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                                "Login attempt for username \"%s\" failed",
                                loginUsername
                        ));
                        System.out.println(String.format(
                                "Login failed: %s",
                                r.getMessage()
                        ));
                    } else {
                        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                                "Successfully logged in with username \"%s\"",
                                loginUsername
                        ));
                        System.out.println("Success!");
                        username = loginUsername;
                        password = loginPassword;
                        isLoggedIn = true;
                    }

                } else if (in.compareTo("2") == 0) {

                    System.out.print("Enter username: ");
                    String createUsername = input.nextLine();
                    System.out.print("Enter password: ");
                    String createPassword = input.nextLine();
                    System.out.println();

                    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                            "Attempting to register user \"%s\"",
                            createUsername
                    ));

                    Response r = centralServerAccessor.getAccess().registerUser(createUsername, createPassword);

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
                        System.out.println("Success!");

                        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                                "Successfully registered user \"%s\"",
                                createUsername
                        ));

                        username = createUsername;
                        password = createPassword;
                        isLoggedIn = true;
                    }

                } else if (in.compareTo("exit") == 0) {
                    isActive = false;
                }
                else {
                    System.out.println("Invalid option selected");
                }

            } else {
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

                if (in.compareTo("1") == 0) {

                    System.out.print("Enter the name of the chatroom to join: ");
                    String chatroomName = input.nextLine();
                    System.out.println();

                    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                            "Attempting to join chatroom \"%s\"",
                            chatroomName
                    ));

                    ChatroomResponse r = centralServerAccessor.getAccess().getChatroom(chatroomName);

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

                        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                                "Successfully found chatroom \"%s\"",
                                chatroomName
                        ));

                        System.out.println("Joining chatroom...");
                        Object chatWait = new Object();
                        Chat chat = new Chat(username, chatroomName, r.getAddress(), r.getTcpPort(), r.getRegistryPort(), centralServerAccessor, chatWait);
                        chat.start();
                        synchronized (chatWait) {
                            try {
                                chatWait.wait();
                            } catch (InterruptedException e) {
                                Logger.writeErrorToLog("Wait while chat window was active was interrupted");
                            }
                        }
                    }
                }
                else if (in.compareTo("2") == 0) {

                    Logger.writeMessageToLog("Attempting to gather list of available chatrooms...");

                    ChatroomListResponse r = centralServerAccessor.getAccess().listChatrooms();

                    Logger.writeMessageToLog("Successfully gathered list of available chatrooms");

                    System.out.println("Available chatrooms:");

                    for (String roomName : r.getChatroomNames()) {
                        System.out.println(roomName);
                    }

                }
                else if (in.compareTo("3") == 0) {

                    System.out.print("Enter the name of the chatroom to create: ");
                    String chatroomName = input.nextLine();
                    System.out.println();

                    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                            "Attempting to create new chatroom \"%s\"",
                            chatroomName
                    ));

                    ChatroomResponse r = centralServerAccessor.getAccess().createChatroom(chatroomName, username);

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

                        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                                "Successfully created new chatroom \"%s\"",
                                chatroomName
                        ));

                        System.out.println("Joining new chatroom...");
                        Object chatWait = new Object();
                        Chat chat = new Chat(username, chatroomName, r.getAddress(), r.getTcpPort(), r.getRegistryPort(), centralServerAccessor, chatWait);
                        chat.start();
                        synchronized (chatWait) {
                            try {
                                chatWait.wait();
                            } catch (InterruptedException e) {
                                Logger.writeErrorToLog("Wait while chat window was active was interrupted");
                            }
                        }
                    }

                }
                else if (in.compareTo("4") == 0) {

                    System.out.print("Enter the name of the chatroom to delete: ");
                    String chatroomName = input.nextLine();
                    System.out.println();

                    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                            "Attempting to delete chatroom \"%s\"",
                            chatroomName
                    ));

                    Response r = centralServerAccessor.getAccess().deleteChatroom(chatroomName, username, password);

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

                        Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                                "Successfully deleted chatroom \"%s\"",
                                chatroomName
                        ));

                        System.out.println("Chatroom successfully deleted!");
                    }

                }
                else if (in.compareTo("5") == 0) {

                    Logger.writeMessageToLog(ThreadSafeStringFormatter.format(
                            "Logging out user \"%s\"",
                            username
                    ));

                    isLoggedIn = false;
                    username = "";
                    password = "";

                }
                else if (in.compareTo("exit") == 0) {
                    isActive = false;
                } else {
                    System.out.println("Invalid option selected");
                }
            }
        }

        System.out.println("Goodbye!");

        input.close();
        System.exit(0);
    }

    public static void main(String[] args) {

        Logger.loggerSetup("client");

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

    public static ServerInfo parseCommandLineArgs(String[] args) throws IllegalArgumentException {

        boolean isTest = false;

        if (args.length == 3 && args[2].compareTo("-t") == 0) {
            isTest = true;
        } else if (args.length != 2) {
            throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
                    "Expected 2 arguments <central hostname> <central port>, received \"%d\" arguments",
                    args.length
            ));
        }

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

package client;

import data.ICentralUserOperations;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class App {

    public App(){}

    public void go(ServerInfo serverInfo) throws RemoteException, NotBoundException {
        RMIAccess<ICentralUserOperations> centralServerAccessor = new RMIAccess<>(serverInfo.getCentralHost(),
                                                                                    serverInfo.getCentralPort(),
                                                                                    "ICentralUserOperations");

        // central server accessor can be used to retrieve all information about available chatrooms, servers
        // ports, etc
        centralServerAccessor.getAccess().login("sampleusername", "samplepassword");
    }

    public static void main(String[] args) {

        Logger.serverLoggerSetup("client");

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

        int centralPort;
        try {
            centralPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
                    "Received illegal <central port> value, expected int, received \"%s\"",
                    args[1]
            ));
        }

        return new ServerInfo(args[0], centralPort);

    }

}

package client;

import data.ICentralUserOperations;
import data.Response;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class App {

    public App(){}

    public void go(ServerInfo serverInfo) throws RemoteException, NotBoundException {

        // used for debugging purposes
        if (serverInfo.getIsTest()) {
            Test t = new Test();
            t.go(serverInfo);
            return;
        }

        RMIAccess<ICentralUserOperations> centralServerAccessor = new RMIAccess<>(serverInfo.getCentralHost(),
                serverInfo.getCentralPort(),
                "ICentralUserOperations");
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

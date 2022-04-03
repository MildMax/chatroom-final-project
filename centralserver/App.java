package centralserver;

import data.ICentralOperations;
import data.IChatroomOperations;
import data.IDataOperations;
import util.Logger;
import util.RMIAccess;
import util.ThreadSafeStringFormatter;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class App {

    private List<RMIAccess<IChatroomOperations>> chatroomNodes;
    private Object chatroomNodeLock;
    private List<RMIAccess<IDataOperations>> dataNodes;
    private Object dataNodeLock;

    public App() {
        this.chatroomNodeLock = new Object();
        this.dataNodeLock = new Object();
        this.chatroomNodes = Collections.synchronizedList(new ArrayList<>());
        this.dataNodes = Collections.synchronizedList(new ArrayList<>());
    }

    public void go(ServerInfo serverInfo) throws RemoteException {
        // start registry for Register function
        Registry centralOperationsRegistry = LocateRegistry.createRegistry(serverInfo.getRegisterPort());
        ICentralOperations centralOperationsEngine = new CentralOperations(this.chatroomNodes, this.chatroomNodeLock, this.dataNodes, this.dataNodeLock);
        centralOperationsRegistry.rebind("ICentralOperations", centralOperationsEngine);

        System.out.println("Central Server is ready");

    }

    public static void main(String[] args) {

        Logger.serverLoggerSetup("CentralServer");
        ServerInfo serverInfo = App.parseCommandLineArguments(args);

        App app = new App();
        try {
            app.go(serverInfo);
        } catch (RemoteException e) {
            Logger.writeErrorToLog(ThreadSafeStringFormatter.format(
                    "Unable to start Central Server; failed with error: \"%s\"",
                    e.getMessage()
            ));
        }

    }

    public static ServerInfo parseCommandLineArguments(String[] args) {

        int registerPort;
        try {
            registerPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
                    "Received illegal <register port> value, must be int, received \"%s\"",
                    args[0]
            ));
        }
        return new ServerInfo(registerPort);
    }

    static class ServerInfo {
        private final int registerPort;

        ServerInfo(int registerPort) {
            this.registerPort = registerPort;
        }

        int getRegisterPort() { return this.registerPort; }
    }


}

package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICentralOperations extends Remote {

    void register(String hostname, int port) throws RemoteException;

}

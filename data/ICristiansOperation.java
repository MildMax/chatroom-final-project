package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

// data node -> centralized server
// chat node -> centralized server

public interface ICristiansOperation extends Remote {

    long getServerTime() throws RemoteException;
}

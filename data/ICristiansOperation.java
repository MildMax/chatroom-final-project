package data;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICristiansOperation extends Remote {

    long getServerTime() throws RemoteException;
}

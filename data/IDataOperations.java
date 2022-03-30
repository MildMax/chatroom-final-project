package data;

import java.rmi.RemoteException;

public interface IDataOperations {
    Response login(String username, String password) throws RemoteException;
}

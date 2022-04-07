package dataserver;

import data.IDataOperations;
import data.Response;
import data.ResponseStatus;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class DataOperations extends UnicastRemoteObject implements IDataOperations {

    private final Map<String, String> userMap;
    private final Object userMapLock;

    public DataOperations(Map<String, String> userMap, Object userMapLock) throws RemoteException {
        this.userMap = userMap;
        this.userMapLock = userMapLock;
    }

    @Override
    public Response verifyUser(String username, String password) throws RemoteException {
        synchronized (userMapLock) {
            if (!userMap.containsKey(username)) {
                return new Response(ResponseStatus.FAIL, "User does not exist");
            }

            if (userMap.get(username).compareTo(password) != 0) {
                return new Response(ResponseStatus.FAIL, "User provided an invalid password");
            }

            return new Response(ResponseStatus.OK, "success");
        }
    }

	@Override
	public boolean userExists(String username) throws RemoteException {
		return userMap.containsKey(username);
	}

}

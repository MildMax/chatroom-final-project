package data;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * centralized initiation of server log.
 *
 */
public interface ICristiansOperation extends Remote {

  /**
   * the current server time.
   * @return the current server time.
   * @throws RemoteException  handles remote object invocations.
   */
  long getServerTime() throws RemoteException;
}

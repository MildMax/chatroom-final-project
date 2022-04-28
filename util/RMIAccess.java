package util;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Provides access to a remote RMI interface
 *
 * @param <K> type of interface at the remote RMI implementation
 */
public class RMIAccess<K> implements Serializable {

  private String interfaceName;
  private String hostname;
  private int port;

  /**
   * Creates an instance of the RMI accessor
   *
   * @param hostname hostname of the machine supporting the RMI interface
   * @param port the port the RMI interface is accepting requests on
   * @param interfaceName name of the interface at the remote RMI implementation
   */
  public RMIAccess(String hostname, int port, String interfaceName) {
    this.hostname = hostname;
    this.port = port;
    this.interfaceName = interfaceName;
  }

  /**
   * Gets the accessor for the remote RMI interface
   *
   * @return the accessor for the remote RMI interface
   * @throws RemoteException if there is an error contacting the remote RMI interface
   * @throws NotBoundException if the remote RMI interface cannot be located
   */
  public K getAccess() throws RemoteException, NotBoundException {
    K access;
    try {
      Registry registry = 
          LocateRegistry.getRegistry(InetAddress.getByName(hostname).getHostAddress(), port);
      access = (K) registry.lookup(interfaceName);
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException(ThreadSafeStringFormatter.format(
          "Unable to resolve host at \"%s\"",
          this.hostname
          ));
    } catch (RemoteException e) {
      throw new RemoteException(ThreadSafeStringFormatter.format(
          "Error occurred during remote communication: %s",
          e.getMessage()
          ));
    } catch (NotBoundException e) {
      throw new NotBoundException(ThreadSafeStringFormatter.format(
          "Error occurred when looking up registry for \"%s\" at \"%s:%d\": %s",
          this.interfaceName,
          this.hostname,
          this.port,
          e.getMessage()
          ));
    }

    return access;
  }

  /**
   * The host address that supports the remote RMI interface
   *
   * @return the host address that supports the remote RMI interface
   */
  public String getHostname() {
    return this.hostname;
  }

  /**
   * Returns the port the remote RMI interface is accepting connections on
   *
   * @return the port the remote RMI interface is accepting connections on
   */
  public int getPort() {
    return this.port;
  }

}

package util;

import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;

/**
 * Describes operations to determine identity of Clients.
 */
public class ClientIPUtil {

  /**
   * Returns the Client's IP address as a String from the current Server RMI thread.
   *
   * @return the Client's IP address
   */
  public static synchronized String getClientIP() {
    String client = null;
    try {
      client = RemoteServer.getClientHost();

    } catch (ServerNotActiveException e) {
      Logger.writeErrorToLog("RMI Server was not active at time of client request");
    }
    return client;
  }

}

package data;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


/**
 * Initiates response objects for both chat node and data node.
 *
 */
public interface ICentralOperations extends Remote {

  /**
   * registered data nodes.
   * @param hostname name of host
   * @param dataOperationsPort port number
   * @param dataParticipantPort port number
   * @param chatrooms list of chat rooms
   * @return registered datanodes
   * @throws RemoteException handles remote object invocations.
   */
  
  RegisterResponse registerDataNode(String hostname, 
      int dataOperationsPort, int dataParticipantPort, 
      List<String> chatrooms) throws RemoteException;
  /**
   * registered chat nodes declaration.
   * @param hostname name of the host
   * @param port port number 
   * @return registered chatnode.
   * @throws RemoteException handles remote object invocations.
   */
  
  RegisterResponse registerChatNode(String hostname, 
      int port) throws RemoteException;
  
  /**
   * server time for the loggers.
   * @return the server time.
   * @throws RemoteException handles remote object invocations.
   */
  long getServerTime() throws RemoteException;

}

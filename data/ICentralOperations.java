package data;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


/**
 * Defines operations that should be supported by the central server to allow
 * the registration of data and server nodes as well as the master time
 * for the central server in Cristian's algorithm
 */
public interface ICentralOperations extends Remote {

  /**
   * Registers a data node with the central server application. Spins up existing chatrooms tracked
   * by the data node prior to the application previously shutting down.
   *
   * @param hostname hostname for the machine supporting the data server
   * @param dataOperationsPort port where data node is accepting operations requests
   * @param dataParticipantPort port where data node is accepting participant requests in 2 phase commmit
   * @param chatrooms list of chat rooms previously recorded in the data server
   * @return a response containing the central server Coordinator port for 2 phase commit
   * @throws RemoteException if there is an error during remote communication
   */
  RegisterResponse registerDataNode(String hostname, 
      int dataOperationsPort, int dataParticipantPort, 
      List<String> chatrooms) throws RemoteException;

  /**
   * Registers a chat server node with the central server application
   *
   * @param hostname hostname of the machine supporting the chat server
   * @param port port where chat server is accepting operations requests from central server
   * @return a response containing port for chatroom operations registry at the central server
   * @throws RemoteException
   */
  RegisterResponse registerChatNode(String hostname, 
      int port) throws RemoteException;

  /**
   * Gets the master server time for Cristian's Algorithm
   *
   * @return the master server time in milliseconds
   * @throws RemoteException if there is an error during remote communication
   */
  long getServerTime() throws RemoteException;

}

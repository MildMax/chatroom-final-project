package util;

import data.ICentralOperations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * CristiansLogger is the class which implements runnable and uses 
 * Cristian's algorithm  to maintain a synchronized clock at the local logger through out the
 * lifetime of the server
 *
 */
public class CristiansLogger implements Runnable {

  private static RMIAccess<ICentralOperations> accessor;
  private static long diff = 0;
  private static final Object diffLock = new Object();

  private static BufferedWriter logWriter;

  /**
   * Initializes the ServerLogger with the respective id use to generate a unique log file for the server
   *
   * @param id id of the server
   */
  public static void loggerSetup(String id) {
    File logFile = new File(String.format("./%sLog.txt", id));
    // check if the logfile already exists; if not, create one
    if (!logFile.exists()) {
      try {
        logFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    try {
      CristiansLogger.logWriter = new BufferedWriter(new FileWriter(logFile, true));
    } catch (IOException e) {
      // if error generating log write stream, print error to stdout
      System.out.println(String.format("There was an error "
          + "initializing log file: %s", e.getMessage()));
    }
  }

  /**
   * Sets the RMI accessor for the central server used to retrieve the master time for the system
   *
   * @param accessor the RMI accessor for the central server
   */
  public static void setCentralAccessor(RMIAccess<ICentralOperations> accessor) {
    CristiansLogger.accessor = accessor;
  }

  /**
   * Writes a standard message to the server log
   *
   * @param log message to be written to server log
   */
  public static synchronized void writeMessageToLog(String log) {
    String dateWithMilli = CristiansLogger.getFormattedTimeInMilli();
    try {
      CristiansLogger.logWriter.write(String.format("%s: %s\n", dateWithMilli, log));
      CristiansLogger.logWriter.flush();
    } catch (IOException e) {
      // if error writing to log, print error to stdout
      System.out.println(ThreadSafeStringFormatter.format(
          "There was an error writing to the log: %s", e.getMessage()));
    } catch (NullPointerException e) {
      System.out.println(ThreadSafeStringFormatter.format(
          "ERROR CristiansLogger class was not initialized: attempting to write: %s",
          log
          ));
    }
  }

  /**
   * Writes an error message to the server log.
   *
   * @param log message to be written to server log
   */
  public static synchronized void writeErrorToLog(String log) {
    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format("ERROR %s", log));
  }

  /**
   * Runs Cristian's algorithm using the master time provided by the central server
   *
   * @throws RemoteException if there is an error during remote communication
   * @throws NotBoundException if the central RMI registry cannot be located
   */
  public static void cristiansAlgorithm() throws RemoteException, NotBoundException {

    CristiansLogger.writeMessageToLog("Running Cristian's algorithm...");

    // get the RTT
    long start = System.currentTimeMillis();
    long serverTime = accessor.getAccess().getServerTime();
    long end = System.currentTimeMillis();

    // hold the system's time in milliseconds
    long localClock;
    // holds the calcualted difference between server time + (RTT/2)
    long calculatedTime;

    // surround in lock so that this process is not licked out while 
    //attempting to calculate the local time
    synchronized (diffLock) {
      // divide the difference between end and start by 2
      // uses formula Tlocal = Tserver + (RTT / 2)
      calculatedTime = serverTime + ((end - start) / 2);
      localClock = System.currentTimeMillis();

      // then calculate the diff between the calculated local time and the system time
      diff = calculatedTime - localClock;
    }

    CristiansLogger.writeMessageToLog(ThreadSafeStringFormatter.format(
        "Calculated a difference of (%d milliseconds) between server "
        + "clock + (RTT/2) (%d milliseconds) and local clock "
        + "(%d milliseconds) with an RTT of (%d milliseconds)",
        diff,
        calculatedTime,
        localClock,
        end - start
        ));
  }

  /**
   * Formats the current time in Year-Month-Day Hour-Minute-Second.Millisecond format
   *
   * @return current time with millisecond precision
   */
  protected static String getFormattedTimeInMilli() {
    long millitime;

    // modify the time used to log a message by the difference between 
    //the local clock and the server clock
    synchronized (diffLock) {
      millitime = System.currentTimeMillis() + diff;
    }
    Date date = new Date(millitime);
    // create expression to define format of current time with millisecond precision
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    // format and return date
    return sdf.format(date);
  }

  /**
   * Runs Cristian's Algorithm every 10 seconds to continuously maintain a synchronized local clock
   */
  @Override
  public void run() {
    // run Cristians for the duration of program
    // run immediately and then wait
    while (true) {
      try {
        CristiansLogger.cristiansAlgorithm();
      } catch (RemoteException | NotBoundException e) {
        CristiansLogger.writeErrorToLog(ThreadSafeStringFormatter.format(
            "There was an error contact the Central Server for Cristian's Algorithm: \"%s\"",
            e.getMessage()
            ));
      }

      // run every 10 seconds
      try {
        Thread.sleep(10000);
      } catch (InterruptedException e) {
        CristiansLogger.writeErrorToLog("Wait on Cristian's algorithm thread was interrupted");
      }
    }
  }
}

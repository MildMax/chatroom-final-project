package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Manages writing events to server logs so as to maintain fault tolerance and
 * these loggers play a very prominent role in identifying the faults.
 */
public abstract class Logger {

  private static BufferedWriter logWriter;

  /**
   * Initializes the ServerLogger.
   * @param id unique logger id.
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
      Logger.logWriter = new BufferedWriter(new FileWriter(logFile, true));
    } catch (IOException e) {
      // if error generating log write stream, print error to stdout
      System.out.println(String.format("There was an error "
          + "initializing log file: %s", e.getMessage()));
    }
  }

  /**
   * Writes a standard message to the server log.
   *
   * @param log message to be written to server log
   */
  public static synchronized void writeMessageToLog(String log) {
    String dateWithMilli = Logger.getFormattedTimeInMilli();
    try {
      Logger.logWriter.write(String.format("%s: %s\n", dateWithMilli, log));
      Logger.logWriter.flush();
    } catch (IOException e) {
      // if error writing to log, print error to stdout
      System.out.println(ThreadSafeStringFormatter.format("There was an "
          + "error writing to the log: %s", e.getMessage()));
    } catch (NullPointerException e) {
      System.out.println(ThreadSafeStringFormatter.format(
          "ERROR Logger class was not initialized: attempting to write: %s",
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
    Logger.writeMessageToLog(ThreadSafeStringFormatter.format("ERROR %s", log));
  }

  /**
   * Formats the current time in Year-Month-Day Hour-Minute-Second.Millisecond format
   *
   * @return current time with millisecond precision
   */
  protected static String getFormattedTimeInMilli() {
    long milliTime = System.currentTimeMillis();
    Date date = new Date(milliTime);
    // create expression to define format of current time with millisecond precision
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    // format and return date
    return sdf.format(date);
  }

}
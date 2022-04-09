package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Manages writing events to server logs
 */
public class Logger {

    private static BufferedWriter logWriter;

    /**
     * Initializes the ServerLogger
     */
    public static void serverLoggerSetup(String id) {
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
            System.out.println(String.format("There was an error initializing Server log file: %s", e.getMessage()));
        }
    }

    /**
     * Writes a standard message to the server log
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
            System.out.println(ThreadSafeStringFormatter.format("There was an error writing to the Server log: %s", e.getMessage()));
        } catch (NullPointerException e) {
            System.out.println(ThreadSafeStringFormatter.format(
                    "ERROR ServerLogger class was not initialized: attempting to write: %s",
                    log
            ));
        }
    }

    /**
     * Writes an error message to the server log
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
    private static String getFormattedTimeInMilli() {
        long milliTime = System.currentTimeMillis();
        Date date = new Date(milliTime);
        // create expression to define format of current time with millisecond precision
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        // format and return date
        return sdf.format(date);
    }
    
   
    /**
     * takes in current local time and server time after the operation
     * subracts the two time stammps and returns the round trip time 
     * @return
     */
    private static String rtt() {
      long localTime = System.currentTimeMillis();
      Date date = new Date(localTime);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      long serverTime = getFormattedTimeInMilli();
      long rtt = serverTime - sdf.format(date);
      
      return rtt;
    }
}
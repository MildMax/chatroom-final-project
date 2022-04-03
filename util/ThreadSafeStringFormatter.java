package util;

/**
 * Provides thread-safe operations on static String operations
 */
public class ThreadSafeStringFormatter {

    /**
     * Wraps String.format in a thread-safe call
     *
     * @param formatString format string
     * @param args objects to format into string
     * @return foramtted string with given arguments
     */
    public static synchronized String format(String formatString, Object ...args) {
        return String.format(
                formatString,
                args
        );
    }
}

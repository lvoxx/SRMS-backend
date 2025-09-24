package io.github.lvoxx.srms.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for printing logs in a standardized format across different
 * modules.
 */
public class LogPrinter {
    private static final Logger logger = LoggerFactory.getLogger(LogPrinter.class);
    private static final String PREFIX = "[%s] ";
    private static final String TIME_FORMAT = "at %s - ";

    /**
     * Constants representing different log flags used in categorizing logs.
     */
    public static abstract class Flag {
        public static final String CONTROLLER_FLAG = "CONTROLLER";
        public static final String SERVICE_FLAG = "SERVICE";
        public static final String REPOSITORY_FLAG = "REPOSITORY";
        public static final String ASPECT_FLAG = "ASPECT";
        public static final String UTILS_FLAG = "UTILS";
        public static final String SCHEDULER_FLAG = "SCHEDULER";
        public static final String START_UP = "START-UP";
    }

    /**
     * Log message templates for different modules.
     */
    private static abstract class Log {
        private static final String CONTROLLER_LOG = "Handling request to path %s via %s.%s(). %s";
        private static final String SERVICE_LOG = "Executing service %s.%s(). %s";
        private static final String REPOSITORY_LOG = "Accessing repository %s.%s(). %s";
        private static final String ASPECT_LOG = "Triggering aspect %s.%s(). %s";
        private static final String UTILS_LOG = "Utilizing utility %s.%s(). %s";
        private static final String SCHEDULER_LOG = "Utilizing scheduler %s.%s(). %s";
    }

    /**
     * Enum representing different log types.
     */
    public static enum Type {
        INFO, DEBUG, WARM, ERROR, SCHEDULER
    }

    /**
     * Prints a log message for controller operations.
     * 
     * @param type         The type of log (INFO, DEBUG, etc.).
     * @param requestPath  The request path.
     * @param className    The name of the class.
     * @param methodName   The method being invoked.
     * @param extraMessage Additional information to log.
     */
    public static void printControllerLog(Type type, String requestPath, String className, String methodName,
            String extraMessage) {
        requestPath = Optional.ofNullable(requestPath).orElseGet(() -> "No controller path found.");
        printLog(type, Flag.CONTROLLER_FLAG,
                String.format(Log.CONTROLLER_LOG,
                        requestPath, className,
                        methodName,
                        extraMessage));
    }

    /**
     * Prints a log message for service operations.
     */
    public static void printServiceLog(Type type, String className, String methodName, String extraMessage) {
        printLog(type, Flag.SERVICE_FLAG,
                String.format(Log.SERVICE_LOG, className, methodName, extraMessage));
    }

    /**
     * Prints a log message for repository operations.
     */
    public static void printRepositoryLog(Type type, String className, String methodName, String extraMessage) {
        printLog(type, Flag.REPOSITORY_FLAG,
                String.format(Log.REPOSITORY_LOG, className, methodName, extraMessage));
    }

    /**
     * Prints a log message for aspect operations.
     */
    public static void printAspectLog(Type type, String className, String methodName, String extraMessage) {
        printLog(type, Flag.ASPECT_FLAG,
                String.format(Log.ASPECT_LOG, className, methodName, extraMessage));
    }

    /**
     * Prints a log message for utility operations.
     */
    public static void printUtilsLog(Type type, String className, String methodName, String extraMessage) {
        printLog(type, Flag.UTILS_FLAG,
                String.format(Log.UTILS_LOG, className, methodName, extraMessage));
    }

    /**
     * Prints a log message for scheduler operations.
     */
    public static void printSchedulerLog(Type type, String className, String methodName, String extraMessage) {
        printLog(type, Flag.SCHEDULER_FLAG,
                String.format(Log.SCHEDULER_LOG, className, methodName, extraMessage));
    }

    /**
     * Prints a log message with a specified type, flag, and message.
     */
    public static void printLog(Type type, String flag, String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String timeFormat = String.format(TIME_FORMAT, time);
        flag = Optional.ofNullable(flag).orElse(Flag.UTILS_FLAG);
        type = Optional.ofNullable(type).orElse(LogPrinter.Type.INFO);

        // [flag] at time - message
        String logMessage = new StringBuilder()
                .append(String.format(PREFIX, flag.toUpperCase()))
                .append(timeFormat)
                .append(message)
                .toString();
        extracteLogType(type, logMessage);
    }

    public static void prettyPrintObjectArray(Type type, Object[] objects) {
        final Type finalType = Optional.ofNullable(type).orElse(LogPrinter.Type.INFO);

        extracteLogType(finalType, "---- Printing Object[] fields ----");
        IntStream.range(0, objects.length)
                .forEach(i -> extracteLogType(finalType, "Field[" + i + "] = " + objects[i]));
        extracteLogType(finalType, "----------------------------------");
    }

    private static void extracteLogType(Type type, String logMessage) {
        switch (type) {
            case LogPrinter.Type.ERROR:
                logger.error(logMessage);
                return;
            case LogPrinter.Type.DEBUG:
                logger.debug(logMessage);
                return;
            case LogPrinter.Type.WARM:
                logger.warn(logMessage);
                return;
            case LogPrinter.Type.INFO:
            case LogPrinter.Type.SCHEDULER:
            default:
                logger.info(logMessage);
        }
    }
}
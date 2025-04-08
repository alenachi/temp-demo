# a sample


```java

import org.slf4j.LoggerFactory;

/**
 * 汎用ログ出力クラス
 * 
 */
public class Logger {

    private static org.slf4j.Logger getLog() {
        return LoggerFactory.getLogger("my logger name");//from logback.xml
    }

    public static void info(String logMessage) {
        getLog().info(logMessage);
    }

    public static void info(int logMessage) {
        info(String.valueOf(logMessage));
    }

    public static void debug(String logMessage) {
        getLog().debug(logMessage);
    }

    public static void debug(int logMessage) {
        debug(String.valueOf(logMessage));
    }

    private static org.slf4j.Logger getLogger(Object caller) {
        if (caller instanceof Class) {
            return LoggerFactory.getLogger((Class<?>) caller);
        } else {
            return LoggerFactory.getLogger(caller.getClass());
        }
    }

    public static void info(Object caller, Object logMessage) {
        org.slf4j.Logger logger = getLogger(caller);
        if (logMessage instanceof Throwable) {
            Throwable t = (Throwable) logMessage;
            logger.info(t.getMessage(), t);
            return;
        }
        logger.info(String.valueOf(logMessage));
    }

    public static void info(Object caller, String message, Throwable e) {
        org.slf4j.Logger logger = getLogger(caller);
        logger.info(message, e);
    }

    public static void error(String logMessage) {
        getLog().error(logMessage);
    }

    public static void error(int logMessage) {
        error(String.valueOf(logMessage));
    }

    public static void error(Object caller, Object logMessage) {
        org.slf4j.Logger logger = getLogger(caller);
        if (logMessage instanceof Throwable) {
            Throwable t = (Throwable) logMessage;
            logger.error(t.getMessage(), t);
            return;
        }
        logger.error(String.valueOf(logMessage));
    }

    public static void error(Object caller, String message, Throwable e) {
    	org.slf4j.Logger logger = getLogger(caller);
    	logger.error(message, e);
    }

    public static void warn(String logMessage) {
        getLog().warn(logMessage);
    }

    public static void warn(int logMessage) {
        warn(String.valueOf(logMessage));
    }

    public static void warn(Object caller, Object logMessage) {
        org.slf4j.Logger logger = getLogger(caller);
        if (logMessage instanceof Throwable) {
            Throwable t = (Throwable) logMessage;
            logger.warn(t.getMessage(), t);
            return;
        }
        logger.warn(String.valueOf(logMessage));
    }

    public static void warn(Object caller, String message, Throwable e) {
    	org.slf4j.Logger logger = getLogger(caller);
    	logger.warn(message, e);
    }

    public static void debug(Object caller, Object logMessage) {
        org.slf4j.Logger logger = getLogger(caller);
        if (logMessage instanceof Throwable) {
            Throwable t = (Throwable) logMessage;
            logger.debug(t.getMessage(), t);
            return;
        }
        logger.debug(String.valueOf(logMessage));
    }


    /**
     * debug出力が使用可能かどうか.
     * @param caller 呼び出し元
     * @return true: 使用可能, false: 使用不可能
     */
    public static boolean isDebugEnabled(Object caller) {
        org.slf4j.Logger logger = getLogger(caller);
        return logger.isDebugEnabled();
    }
}
```
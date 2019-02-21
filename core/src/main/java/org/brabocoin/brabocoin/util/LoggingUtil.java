package org.brabocoin.brabocoin.util;

import javafx.application.Platform;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingUtil {
    private static final Logger LOGGER = Logger.getLogger(LoggingUtil.class.getName());

    public static Level setLogLevel(String logLevel) {
        if (logLevel != null) {
            Logger rootLogger = Logger.getLogger("org.brabocoin.brabocoin");
            ConsoleHandler consoleHandler = new ConsoleHandler();
            rootLogger.addHandler(
                consoleHandler
            );
            switch (logLevel.toLowerCase()) {
                case "all":
                    rootLogger.setLevel(Level.ALL);
                    break;
                case "finest":
                    rootLogger.setLevel(Level.FINEST);
                    break;
                case "finer":
                    rootLogger.setLevel(Level.FINER);
                    break;
                case "fine":
                    rootLogger.setLevel(Level.FINE);
                    break;
                case "warning":
                    rootLogger.setLevel(Level.WARNING);
                    break;
                default:
                    LOGGER.severe("Could not parse config level");
                    Platform.exit();
            }
            consoleHandler.setLevel(rootLogger.getLevel());
            return rootLogger.getLevel();
        }
        return null;
    }
}

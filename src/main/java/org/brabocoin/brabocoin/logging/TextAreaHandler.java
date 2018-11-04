package org.brabocoin.brabocoin.logging;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Logging handler for writing to a JavaFX {@link TextArea}.
 */
public class TextAreaHandler extends Handler {

    private final @NotNull TextArea textArea;

    /**
     * Create a new text area handler for the provided text area.
     *
     * @param textArea
     *     The text area to log to.
     */
    public TextAreaHandler(@NotNull TextArea textArea) {
        this(textArea, new SimpleFormatter());
    }

    /**
     * Create a new text area handler for the provided text area and formatter.
     *
     * @param textArea
     *     The text area to log to.
     * @param formatter
     *     The formatter to use.
     */
    public TextAreaHandler(@NotNull TextArea textArea, @NotNull Formatter formatter) {
        this.textArea = textArea;
        setFormatter(formatter);
    }

    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record)) {
            Platform.runLater(() -> textArea.appendText(getFormatter().format(record)));
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}

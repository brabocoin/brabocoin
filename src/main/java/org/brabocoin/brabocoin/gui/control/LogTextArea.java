package org.brabocoin.brabocoin.gui.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;


/**
 * A modified text area control that is suitable for displaying (console) log output.
 * <p>
 * Provides the capability to enable/disable auto-scroll to end when a new log message is added.
 * <p>
 * By default, a {@link TextArea} does auto-scroll to the end, but it is not possible to disable
 * this behavior.
 */
public class LogTextArea extends TextArea {

    /**
     * Indicates whether to auto-scroll to end when a new log message is added.
     */
    private final BooleanProperty autoScrollToEnd = new SimpleBooleanProperty(true);

    /**
     * Create a new log text area.
     */
    public LogTextArea() {
        super();

        // Scroll to bottom when auto scroll is enabled
        autoScrollToEnd.addListener((obs, old, autoScroll) -> {
            if (autoScroll) {
                this.appendText("");
            }
        });

        // If scrolled up, set auto scroll to false
        scrollTopProperty().addListener((obs, oldValue, newValue) -> {
            // Make sure that at least one pixel scrolled up (double arithmetic inaccuracies)
            if (oldValue.doubleValue() - newValue.doubleValue() > 1) {
                setAutoScrollToEnd(false);
            }
        });
    }

    @Override
    public void replaceText(int start, int end, String text) {
        if (isAutoScrollToEnd()) {
            super.replaceText(start, end, text);
        }
        else {
            // Remember caret position and selection
            IndexRange range = this.getSelection();
            int position = this.getCaretPosition();

            super.replaceText(start, end, text);

            this.positionCaret(position);
            this.selectRange(range.getStart(), range.getEnd());
        }
    }

    /**
     * Indicates whether to auto-scroll to end when a new log message is added.
     *
     * @return The auto scroll property.
     */
    public BooleanProperty autoScrollToEndProperty() {
        return autoScrollToEnd;
    }

    /**
     * Indicates whether to auto-scroll to end when a new log message is added.
     *
     * @return Whether auto-scroll is enabled.
     */
    public boolean isAutoScrollToEnd() {
        return autoScrollToEnd.get();
    }

    public void setAutoScrollToEnd(boolean autoScrollToEnd) {
        this.autoScrollToEnd.set(autoScrollToEnd);
    }
}

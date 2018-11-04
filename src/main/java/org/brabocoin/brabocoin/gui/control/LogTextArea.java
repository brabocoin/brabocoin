package org.brabocoin.brabocoin.gui.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;


/**
 * @author Sten Wessel
 */
public class LogTextArea extends TextArea {

    private final BooleanProperty autoScrollToEnd = new SimpleBooleanProperty(true);

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
            IndexRange range = this.getSelection();
            int position = this.getCaretPosition();
            super.replaceText(start, end, text);
            this.positionCaret(position);
            this.selectRange(range.getStart(), range.getEnd());
        }
    }

    public BooleanProperty autoScrollToEndProperty() {
        return autoScrollToEnd;
    }

    public boolean isAutoScrollToEnd() {
        return autoScrollToEnd.get();
    }

    public void setAutoScrollToEnd(boolean autoScrollToEnd) {
        this.autoScrollToEnd.set(autoScrollToEnd);
    }
}

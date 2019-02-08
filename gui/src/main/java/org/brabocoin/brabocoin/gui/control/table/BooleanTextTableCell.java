package org.brabocoin.brabocoin.gui.control.table;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

public class BooleanTextTableCell<S> extends TableCell<S, Boolean> {
    private final String trueText;
    private final String falseText;

    public BooleanTextTableCell(String trueText, String falseText) {
        this.trueText = trueText;
        this.falseText = falseText;

        this.setAlignment(Pos.CENTER);
    }

    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        setText(item ? trueText : falseText);
    }
}

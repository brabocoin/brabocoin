package org.brabocoin.brabocoin.gui.control.table;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import org.brabocoin.brabocoin.gui.util.GUIUtils;

public class BalanceTableCell<T> extends TableCell<T, Long> {

    public BalanceTableCell() {
        setAlignment(Pos.CENTER_RIGHT);
    }

    @Override
    protected void updateItem(Long item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else {
            setText(GUIUtils.formatValue(item, false));
        }
    }
}

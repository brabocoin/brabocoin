package org.brabocoin.brabocoin.gui.control.table;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import org.brabocoin.brabocoin.gui.util.GUIUtils;
import org.brabocoin.brabocoin.gui.util.WalletUtils;

public class ColoredBalanceTableCell<T> extends TableCell<T, Long> {

    public ColoredBalanceTableCell() {
        setAlignment(Pos.CENTER_RIGHT);
    }

    @Override
    protected void updateItem(Long item, boolean empty) {
        setAlignment(Pos.CENTER_RIGHT);
        if (empty) {
            setText(null);
        }
        else {
            setText((item > 0 ? "+" : "") +
                GUIUtils.formatValue(item, false));
            setStyle(WalletUtils.getPendingStyle(item));
        }
    }
}

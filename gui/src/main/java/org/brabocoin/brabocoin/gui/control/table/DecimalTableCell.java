package org.brabocoin.brabocoin.gui.control.table;

import javafx.scene.control.TableCell;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

/**
 * Table cell that formats a decimal number.
 */
public class DecimalTableCell <T, N extends Number> extends TableCell<T, N> {

    private @NotNull final DecimalFormat format;

    public DecimalTableCell(@NotNull DecimalFormat format) {
        super();
        this.format = format;
    }

    @Override
    protected void updateItem(N item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else {
            setText(format.format(item));
        }
    }
}

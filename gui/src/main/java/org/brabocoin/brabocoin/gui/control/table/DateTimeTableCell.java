package org.brabocoin.brabocoin.gui.control.table;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Table cell that formats a timestamp.
 */
public class DateTimeTableCell <T> extends CopyableTableCell<T, LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss");

    @Override
    protected void updateItem(LocalDateTime item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else {
            setText(item.format(FORMATTER));
        }
    }
}

package org.brabocoin.brabocoin.gui.control.table;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;


/**
 * Table cell where the contents are copyable.
 */
public class CopyableTableCell<S, T> extends TableCell<S, T> {

    public CopyableTableCell() {
        super();
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(getText());
            clipboard.setContent(content);
        });
        ContextMenu menu = new ContextMenu(copyItem);
        setContextMenu(menu);
    }
}

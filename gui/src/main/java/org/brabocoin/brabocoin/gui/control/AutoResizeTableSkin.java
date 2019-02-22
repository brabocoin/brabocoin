package org.brabocoin.brabocoin.gui.control;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.TableView;
import org.brabocoin.brabocoin.gui.util.GUIUtils;

/**
 * Table view skin automatically resizing columns on item update.
 */
public class AutoResizeTableSkin<T> extends TableViewSkin<T> {

    private TableView<T> tableView;

    public AutoResizeTableSkin(TableView<T> tableView) {
        super(tableView);
        this.tableView = tableView;
    }

    @Override
    protected void updateRowCount() {
        super.updateRowCount();
        GUIUtils.autoFitTable(tableView);
    }
}

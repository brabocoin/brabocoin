package org.brabocoin.brabocoin.gui.control;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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
        for (TableColumn<T, ?> col : tableView.getColumns()) {
            this.resizeColumnToFitContent(col, 1);
        }
    }
}

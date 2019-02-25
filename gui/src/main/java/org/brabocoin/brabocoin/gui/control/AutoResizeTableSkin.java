package org.brabocoin.brabocoin.gui.control;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.scene.control.IndexedCell;
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

        VirtualFlow<?> virtualFlow = (VirtualFlow<?>)getChildren().get(1);
        IndexedCell lastVisibleCell = virtualFlow.getLastVisibleCell();
        int count = lastVisibleCell != null ? (lastVisibleCell.getIndex() + 1) : 1;

        for (TableColumn<T, ?> col : tableView.getColumns()) {
            if (this.getTableHeaderRow().getColumnHeaderFor(col) != null) {
                this.resizeColumnToFitContent(col, Math.max(1, count));
            }
        }
    }
}

package org.brabocoin.brabocoin.gui.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.controlsfx.control.MasterDetailPane;

public class CollapsibleMasterDetailPane extends MasterDetailPane {

    private Object previouslySelectedItem;
    private ObjectProperty<TableRow> lastSelectedRow = new SimpleObjectProperty<>();

    public void registerTableView(TableView tableView) {
        tableView.setOnMouseClicked(event -> {
            Object selectedItem = tableView.getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                return;
            }

            // Check if mouse click on actual item
            if (lastSelectedRow.get() != null && !lastSelectedRow.get()
                .localToScene(lastSelectedRow.get().getBoundsInLocal())
                .contains(
                    event.getSceneX(), event.getSceneY()
                )) {
                return;
            }

            if (previouslySelectedItem == selectedItem) {
                this.setShowDetailNode(!this.isShowDetailNode());
            }

            previouslySelectedItem = selectedItem;
        });

        tableView.setRowFactory(table -> {
            TableRow row = new TableRow();
            row.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                if (isNowSelected) {
                    lastSelectedRow.set(row);
                }
            });
            return row;
        });
    }
}

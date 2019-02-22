package org.brabocoin.brabocoin.gui.control;

import javafx.scene.control.TableView;
import org.controlsfx.control.MasterDetailPane;

public class CollapsibleMasterDetailPane extends MasterDetailPane {
    private Object previouslySelectedItem;

    public void registerTableView(TableView tableView) {
        tableView.setOnMouseClicked(event -> {
            Object selectedItem = tableView.getSelectionModel().getSelectedItem();

            if (previouslySelectedItem == selectedItem) {
                this.setShowDetailNode(!this.isShowDetailNode());
            }

            previouslySelectedItem = selectedItem;
        });
    }
}

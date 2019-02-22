package org.brabocoin.brabocoin.gui.control;

import javafx.beans.DefaultProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ToolBar;

/**
 * ToolBar that supports hidden/unmanaged allItems.
 */
@DefaultProperty("allItems")
public class HiddenItemsToolBar extends ToolBar {

    private final ObservableList<Node> allItems = FXCollections.observableArrayList();

    public HiddenItemsToolBar() {
        super();
        this.allItems.addListener((ListChangeListener<Node>)c -> {
            while (c.next()) {
                c.getAddedSubList().forEach(n -> {
                    if (n.isManaged()) {
                        getItems().add(n);
                    }
                    n.managedProperty().addListener(new ManagedChangeListener(n));
                });
                c.getRemoved().forEach(n -> n.managedProperty().removeListener(new ManagedChangeListener(n)));
            }
        });
    }

    public HiddenItemsToolBar(Node... items) {
        this();
        this.allItems.addAll(items);
    }

    public ObservableList<Node> getAllItems() {
        return allItems;
    }

    private class ManagedChangeListener implements ChangeListener<Boolean> {

        private final Node node;

        private ManagedChangeListener(Node node) {
            this.node = node;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable,
                            Boolean oldValue, Boolean newValue) {
            if (newValue) {
                getItems().add(node);
            }
            else {
                getItems().remove(node);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ManagedChangeListener that = (ManagedChangeListener)o;

            return node != null ? node.equals(that.node) : that.node == null;
        }

        @Override
        public int hashCode() {
            return node != null ? node.hashCode() : 0;
        }
    }
}

package org.brabocoin.brabocoin.gui.control.table;

import io.grpc.MethodDescriptor;
import javafx.scene.control.TableCell;

public class MethodDescriptorTableCell<T> extends TableCell<T, MethodDescriptor> {
    public MethodDescriptorTableCell() {
        super();
    }

    @Override
    protected void updateItem(MethodDescriptor item, boolean empty) {
        if (empty || item == null) {
            setText(null);
        }
        else {
            setText(item.getFullMethodName().split("/", 2)[1]);
        }
    }
}

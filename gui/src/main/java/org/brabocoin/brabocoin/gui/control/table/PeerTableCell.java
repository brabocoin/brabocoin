package org.brabocoin.brabocoin.gui.control.table;

import javafx.scene.control.TableCell;
import org.brabocoin.brabocoin.node.Peer;

public class PeerTableCell<T> extends TableCell<T, Peer> {

    public PeerTableCell() {
        super();
    }

    @Override
    protected void updateItem(Peer item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else if (item == null) {
            setText("?");
        }
        else {
            setText(item.toSocketString());
        }
    }
}

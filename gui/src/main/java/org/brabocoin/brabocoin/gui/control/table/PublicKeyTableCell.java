package org.brabocoin.brabocoin.gui.control.table;

import javafx.scene.control.TableCell;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.util.ByteUtil;

/**
 * Table cell that formats a hash to to a hexadecimal string.
 */
public class PublicKeyTableCell<T> extends TableCell<T, PublicKey> {

    public PublicKeyTableCell() {
        super();
        getStyleClass().add("hash");
    }

    @Override
    protected void updateItem(PublicKey item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else {
            setText(ByteUtil.toHexString(item.toCompressed()));
        }
    }

}

package org.brabocoin.brabocoin.gui.control.table;

import javafx.scene.control.TableCell;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.model.Hash;

/**
 * Table cell that formats a hash to to a hexadecimal string.
 */
public class AddressTableCell<T> extends TableCell<T, Hash> {

    public AddressTableCell() {
        super();
        getStyleClass().add("hash");
    }

    @Override
    protected void updateItem(Hash item, boolean empty) {
        if (empty) {
            setText(null);
        }
        else {
            setText(PublicKey.getBase58AddressFromHash(item));
        }
    }

}

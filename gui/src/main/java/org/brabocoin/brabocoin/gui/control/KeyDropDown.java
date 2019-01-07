package org.brabocoin.brabocoin.gui.control;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.wallet.Wallet;

public class KeyDropDown extends ComboBox<PublicKey> {

    public KeyDropDown(Wallet wallet) {
        this.setCellFactory(l -> new ListCell<PublicKey>() {
            @Override
            protected void updateItem(PublicKey item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(item.getBase58Address());
                }
            }
        });

        this.getItems().addAll(wallet.getPublicKeys());
        this.getSelectionModel().selectFirst();
    }
}

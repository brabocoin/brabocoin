package org.brabocoin.brabocoin.gui.control;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.brabocoin.brabocoin.gui.window.DataWindow;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;

public class TransactionDataMenuButton extends MenuButton {

    private Transaction transaction;


    public TransactionDataMenuButton() {
        this(null);
    }

    public TransactionDataMenuButton(Transaction transaction) {
        this.transaction = transaction;

        this.setText("Show data");

        MenuItem showUnsignedData = new MenuItem("Unsigned transaction");
        showUnsignedData.setOnAction(event -> {
            if (this.transaction == null) {
                return;
            }
            BrabocoinProtos.UnsignedTransaction protoTx = ProtoConverter.toProto(
                this.transaction.getUnsignedTransaction(), BrabocoinProtos.UnsignedTransaction.class
            );
            new DataWindow(protoTx).show();
        });

        MenuItem showSignedData = new MenuItem("Signed transaction");
        showSignedData.setOnAction(event -> {
            if (this.transaction == null) {
                return;
            }
            BrabocoinProtos.Transaction protoTx = ProtoConverter.toProto(
                this.transaction, BrabocoinProtos.Transaction.class
            );
            new DataWindow(protoTx).show();
        });

        this.getItems().addAll(showUnsignedData, showSignedData);
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
}

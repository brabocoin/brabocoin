package org.brabocoin.brabocoin.gui.window;

import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.gui.BraboDialog;
import org.brabocoin.brabocoin.gui.view.TransactionCreationView;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.wallet.Wallet;

public class TransactionCreationWindow extends BraboDialog<Transaction> {

    private static final double WIDTH = 600;
    private static final double HEIGHT = 700;
    private final Wallet wallet;

    private VBox transactionCreationView;

    public TransactionCreationWindow(Wallet wallet) {
        this.wallet = wallet;
        this.transactionCreationView = new TransactionCreationView(wallet);

        setTitle("Transaction creation");

        this.getDialogPane().setContent(transactionCreationView);

        setResizable(true);

        // Remove header
        setHeaderText(null);
        setGraphic(null);

        this.getDialogPane().setPrefWidth(WIDTH);
        this.getDialogPane().setPrefHeight(HEIGHT);
    }
}

package org.brabocoin.brabocoin.gui.window;

import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.gui.dialog.BraboDialog;
import org.brabocoin.brabocoin.gui.view.TransactionCreationView;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.wallet.Wallet;

public class TransactionCreationWindow extends BraboDialog<Transaction> {

    private static final double WIDTH = 600;
    private static final double HEIGHT = 700;
    private final Wallet wallet;

    private VBox transactionCreationView;

    public TransactionCreationWindow(State state) {
        this.wallet = state.getWallet();
        this.transactionCreationView = new TransactionCreationView(state, this);

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

package org.brabocoin.brabocoin.gui;

import javafx.application.Platform;
import javafx.util.Duration;
import org.brabocoin.brabocoin.gui.window.ValidationWindow;
import org.brabocoin.brabocoin.listeners.BlockReceivedListener;
import org.brabocoin.brabocoin.listeners.TransactionReceivedListener;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.state.State;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;

public class NotificationManager implements BlockReceivedListener, TransactionReceivedListener {

    private final State state;

    public NotificationManager(State state) {
        this.state = state;

        state.getEnvironment().addBlockListener(this);
        state.getEnvironment().addTransactionListener(this);
    }

    @Override
    public void receivedBlock(Block block) {
        Platform.runLater(() -> Notifications.create()
            .title("New block received!")
            .text("New block at height #" + block.getBlockHeight())
            .action(new Action(
                "Validate",
                (a) -> {
                    new ValidationWindow(state.getBlockchain(),
                        block,
                        state.getBlockValidator()
                    ).show();
                    a.consume();
                }
            ))
            .hideAfter(Duration.seconds(8))
            .showInformation());
    }

    @Override
    public void receivedTransaction(Transaction transaction) {

    }
}

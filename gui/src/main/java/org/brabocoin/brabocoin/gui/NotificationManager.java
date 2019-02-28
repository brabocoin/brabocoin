package org.brabocoin.brabocoin.gui;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.util.Duration;
import org.brabocoin.brabocoin.gui.window.ValidationWindow;
import org.brabocoin.brabocoin.listeners.NotificationListener;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.state.State;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.action.Action;

import java.text.MessageFormat;

public class NotificationManager implements NotificationListener {

    private final State state;

    public NotificationManager(State state) {
        this.state = state;

        state.getEnvironment().addNotificationListener(this);
        state.getBlockProcessor().addNotificationListener(this);
    }

    @Override
    public void receivedBlock(Block block) {
        Platform.runLater(() -> Notifications.create()
            .title("New block received!")
            .text("New block at height #" + block.getBlockHeight())

            .action(
                new Action(
                    "Validate",
                    (a) -> {
                        MenuItem quick = new MenuItem("Quick");
                        quick.setOnAction(e -> {
                            new ValidationWindow(
                                state.getBlockchain(),
                                block,
                                state.getBlockValidator(),
                                state.getConsensus(),
                                false
                            ).show();
                        });

                        MenuItem complete = new MenuItem("Complete");
                        complete.setOnAction(e -> {
                            new ValidationWindow(
                                state.getBlockchain(),
                                block,
                                state.getBlockValidator(),
                                state.getConsensus(),
                                true
                            ).show();
                        });

                        ContextMenu menu = new ContextMenu(quick, complete);
                        menu.show(((Node)a.getSource()), Side.BOTTOM, 0, 0);

                        a.consume();
                    }
                )
            )
            .hideAfter(Duration.seconds(8))
            .showInformation());
    }

    @Override
    public void receivedTransaction(Transaction transaction) {
        Platform.runLater(() -> Notifications.create()
            .title("New transaction received!")
            .action(new Action(
                "Validate",
                (a) -> {
                    new ValidationWindow(
                        transaction,
                        state.getTransactionValidator()
                    ).show();
                    a.consume();
                }
            ))
            .hideAfter(Duration.seconds(8))
            .showInformation());
    }

    @Override
    public void forkSwitched(int height) {
        Platform.runLater(() ->
            Notifications.create()
                .title(MessageFormat.format("Switched fork at height {0}", height))
                .hideAfter(Duration.seconds(8))
                .showInformation()
        );
    }
}

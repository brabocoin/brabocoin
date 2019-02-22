package org.brabocoin.brabocoin.gui.dialog;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.gui.control.NumberTextField;
import org.brabocoin.brabocoin.gui.glyph.BraboGlyph;
import org.brabocoin.brabocoin.model.messages.HandshakeResponse;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.processor.PeerProcessor;
import org.controlsfx.control.StatusBar;

import java.text.MessageFormat;

public class PeerCreationDialog extends BraboDialog<Peer> {

    public static final int WIDTH = 400;
    public static final int PROGRESS_INDICATOR_SIZE = 20;
    public static final int ICON_FONT_SIZE = 20;
    private final BraboConfig config;
    private final PeerProcessor peerProcessor;
    private Node okButtonNode;
    private BraboGlyph failIcon = new BraboGlyph("CRYSAD");
    private BraboGlyph successIcon = new BraboGlyph("HAPPY");

    public PeerCreationDialog(BraboConfig config, PeerProcessor peerProcessor) {
        super(false);
        this.config = config;
        this.peerProcessor = peerProcessor;
        this.setGraphic(null);
        this.setHeaderText(null);
        this.setTitle("Peer creation");
        GridPane grid = new GridPane();

        failIcon.setFontSize(ICON_FONT_SIZE);
        successIcon.setFontSize(ICON_FONT_SIZE);

        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        okButtonNode = this.getDialogPane().lookupButton(ButtonType.OK);

        TextField ipTextField = new TextField();
        TextField portTextField = new NumberTextField();
        Button checkButton = new Button("Connect");
        StatusBar statusBar = new StatusBar();
        statusBar.setText("");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(PROGRESS_INDICATOR_SIZE, PROGRESS_INDICATOR_SIZE);

        grid.setHgap(10);
        grid.setVgap(10);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        okButtonNode.setDisable(true);
        grid.add(new Label("IP or hostname:"), 0, 0, 1, 1);
        grid.add(ipTextField, 1, 0, 1, 1);
        grid.add(new Label("Port:"), 0, 1, 1, 1);
        grid.add(portTextField, 1, 1, 1, 1);
        grid.add(checkButton, 0, 2, 1, 1);
        grid.add(statusBar, 1, 2, 1, 1);

        javafx.beans.value.ChangeListener<String> listener = (observable, oldValue, newValue) -> {
            okButtonNode.setDisable(true);
            statusBar.setGraphic(null);
            statusBar.setText("");
        };

        checkButton.setOnAction(event -> {
            statusBar.setGraphic(progressIndicator);
            statusBar.setText("Handshaking with peer...");

            new Thread(() -> {
                Peer checkPeer = null;
                try {
                    checkPeer = new Peer(MessageFormat.format(
                        "{0}:{1}",
                        ipTextField.getText(),
                        portTextField.getText()
                    ));
                }
                catch (MalformedSocketException e) {
                    // ignore
                }
                if (checkPeer == null) {
                    Platform.runLater(() -> {
                        statusBar.setGraphic(failIcon);
                        statusBar.setText("Invalid IP or port.");
                    });
                    return;
                }

                HandshakeResponse response = peerProcessor.handshake(checkPeer);

                if (response == null || config.networkId() != response.getNetworkId()) {
                    Platform.runLater(() -> {
                        statusBar.setGraphic(failIcon);
                        statusBar.setText("Peer connection failed.");
                    });
                    return;
                }

                Platform.runLater(() -> {
                    okButtonNode.setDisable(false);
                    statusBar.setGraphic(successIcon);
                    statusBar.setText("Peer connection successful.");
                });
            }).start();
        });

        portTextField.setOnAction(event -> checkButton.fire());

        ipTextField.textProperty().addListener(listener);
        portTextField.textProperty().addListener(listener);

        this.getDialogPane().setContent(grid);
        this.getDialogPane().setPrefWidth(WIDTH);

        BraboDialog.setBraboStyling(this.getDialogPane());

        this.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    return new Peer(MessageFormat.format(
                        "{0}:{1}",
                        ipTextField.getText(),
                        portTextField.getText()
                    ));
                }
                catch (MalformedSocketException e) {
                    return null;
                }
            }
            return null;
        });

        this.setOnShowing(event -> ipTextField.requestFocus());
    }
}

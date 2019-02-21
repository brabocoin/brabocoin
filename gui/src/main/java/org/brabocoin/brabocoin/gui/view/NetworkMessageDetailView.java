package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.window.DataWindow;
import org.brabocoin.brabocoin.node.MessageArtifact;
import org.brabocoin.brabocoin.node.NetworkMessage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NetworkMessageDetailView extends SplitPane implements BraboControl, Initializable {

    private static final double DIVIDER_POSITION = 0.5;
    private static final double TITLED_PANE_SPACING = 10.0;
    private static final double MIN_TEXTAREA_HEIGHT = 36.0;
    private final ObjectProperty<NetworkMessage> networkMessage = new SimpleObjectProperty<>();
    @FXML public VBox responseArtifactPane;
    @FXML public VBox requestArtifactPane;
    @FXML public TitledPane responseTitledPane;
    @FXML public TitledPane requestTitledPane;
    @FXML public ScrollPane requestScrollPane;
    @FXML public ScrollPane responseScrollPane;

    public NetworkMessageDetailView() {
        super();

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.setDividerPositions(DIVIDER_POSITION);
        this.setOrientation(Orientation.VERTICAL);

        networkMessage.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadNetworkMessage(newValue);
            }
        });
    }

    private void loadNetworkMessage(NetworkMessage message) {
        List<MessageArtifact> requestMessages = message.getRequestMessages();
        addArtifacts(requestMessages, requestArtifactPane, requestTitledPane);
        List<MessageArtifact> responseMessages = message.getResponseMessages();
        addArtifacts(responseMessages, responseArtifactPane, responseTitledPane);
    }

    private void resetTitledPane(TitledPane titledPane) {
        if (titledPane.getGraphic() != null) {
            titledPane.setText(
                extractTitleFromTitledArtifactPane(titledPane)
            );
            titledPane.setGraphic(null);
        }
    }

    private void addArtifacts(List<MessageArtifact> artifacts, VBox destinationBox,
                              TitledPane titledPane) {
        destinationBox.getChildren().clear();
        if (artifacts.size() > 1) {
            resetTitledPane(titledPane);
            for (int i = 0; i < artifacts.size(); i++) {
                MessageArtifact artifact = artifacts.get(i);
                TitledPane artifactPane = createMessageArtifactTitledPane(artifact, i);

                destinationBox.getChildren().add(artifactPane);
            }
        }
        else if (artifacts.size() == 1) {
            TextArea textArea = createMessageArtifactTextArea(artifacts.get(0));
            VBox.setVgrow(textArea, Priority.ALWAYS);
            destinationBox.getChildren().add(textArea);
            String title;
            if (titledPane.getGraphic() == null) {
                title = titledPane.getText();
            } else {
                title = extractTitleFromTitledArtifactPane(titledPane);
            }
            titledPane.setGraphic(createTitledPaneGraphic(
                title,
                artifacts.get(0).getMessage()
            ));
            titledPane.setText("");
        }
        else {
            resetTitledPane(titledPane);
            Label label = new Label("No messages found.");
            label.setPadding(new Insets(3, 7, 3, 7));
            destinationBox.getChildren().add(label);
        }
    }

    private String extractTitleFromTitledArtifactPane(TitledPane titledPane) {
        return ((Label)((HBox)titledPane.getGraphic()).getChildren().get(0)).getText();
    }

    private TitledPane createMessageArtifactTitledPane(MessageArtifact artifact, int index) {
        TextArea textArea = createMessageArtifactTextArea(artifact);

        // Compute textarea height that fits parent
        double textHeight = Math.max(MIN_TEXTAREA_HEIGHT, getTextAreaTextHeight(textArea));
        textArea.setPrefHeight(Math.ceil(textArea.getInsets().getBottom() + textArea.getInsets().getTop() + textHeight));
        textArea.setMinHeight(Region.USE_PREF_SIZE);

        TitledPane titledPane = createShowDataTitledPane(
            "Artifact " + index,
            textArea,
            artifact.getMessage()
        );
        titledPane.setCollapsible(true);
        titledPane.setExpanded(false);

        return titledPane;
    }

    private double getTextAreaTextHeight(TextArea textArea) {
        StringBuilder sb = new StringBuilder();
        textArea.getParagraphs().forEach(p -> sb.append("W\n"));
        sb.append("W\nW");
        Text helper = new Text();
        helper.setText(sb.toString());
        helper.setFont(textArea.getFont());
        return helper.getLayoutBounds().getHeight();
    }

    private HBox createTitledPaneGraphic(String title, Message message) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(TITLED_PANE_SPACING);

        Label titleOfTitledPane = new Label(title);

        Button buttonShowData = new Button("Show data");
        buttonShowData.setOnAction(event -> new DataWindow(message).show());

        hbox.getChildren().add(titleOfTitledPane);
        hbox.getChildren().add(buttonShowData);

        return hbox;
    }

    private TitledPane createShowDataTitledPane(String title, Node content, Message message) {
        TitledPane pane = new TitledPane("", content);
        pane.setGraphic(createTitledPaneGraphic(title, message));

        return pane;
    }

    private TextArea createMessageArtifactTextArea(MessageArtifact artifact) {
        String json = "";
        try {
            json = JsonFormat.printer().print(artifact.getMessage());
        }
        catch (InvalidProtocolBufferException e) {
            // ignored
        }
        TextArea content = new TextArea(json);
        content.setEditable(false);
        content.getStyleClass().add("monospace");

        return content;
    }

    public void setNetworkMessage(NetworkMessage networkMessage) {
        this.networkMessage.set(networkMessage);
    }
}

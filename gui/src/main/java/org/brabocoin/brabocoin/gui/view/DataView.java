package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.control.SelectableLabel;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.util.ByteUtil;

import java.net.URL;
import java.util.ResourceBundle;

public class DataView extends VBox implements BraboControl, Initializable {

    @FXML private SelectableLabel objectName;
    @FXML private TextArea jsonTextArea;
    @FXML private TextArea hexDataTextArea;
    @FXML private SelectableLabel hashSHA256;
    @FXML private SelectableLabel hashDoubleSHA256;

    private final Message message;

    public DataView(Message message) {
        this.message = message;
        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set object name to class simple name
        this.objectName.setText(message.getClass().getSimpleName());

        // Render object to JSON format
        try {
            this.jsonTextArea.setText(JsonFormat.printer().print(message));
        }
        catch (InvalidProtocolBufferException e) {
            this.jsonTextArea.setText("Could not render object to JSON format.");
        }

        this.hexDataTextArea.setText(ByteUtil.toHexString(message.toByteString()));

        Hash SHA = Hashing.digestSHA256(message.toByteString());
        this.hashSHA256.setText(
            ByteUtil.toHexString(SHA.getValue())
        );

        this.hashDoubleSHA256.setText(
            ByteUtil.toHexString(Hashing.digestSHA256(SHA).getValue())
        );
    }
}

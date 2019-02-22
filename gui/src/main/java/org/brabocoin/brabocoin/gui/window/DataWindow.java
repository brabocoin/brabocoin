package org.brabocoin.brabocoin.gui.window;

import com.google.protobuf.Message;
import org.brabocoin.brabocoin.gui.dialog.BraboDialog;
import org.brabocoin.brabocoin.gui.view.DataView;

public class DataWindow extends BraboDialog {
    public DataWindow(Message message) {
        super();

        this.setTitle("Data view");
        this.setResizable(true);

        this.getDialogPane().setContent(new DataView(message));
    }
}

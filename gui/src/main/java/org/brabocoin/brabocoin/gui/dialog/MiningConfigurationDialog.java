package org.brabocoin.brabocoin.gui.dialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.config.MiningConfig;
import org.brabocoin.brabocoin.gui.control.KeyDropDown;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.wallet.Wallet;

public class MiningConfigurationDialog extends BraboValidatedDialog<MiningConfig> {

    IndexedBlock currentParentBlock = null;

    public MiningConfigurationDialog(MiningConfig config, Wallet wallet, Blockchain blockchain) {
        super();
        this.setGraphic(null);
        this.setHeaderText(null);
        this.setTitle("Mining configuration");

        grid.add(new Label("Mining reward address:"), 0, 0, 1, 1);
        KeyDropDown keyDropDown = new KeyDropDown(wallet);
        keyDropDown.getSelectionModel().select(
            config.getRewardPublicKey()
        );
        grid.add(keyDropDown, 1, 0, 1, 1);

        CheckBox mineOnTopBlock = new CheckBox("Mine on top block");
        TextField parentBlockHashTextField = new TextField();
        parentBlockHashTextField.setEditable(config.getParentBlock() != null);
        Label parentBlockHashLabel = new Label("Parent block hash:");

        mineOnTopBlock.selectedProperty().addListener((observable, oldValue, newValue) -> {
            parentBlockHashTextField.setEditable(!newValue);
            if (newValue) {

                parentBlockHashTextField.setText("");
                parentBlockHashLabel.setStyle("-fx-text-fill: grey");
                hideErrorLabel();
            }
            else {
                parentBlockHashLabel.setStyle("-fx-text-fill: black");
                parentBlockHashTextField.setText(parentBlockHashTextField.getText());
            }
        });
        mineOnTopBlock.setSelected(config.getParentBlock() == null);

        grid.add(mineOnTopBlock, 0, 1, 2, 1);
        grid.add(parentBlockHashLabel, 0, 2, 1, 1);
        grid.add(parentBlockHashTextField, 1, 2, 1, 1);


        grid.add(messageLabel, 0, 3, 2, 1);

        parentBlockHashTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (parentBlockHashTextField.getText().equals("")) {
                return;
            }

            Hash parentBlockHash;
            try {
                parentBlockHash =
                    new Hash(ByteUtil.fromHexString(parentBlockHashTextField.getText()));
            }
            catch (IllegalArgumentException e) {
                setError("Parent block hash of invalid format.");
                return;
            }

            boolean validBlock = true;
            IndexedBlock indexedBlock = null;
            try {
                indexedBlock = blockchain.getIndexedBlock(parentBlockHash);
            }
            catch (DatabaseException e) {
                validBlock = false;
            }

            if (!validBlock || indexedBlock == null) {
                setError("Parent block hash does not exist in the chain.");
                return;
            }

            currentParentBlock = indexedBlock;

            hideErrorLabel();
        });
        parentBlockHashTextField.setText(
            config.getParentBlock() == null ? "" :
                ByteUtil.toHexString(config.getParentBlock().getHash().getValue()));

        this.getDialogPane().setContent(grid);

        this.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new MiningConfig(
                    keyDropDown.getSelectionModel().getSelectedItem(),
                    mineOnTopBlock.isSelected() ? null : currentParentBlock
                );
            }
            return null;
        });
    }
}

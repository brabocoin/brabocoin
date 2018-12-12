package org.brabocoin.brabocoin.gui.window;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.gui.BraboDialog;
import org.brabocoin.brabocoin.gui.view.ValidationView;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.Validator;
import org.controlsfx.control.MasterDetailPane;

import java.io.IOException;

public class ValidationWindow extends BraboDialog {
    private MasterDetailPane masterDetailPane;

    /**
     * Validation window for a given block.
     *
     * @param block
     *     The block to create a validation window for
     */
    public ValidationWindow(Blockchain blockchain, Block block, Validator<Block> validator) throws IOException {
        super();

        setProperties();

        setTitle("Block Validation");
        setHeaderText(String.format(
            "Validating block %s",
            ByteUtil.toHexString(block.getHash().getValue(), 32)
        ));

        masterDetailPane = new ValidationView(blockchain, block, validator);

        this.getDialogPane().setContent(masterDetailPane);
        this.getDialogPane().setMinHeight(100.0);
    }

    /**
     * Validation window for a given transaction.
     *
     * @param transaction
     *     The transaction to create a validation window for
     */
    public ValidationWindow(Transaction transaction) throws IOException {
        super();

        setProperties();

        setTitle("Transaction Validation");
        setHeaderText(String.format(
            "Validating transaction %s",
            ByteUtil.toHexString(transaction.getHash().getValue(), 32)
        ));
    }

    private void setProperties() {
        setResizable(true);
    }

}

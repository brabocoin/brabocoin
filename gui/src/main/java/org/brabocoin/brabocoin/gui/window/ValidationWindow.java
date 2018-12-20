package org.brabocoin.brabocoin.gui.window;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.gui.BraboDialog;
import org.brabocoin.brabocoin.gui.view.ValidationView;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.Validator;
import org.controlsfx.control.MasterDetailPane;

import java.io.IOException;

public class ValidationWindow extends BraboDialog {

    private static final double WIDTH = 800.0;
    private static final double SLIDER_POSITION = 0.5;
    private MasterDetailPane masterDetailPane;

    /**
     * Validation window for a given block.
     *
     * @param block
     *     The block to create a validation window for
     */
    public ValidationWindow(Blockchain blockchain, Block block,
                            Validator<Block> validator) {
        super();

        setTitle("Block Validation");

        masterDetailPane = new ValidationView(blockchain, block, validator);
        setProperties();
    }

    /**
     * Validation window for a given transaction.
     *
     * @param transaction
     *     The transaction to create a validation window for
     */
    public ValidationWindow(Transaction transaction, Validator<Transaction> validator) throws IOException {
        super();
        setTitle("Transaction Validation");

        masterDetailPane = new ValidationView(transaction, validator);
        setProperties();

    }

    private void setProperties() {
        this.getDialogPane().setContent(masterDetailPane);

        setResizable(true);

        // Remove header
        setHeaderText(null);
        setGraphic(null);

        this.getDialogPane().setMinWidth(WIDTH);

        masterDetailPane.setDividerPosition(SLIDER_POSITION);
    }
}

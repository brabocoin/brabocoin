package org.brabocoin.brabocoin.gui.window;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.gui.dialog.BraboDialog;
import org.brabocoin.brabocoin.gui.view.BlockValidationView;
import org.brabocoin.brabocoin.gui.view.TransactionValidationView;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
import org.brabocoin.brabocoin.validation.block.BlockValidator;
import org.brabocoin.brabocoin.validation.transaction.TransactionValidator;
import org.controlsfx.control.MasterDetailPane;

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
                            BlockValidator validator, Consensus consensus,
                            boolean withRevertedUTXO) {
        super();
        setTitle("Block Validation");

        masterDetailPane = new BlockValidationView(
            blockchain,
            block,
            validator,
            consensus,
            withRevertedUTXO
        );
        setProperties();
    }

    /**
     * Validation window for a given transaction.
     *
     * @param transaction
     *     The transaction to create a validation window for
     */
    public ValidationWindow(Blockchain blockchain, Transaction transaction,
                            TransactionValidator validator, Consensus consensus,
                            boolean withRevertedUTXO) {
        super();
        setTitle("Transaction Validation");

        masterDetailPane = new TransactionValidationView(
            transaction,
            blockchain,
            consensus,
            validator,
            withRevertedUTXO
        );
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

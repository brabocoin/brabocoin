package org.brabocoin.brabocoin.gui.view.block;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.view.CoinbaseDetailView;
import org.brabocoin.brabocoin.gui.view.TransactionDetailView;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Transaction;

import java.util.List;

/**
 * Pane displaying the block header.
 */
public class BlockTransactionsPane extends TitledPane implements BraboControl {

    private final ObjectProperty<Block> block = new SimpleObjectProperty<>();

    @FXML private VBox transactionsPane;

    public BlockTransactionsPane() {
        super();
        BraboControlInitializer.initialize(this);

        block.addListener((obs, old, val) -> {
            if (val != null) loadBlock();
        });
    }

    private void loadBlock() {
        Block block = this.block.get();

        if (block == null) {
            return;
        }

        transactionsPane.getChildren().clear();

        // Show coinbase (if available)
        if (block.getCoinbaseTransaction() != null) {
            CoinbaseDetailView coinbaseView = new CoinbaseDetailView(block.getCoinbaseTransaction());
            coinbaseView.setShowHeader(false);
            TitledPane coinbasePane = new TitledPane("Coinbase transaction", coinbaseView);
            coinbasePane.setExpanded(false);
            transactionsPane.getChildren().add(coinbasePane);
        }

        List<Transaction> transactions = block.getTransactions();
        for (int i = 1; i < transactions.size(); i++) {
            Transaction tx = transactions.get(i);

            TransactionDetailView detailView = new TransactionDetailView(tx, null);
            detailView.setShowHeader(false);

            TitledPane pane = new TitledPane("Transaction " + i, detailView);
            pane.setExpanded(false);

            transactionsPane.getChildren().add(pane);
        }
    }

    public void setBlock(Block value) {
        block.set(value);
    }
}

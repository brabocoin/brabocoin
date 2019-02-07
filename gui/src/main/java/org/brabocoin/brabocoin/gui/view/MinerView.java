package org.brabocoin.brabocoin.gui.view;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.config.MiningConfig;
import org.brabocoin.brabocoin.gui.dialog.MiningConfigurationDialog;
import org.brabocoin.brabocoin.gui.task.TaskManager;
import org.brabocoin.brabocoin.gui.view.block.BlockDetailsPane;
import org.brabocoin.brabocoin.gui.view.block.BlockHeaderPane;
import org.brabocoin.brabocoin.gui.view.block.BlockTransactionsPane;
import org.brabocoin.brabocoin.mining.Miner;
import org.brabocoin.brabocoin.mining.MiningBlock;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.processor.BlockProcessorListener;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.wallet.Wallet;
import org.controlsfx.control.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Miner view.
 */
public class MinerView extends BorderPane implements BraboControl, Initializable,
                                                     BlockProcessorListener {

    private static final Logger LOGGER = Logger.getLogger(MinerView.class.getName());

    private final @NotNull Miner miner;
    private final @NotNull Blockchain blockchain;
    private final @NotNull NodeEnvironment nodeEnvironment;
    private final @NotNull Wallet wallet;

    private final @NotNull TaskManager taskManager;
    private AnimationTimer timer;
    private MiningConfig config;
    private boolean isMiningContinuously;

    private @Nullable Task<Block> miningTask;
    private @NotNull IndexedBlock parentBlock;

    @FXML private VBox container;

    @FXML private ProgressIndicator miningProgressIndicator;
    @FXML private Label titleLabel;

    @FXML private BlockHeaderPane blockHeaderPane;
    @FXML private BlockDetailsPane blockDetailsPane;
    @FXML private BlockTransactionsPane blockTransactionsPane;

    @FXML private TextField timeField;
    @FXML private TextField iterationsField;
    @FXML private TextField targetValueField;
    @FXML private TextField bestHashField;

    public MinerView(@NotNull State state,
                     @NotNull TaskManager taskManager) {
        super();
        this.miner = state.getMiner();
        this.blockchain = state.getBlockchain();
        this.wallet = state.getWallet();
        this.nodeEnvironment = state.getEnvironment();
        this.taskManager = taskManager;
        this.config = new MiningConfig(wallet.getPublicKeys().iterator().next());
        this.parentBlock = config.getParentBlock();
        state.getBlockProcessor().addBlockProcessorListener(this);

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        blockDetailsPane.setBlockchain(blockchain);

        timer = new AnimationTimer() {
            private long startTime;

            @Override
            public void start() {
                startTime = System.currentTimeMillis();
                super.start();
            }

            @Override
            public void handle(long now) {
                long delta = System.currentTimeMillis() - startTime;

                long seconds = delta / 1000;
                timeField.setText(String.format("%d:%02d", seconds / 60, seconds % 60));

                updateMinerInfo();
            }
        };
    }

    private void updateMinerInfo() {
        MiningBlock block = miner.getMiningBlock();

        if (block != null) {
            IndexRange targetSelection = targetValueField.getSelection();
            targetValueField.setText(ByteUtil.toHexString(
                block.getTargetValue().getValue(),
                Constants.BLOCK_HASH_SIZE
            ));
            targetValueField.selectRange(targetSelection.getStart(), targetSelection.getEnd());

            iterationsField.setText(String.format("%,d", block.getIterations()));

            Hash bestHash = block.getBestHash();

            if (bestHash != null) {
                IndexRange hashSelection = bestHashField.getSelection();
                bestHashField.setText(ByteUtil.toHexString(
                    block.getBestHash().getValue(),
                    Constants.BLOCK_HASH_SIZE
                ));
                bestHashField.selectRange(hashSelection.getStart(), hashSelection.getEnd());
            }

            blockHeaderPane.setNonce(block.getNonce());
        }
    }

    private void showCurrentBlock() {
        MiningBlock block = miner.getMiningBlock();

        if (block == null) {
            return;
        }

        titleLabel.setText("Mining block #" + block.getBlockHeight());

        blockHeaderPane.setBlock(block);
        blockDetailsPane.setBlock(block);
        blockTransactionsPane.setBlock(block);

        Platform.runLater(() -> {
            container.setManaged(true);
            container.setVisible(true);
        });
    }

    @FXML
    private void mineSingleBlock() {
        isMiningContinuously = false;
        initiateMininig();
    }

    @FXML
    private void autoMine() {
        isMiningContinuously = true;
        initiateMininig();
    }

    @FXML
    private void configuration() {
        Optional<MiningConfig> configOptional = new MiningConfigurationDialog(
            config,
            wallet,
            blockchain
        ).showAndWait();

        configOptional.ifPresent(miningConfig -> config = miningConfig);
    }

    private void initiateMininig() {
        if (miningTask != null && !miningTask.isDone()) {
            return;
        }

        miningTask = new Task<Block>() {
            @Override
            protected Block call() {
                updateTitle("Mining new block...");

                parentBlock = config.hasCustomParentBlock()
                    ? config.getParentBlock()
                    : blockchain.getMainChain().getTopBlock();

                return miner.mineNewBlock(
                    parentBlock,
                    config.getRewardPublicKey().getHash()
                );
            }

            @Override
            protected void cancelled() {
                miner.stop();
            }
        };

        miningTask.setOnSucceeded(event -> {
            Block block = miningTask.getValue();
            if (block == null) {
                return;
            }

            updateMinerInfo();

            Notifications.create()
                .title("New block mined!")
                .text("New block at height #" + block.getBlockHeight())
                .showConfirm();

            try {
                ValidationStatus status = nodeEnvironment.processNewlyMinedBlock(block);

                if (!updateConfigParentBlock(block)) {
                    return;
                }

                // Continue mining if setting is enabled and the mined block was valid
                if (isMiningContinuously && status == ValidationStatus.VALID) {
                    initiateMininig();
                }
            }
            catch (DatabaseException e) {
                e.printStackTrace();
            }
        });

        miningTask.stateProperty().addListener((obs, old, state) -> {
            switch (state) {
                case RUNNING:
                    showCurrentBlock();
                    miningProgressIndicator.setProgress(-1);
                    timer.start();
                    break;

                case SUCCEEDED:
                    miningProgressIndicator.setProgress(1);
                    timer.stop();
                    break;

                case FAILED:
                case CANCELLED:
                    miningProgressIndicator.setProgress(0);
                    timer.stop();
            }
        });

        taskManager.runTask(miningTask);
    }

    @FXML
    private void stop() {
        if (miningTask != null) {
            miningTask.cancel();
        }
    }

    @Override
    public void onValidBlockProcessed(@NotNull Block block) {
        Platform.runLater(() -> {
            if (miningTask == null || !miningTask.isRunning()) {
                return;
            }

            // Check if a new block mined on the parent block, restart mining if so
            if (parentBlock.getHash().equals(block.getPreviousBlockHash())) {
                stop();

                if (!updateConfigParentBlock(block)) {
                    return;
                }

                initiateMininig();
            }
        });
    }

    private boolean updateConfigParentBlock(@NotNull Block block) {
        if (config.hasCustomParentBlock()) {
            try {
                config.setParentBlock(blockchain.getIndexedBlock(block.getHash()));
            }
            catch (DatabaseException e) {
                LOGGER.log(
                    Level.SEVERE,
                    "Could not retrieve processed block to set new mining target."
                );
                return false;
            }
        }
        return true;
    }
}

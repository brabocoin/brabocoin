package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.ByteString;
import javafx.animation.AnimationTimer;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.task.TaskManager;
import org.brabocoin.brabocoin.mining.Miner;
import org.brabocoin.brabocoin.mining.MiningBlock;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.NodeEnvironment;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.controlsfx.control.Notifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Miner view.
 */
public class MinerView extends BorderPane implements BraboControl, Initializable {

    private final @NotNull Miner miner;
    private final @NotNull Blockchain blockchain;
    private final @NotNull BlockProcessor blockProcessor;
    private final @NotNull NodeEnvironment nodeEnvironment;

    private final @NotNull TaskManager taskManager;
    private AnimationTimer timer;

    private @Nullable Task<Block> miningTask;

    @FXML private CheckBox continueMining;

    @FXML private TextField timeField;
    @FXML private TextField iterationsField;
    @FXML private TextField targetValueField;
    @FXML private TextField bestHashField;

    public MinerView(@NotNull Miner miner, @NotNull Blockchain blockchain,
                     @NotNull BlockProcessor blockProcessor,
                     @NotNull NodeEnvironment nodeEnvironment,
                     @NotNull TaskManager taskManager) {
        super();
        this.miner = miner;
        this.blockchain = blockchain;
        this.blockProcessor = blockProcessor;
        this.nodeEnvironment = nodeEnvironment;
        this.taskManager = taskManager;

        BraboControlInitializer.initialize(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
            targetValueField.setText(ByteUtil.toHexString(
                block.getTargetValue().getValue(),
                Constants.BLOCK_HASH_SIZE
            ));

            iterationsField.setText(String.format("%,d", block.getIterations()));

            Hash bestHash = block.getBestHash();

            if (bestHash != null) {
                bestHashField.setText(ByteUtil.toHexString(
                    block.getBestHash().getValue(),
                    Constants.BLOCK_HASH_SIZE
                ));
            }
        }
    }

    @FXML
    private void autoMine() {
        if (miningTask != null && !miningTask.isDone()) {
            return;
        }

        miningTask = new Task<Block>() {
            @Override
            protected Block call() {
                updateTitle("Mining new block...");

                return miner.mineNewBlock(
                    blockchain.getMainChain().getTopBlock(),
                    new Hash(ByteString.copyFromUtf8("address"))
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
                ValidationStatus status = blockProcessor.processNewBlock(block);
                if (status == ValidationStatus.VALID) {
                    nodeEnvironment.announceBlockRequest(block);

                    // Continue mining if setting is enabled
                    if (continueMining.isSelected()) {
                        autoMine();
                    }
                }
            }
            catch (DatabaseException e) {
                e.printStackTrace();
            }
        });

        miningTask.stateProperty().addListener((obs, old, state) -> {
            switch (state) {
                case RUNNING:
                    timer.start();
                    break;

                case FAILED:
                case SUCCEEDED:
                case CANCELLED:
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
}

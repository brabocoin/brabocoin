package org.brabocoin.brabocoin.gui.view;

import com.google.protobuf.ByteString;
import javafx.animation.AnimationTimer;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.brabocoin.brabocoin.Constants;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.gui.BraboControl;
import org.brabocoin.brabocoin.gui.BraboControlInitializer;
import org.brabocoin.brabocoin.gui.task.TaskManager;
import org.brabocoin.brabocoin.mining.Miner;
import org.brabocoin.brabocoin.mining.MiningBlock;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.util.ByteUtil;
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

    private final @NotNull TaskManager taskManager;
    private AnimationTimer timer;

    private @Nullable Task<Block> miningTask;
    private @Nullable Block minedBlock;

    @FXML private TextField timeField;
    @FXML private TextField iterationsField;
    @FXML private TextField targetValueField;
    @FXML private TextField bestHashField;

    public MinerView(@NotNull Miner miner, @NotNull Blockchain blockchain,
                     @NotNull TaskManager taskManager) {
        super();
        this.miner = miner;
        this.blockchain = blockchain;
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
        };
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
            minedBlock = miningTask.getValue();
            if (minedBlock == null) {
                return;
            }

            Notifications.create()
                .title("New block mined!")
                .text("New block at height #" + minedBlock.getBlockHeight())
                .showConfirm();
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

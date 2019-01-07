package org.brabocoin.brabocoin.mining;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import org.brabocoin.brabocoin.crypto.Hashing;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.validation.Consensus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.List;
import java.util.logging.Logger;

/**
 * A block that can be mined.
 */
@ProtoClass(BrabocoinProtos.Block.class)
public class MiningBlock extends Block {

    private static final Logger LOGGER = Logger.getLogger(MiningBlock.class.getName());

    private volatile boolean stopped;

    private @NotNull BigInteger nonce;

    private @Nullable Hash bestHash;

    private long iterations;

    /**
     * Create a new mining block block.
     *
     * @param previousBlockHash
     *     Hash of the previous block in the blockchain.
     * @param merkleRoot
     *     Hash of the Merkle root.
     * @param targetValue
     *     Target value for the proof-of-work.
     * @param nonce
     *     Starting nonce for the proof-of-work.
     * @param blockHeight
     *     Height of the block in the blockchain.
     * @param transactions
     * @param networkId
     */
    public MiningBlock(@NotNull Hash previousBlockHash, @NotNull Hash merkleRoot,
                       @NotNull Hash targetValue, @NotNull BigInteger nonce, int blockHeight,
                       List<Transaction> transactions, int networkId) {
        super(
            previousBlockHash,
            merkleRoot,
            targetValue,
            nonce,
            blockHeight,
            transactions,
            networkId
        );

        this.nonce = nonce;
    }

    /**
     * Mine a block by incrementing the nonce in the block header and recomputing the block hash,
     * until the block hash is at most the target value.
     * <p>
     * This method is blocking and can potentially take very long. The execution of this method
     * can be stopped when {@link #stop()} is called.
     * In that case, {@code null} is returned.
     *
     * @param consensus
     *     The consensus.
     * @return The mined block or {@code null} if the mining was prematurely terminated.
     */
    public @Nullable Block mine(@NotNull Consensus consensus) {
        LOGGER.fine("Start mining the block.");

        while (!stopped && !isBlockHashValid()) {
            nonce = nonce.add(BigInteger.ONE).mod(consensus.getMaxNonce());
            iterations++;
        }

        // If forcefully stopped, and the block hash is still not valid, return null
        if (!isBlockHashValid()) {
            LOGGER.info("Mining of the block was interrupted.");
            return null;
        }

        LOGGER.info("New block is mined.");
        return toBlock();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isBlockHashValid() {
        Hash hash = getHash();

        if (bestHash == null || hash.compareTo(bestHash) < 0) {
            bestHash = hash;
        }

        return hash.compareTo(getTargetValue()) <= 0;
    }

    @Override
    public @NotNull Hash getHash() {
        ByteString header = getRawHeader();
        return Hashing.digestSHA256(Hashing.digestSHA256(header));
    }

    @Contract(" -> new")
    private @NotNull Block toBlock() {
        return new Block(
            getPreviousBlockHash(),
            getMerkleRoot(),
            getTargetValue(),
            getNonce(),
            getBlockHeight(),
            getTransactions(),
            getNetworkId()
        );
    }

    /**
     * Stop the execution of the {@link #mine(Consensus)} method.
     */
    public void stop() {
        stopped = true;
    }

    /**
     * Whether the execution of the {@link #mine(Consensus)} method was stopped.
     *
     * @return Whether the execution was stopped.
     */
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public @NotNull BigInteger getNonce() {
        return nonce;
    }

    /**
     * Get the best hash found while mining this block, or {@code null} if this block did not yet
     * start mining.
     *
     * @return The best hash found while mining.
     */
    public @Nullable Hash getBestHash() {
        return bestHash;
    }

    public long getIterations() {
        return iterations;
    }
}

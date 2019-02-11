package org.brabocoin.brabocoin.mining;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.MerkleTree;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.validation.transaction.TransactionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Miner can mine blocks.
 * <p>
 * The transactions that are included in the new block are selected from the transaction pool.
 * Only independent transactions are selected from the pool.
 */
public class Miner {

    private static final Logger LOGGER = Logger.getLogger(Miner.class.getName());

    private final @NotNull TransactionPool transactionPool;
    private final @NotNull Consensus consensus;
    private final @NotNull Random random;
    private final ReadonlyUTXOSet utxoSet;
    private final int networkId;
    private final @NotNull Hash targetValue;

    /**
     * Block that is currently mined.
     */
    private @Nullable MiningBlock block;

    private volatile boolean isStopped;

    /**
     * Create a new miner that is able to mine a new block.
     *  @param transactionPool
     *     The transaction pool to retrieve transactions from.
     * @param consensus
     *     The consensus.
     * @param random
     * @param targetValue
     */
    public Miner(@NotNull TransactionPool transactionPool, @NotNull Consensus consensus,
                 @NotNull Random random, @NotNull ReadonlyUTXOSet utxoSet, int networkId,
                 @NotNull Hash targetValue) {
        this.transactionPool = transactionPool;
        this.consensus = consensus;
        this.random = random;
        this.utxoSet = utxoSet;
        this.networkId = networkId;
        this.targetValue = targetValue;
    }

    /**
     * Mine a new block op top op {@code previousBlock}.
     * <p>
     * This method potentially blocks execution for a long time. The execution can be interrupted
     * by calling {@link #stop()}.
     *
     * @param previousBlock
     *     The previous block on top of which the new block is mined.
     * @param coinbaseAddress
     *     The address to which the coinbase output is paid.
     * @return The mined block, or {@code null} if the mining process was interrupted.
     */
    public @Nullable Block mineNewBlock(@NotNull IndexedBlock previousBlock,
                                        @NotNull Hash coinbaseAddress) {
        isStopped = false;

        List<Transaction> transactions = collectTransactions();

        Transaction coinbase = createCoinbase(
            coinbaseAddress,
            transactions,
            previousBlock.getBlockInfo().getBlockHeight() + 1
        );
        transactions.add(0, coinbase);

        List<Hash> hashes = transactions.stream()
            .map(Transaction::getHash)
            .collect(Collectors.toList());

        Hash merkleRoot = new MerkleTree(consensus.getMerkleTreeHashFunction(), hashes).getRoot();

        BigInteger randomStartingNonce = new BigInteger(consensus.getMaxNonceSize() * 8, random);

        LOGGER.fine("Starting to mine a new block.");
        LOGGER.finest(() -> MessageFormat.format(
            "previousBlockHeight={0}, randomStartingNonce={1}, transactionCount={2}",
            previousBlock.getBlockInfo().getBlockHeight(),
            randomStartingNonce.toString(16),
            transactions.size()
        ));

        block = new MiningBlock(
            previousBlock.getHash(),
            merkleRoot,
            targetValue,
            randomStartingNonce,
            previousBlock.getBlockInfo().getBlockHeight() + 1,
            transactions,
            networkId
        );

        if (isStopped) {
            return null;
        }

        return block.mine(consensus);
    }

    private @NotNull Transaction createCoinbase(Hash coinbaseAddress,
                                                List<Transaction> transactions,
                                                int blockHeight) {
        long amount = consensus.getBlockReward() +
            transactions.stream()
                .mapToLong(t -> {
                    try {
                        return TransactionUtil.computeFee(t, utxoSet);
                    }
                    catch (DatabaseException | IllegalStateException e) {
                        return 0;
                    }
                })
                .sum();

        return Transaction.coinbase(new Output(coinbaseAddress, amount), blockHeight);
    }

    /**
     * Stop the mining process if currently a block is being mined.
     *
     * @see #mineNewBlock(IndexedBlock, Hash)
     * @see MiningBlock#stop()
     */
    public void stop() {
        isStopped = true;

        if (block != null) {
            LOGGER.fine("Stopping the mining process.");
            block.stop();
        }
    }

    /**
     * Collect all independent transactions.
     *
     * @return A list of independent transactions such that the maximum block size is not exceeded.
     */
    private @NotNull List<Transaction> collectTransactions() {
        long maxTransactionsSize = consensus.getMaxTransactionSize();

        int usedSize = 0;
        List<Transaction> transactions = new ArrayList<>();

        Iterator<Transaction> independent = transactionPool.independentTransactionsIterator();

        // Add all independent transactions
        while (independent.hasNext()) {
            Transaction transaction = independent.next();

            ByteString serialized = ProtoConverter.toProtoBytes(
                transaction,
                BrabocoinProtos.Transaction.class
            );
            if (serialized == null) {
                // TODO: error handling?
                LOGGER.severe("Transaction could not be serialized.");
                continue;
            }

            int transactionSize = serialized.size();

            // Skip if the max transactions size is reached
            if (usedSize + transactionSize > maxTransactionsSize) {
                LOGGER.finest(() -> MessageFormat.format(
                    "Transaction with size {0} is skipped.",
                    transactionSize
                ));
                continue;
            }

            usedSize += transactionSize;
            transactions.add(transaction);

            LOGGER.finest("New transaction is selected to be mined in a block.");
        }

        return transactions;
    }

    public @Nullable MiningBlock getMiningBlock() {
        return block;
    }
}

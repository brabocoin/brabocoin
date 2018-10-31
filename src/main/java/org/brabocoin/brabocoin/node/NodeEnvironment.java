package org.brabocoin.brabocoin.node;

import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.processor.*;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a node environment.
 */
public class NodeEnvironment {
    private static final Logger LOGGER = Logger.getLogger(NodeEnvironment.class.getName());
    protected BraboConfig config;

    /**
     * Main loop timer
     */
    Timer mainLoopTimer;

    /**
     * Data holders
     */
    private int servicePort;
    private Blockchain blockchain;
    private TransactionPool transactionPool;
    private Queue<Runnable> messageQueue = new LinkedList<>();

    /**
     * Processors
     */
    private BlockProcessor blockProcessor;
    private PeerProcessor peerProcessor;
    private TransactionProcessor transactionProcessor;

    public NodeEnvironment(int servicePort,
                           Blockchain blockchain,
                           BlockProcessor blockProcessor,
                           PeerProcessor peerProcessor,
                           TransactionPool transactionPool,
                           TransactionProcessor transactionProcessor,
                           BraboConfig config) {
        this.servicePort = servicePort;
        this.blockchain = blockchain;
        this.blockProcessor = blockProcessor;
        this.peerProcessor = peerProcessor;
        this.transactionPool = transactionPool;
        this.transactionProcessor = transactionProcessor;
        this.config = config;
    }

    /**
     * Setup the environment.
     * This instantiates the bootstrapping process of peers.
     */
    public void setup() {
        LOGGER.info("Setting up the node environment.");
        peerProcessor.bootstrap(servicePort);
        LOGGER.info("Environment setup done.");

        mainLoopTimer = new Timer();
        mainLoopTimer.schedule(new MainLoopTask(), 0, 500);

        updateBlockchain();
    }

    public void stop() {
        mainLoopTimer.cancel();
    }

    class MainLoopTask extends TimerTask {

        @Override
        public void run() {
            while (!messageQueue.isEmpty()) {
                messageQueue.remove().run();
            }
        }
    }

    private void updateBlockchain() {
        Map<Peer, Integer> topBlockHeights = discoverTopBlockHeightRequest();
        Optional<Map.Entry<Peer, Integer>> maxHeightEntryOptional = topBlockHeights
                .entrySet()
                .stream()
                .max(Comparator.comparing(Map.Entry::getValue));

        if (maxHeightEntryOptional.isPresent()) {
            Peer maxHeightPeer = maxHeightEntryOptional.get().getKey();
            Integer maxHeight = maxHeightEntryOptional.get().getValue();
            LOGGER.log(Level.FINE, () -> MessageFormat.format("Max height peer found: {0} at value {1}", maxHeightPeer, maxHeight));

            if (maxHeight > blockchain.getMainChain().getHeight()) {
                // Peer has a longer chain, need to update.
                Hash matchingBlockHash = checkChainCompatibleRequest(maxHeightPeer);

                seekBlockchainRequest(maxHeightPeer, matchingBlockHash);
            }
        } else {
            LOGGER.warning("Could not retrieve max height from peers, no entry found.");
        }
    }

    //================================================================================
    // Peer management
    //================================================================================

    /**
     * Attempts to add the client peer to the list of peers.
     * Used when a client handshakes with this node's service.
     *
     * @param address The address of the client.
     * @param port    The port of the Client
     */
    public void addClientPeer(InetAddress address, int port) {
        try {
            Peer clientPeer = new Peer(address, port);
            peerProcessor.addPeer(clientPeer);
        } catch (MalformedSocketException e) {
            LOGGER.log(Level.WARNING, "Could not add client peer: {0}", e.getMessage());
        }
    }

    public List<Peer> findClientPeers(InetAddress clientAddress) {
        return peerProcessor.findClientPeers(clientAddress);
    }

    /**
     * Propagate a peer consuming lambda expression to all known peers.
     * Only if the given hash was not already propagated.
     *
     * @param peerConsumer The lambda expression evaluated on all peers.
     */
    public void propagateMessage(Consumer<Peer> peerConsumer) {
        for (Peer peer : peerProcessor.copyPeers()) {
            peerConsumer.accept(peer);
        }

        LOGGER.info("Message propagated to all peers.");
    }

    //================================================================================
    // Callbacks
    //================================================================================

    /**
     * Callback on receival of block after a {@code getBlocks} message.
     *
     * @param block     The received block
     * @param peers     The peers for which we request the parent block, if needed.
     * @param propagate Whether or not to propagate the message to peers.
     */
    private void onReceiveBlock(Block block, List<Peer> peers, boolean propagate) {
        try {
            LOGGER.info("Received new block from peer.");
            ProcessedBlockStatus processedBlockStatus = blockProcessor.processNewBlock(block);
            LOGGER.log(Level.FINEST, () -> MessageFormat.format("Processed new block: {0}", processedBlockStatus));
            switch (processedBlockStatus) {
                case ORPHAN:
                    messageQueue.add(() -> getBlocksRequest(
                            new ArrayList<Hash>() {{ add(block.getPreviousBlockHash()); }},
                            peers,
                            propagate
                            ));
                case ADDED_TO_BLOCKCHAIN:
                    if (propagate) {
                        final BrabocoinProtos.Hash protoBlockHash = ProtoConverter.toProto(block.computeHash(), BrabocoinProtos.Hash.class);
                        // TODO: We actually want to use async stub here, but that went wrong before (Cancelled exception by GRPC).
                        messageQueue.add(() -> propagateMessage((Peer p) -> p.getBlockingStub().announceBlock(protoBlockHash)));
                    }
                    break;

                case INVALID:
                case ALREADY_STORED:
                    LOGGER.log(Level.FINE, "Block invalid or already stored.");
                    break;
            }
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Could not store received block: {0}", e.getMessage());
        }
    }

    /**
     * Handles the receival of a new block.
     * Tries to store the block in the blockchain.
     *
     * @param blockHash Hash of the new block.
     */
    public void onReceiveBlockHash(@NotNull Hash blockHash, @NotNull List<Peer> peers) {
        LOGGER.fine("Block hash received.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(blockHash.getValue())));

        try {
            if (blockchain.isBlockStored(blockHash)) {
                LOGGER.log(Level.FINE, "Block was already stored, ignoring.");
                return;
            }

            messageQueue.add(() -> getBlocksRequest(new ArrayList<Hash>() {{
                add(blockHash);
            }}, peers, true));
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Could not check if block was already stored: {0}", e.getMessage());
        }
    }

    /**
     * Handles the receival of a new transaction.
     * Checks whether the transaction is already processed, and if not requests the transaction from the peers.
     *
     * @param transactionHash Hash of the new block.
     * @param peers           The peers to request the transaction from.
     */
    public void onReceiveTransactionHash(@NotNull Hash transactionHash, List<Peer> peers) {
        LOGGER.fine("Transaction hash received.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(transactionHash.getValue())));

        if (transactionPool.contains(transactionHash)) {
            LOGGER.log(Level.FINE, "Transaction was already processed, ignoring.");
            return;
        }

        messageQueue.add(() -> getTransactionRequest(new ArrayList<Hash>() {{
            add(transactionHash);
        }}, peers, true));
    }

    /**
     * Check whether the given block hash is contained in the main chain.
     *
     * @param blockHash Hash of the block to check.
     * @return True if it is contained in the main chain.
     */
    public boolean isChainCompatible(@NotNull Hash blockHash) {
        LOGGER.fine("Chain compatibility check requested for block hash.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(blockHash.getValue())));

        try {
            final IndexedBlock indexedBlock = blockchain.getIndexedBlock(blockHash);

            if (indexedBlock == null) {
                LOGGER.log(Level.FINEST, "Hash not found");
                return false;
            }

            final boolean contains = blockchain.getMainChain().contains(indexedBlock);

            LOGGER.log(Level.FINEST, () -> MessageFormat.format("Hash contained in main chain: {0}", contains));

            return contains;
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Indexed block not available: {0}", e.getMessage());
        }

        return false;
    }

    /**
     * Callback on receival of transaction after a {@code getTransactions} message.
     *
     * @param transaction The received transaction
     * @param propagate   Whether or not to propagate the message to peers.
     */
    private void onReceiveTransaction(Transaction transaction, boolean propagate) {
        try {
            ProcessedTransactionResult result = transactionProcessor.processNewTransaction(transaction);

            switch (result.getStatus()) {
                case DEPENDENT:
                case INDEPENDENT:
                    if (propagate) {
                        final BrabocoinProtos.Hash protoTransactionHash = ProtoConverter.toProto(transaction.computeHash(), BrabocoinProtos.Hash.class);
                        // TODO: We actually want to use async stub here, but that went wrong before (Cancelled exception by GRPC).
                        messageQueue.add(() -> propagateMessage((Peer p) -> p.getBlockingStub().announceTransaction(protoTransactionHash)));
                    }
                    break;

                case ORPHAN:
                case ALREADY_STORED:
                case INVALID:
                    LOGGER.log(Level.FINE, "Transaction invalid, already stored or orphan.");
                    break;
            }

            if (propagate) {
                // Propagate any remaining transactions that became valid.
                for (Transaction t : result.getValidatedOrphans()) {
                    final BrabocoinProtos.Hash protoTransactionHash = ProtoConverter.toProto(t.computeHash(), BrabocoinProtos.Hash.class);
                    messageQueue.add(() -> propagateMessage((Peer p) -> p.getBlockingStub().announceTransaction(protoTransactionHash)));
                }
            }
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Could not store received block: {0}", e.getMessage());
        }

    }

    //================================================================================
    // Requests
    //================================================================================

    /**
     * Tries to acquire the blocks from the given peers using the {@code getBlocks} message, given the block hashes.
     * Also propagates an announce block message if {@code propagate} is set to true.
     *
     * @param hashes    The list of block hashes to fetch.
     * @param peers     The list of peers used to request the blocks.
     * @param propagate Whether or not to propagate an announce message to all peers.
     */
    public void getBlocksRequest(List<Hash> hashes, List<Peer> peers, boolean propagate) {
        LOGGER.info("Getting a list of blocks from peers.");

        for (Peer peer : peers) {
            // TODO: Check if async processing is done correctly.
            StreamObserver<BrabocoinProtos.Hash> hashStreamObserver = peer.getAsyncStub().getBlocks(new StreamObserver<BrabocoinProtos.Block>() {
                @Override
                public void onNext(BrabocoinProtos.Block value) {
                    LOGGER.log(Level.FINEST, () -> {
                        try {
                            return MessageFormat.format("Received peer block: {0}", JsonFormat.printer().print(value));
                        } catch (InvalidProtocolBufferException e) {
                            LOGGER.log(Level.WARNING, "Could not log the JSON format of the response message.", e);
                        }

                        return "";
                    });
                    onReceiveBlock(ProtoConverter.toDomain(value, Block.Builder.class), peers, propagate);
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(Level.WARNING, "Peer returned an error while getting block: {0}", t.getMessage());
                }

                @Override
                public void onCompleted() {
                    LOGGER.log(Level.FINE, "Peer block stream completed.");
                }
            });

            for (Hash hash : hashes) {
                BrabocoinProtos.Hash protoBlockHash = ProtoConverter.toProto(hash, BrabocoinProtos.Hash.class);
                hashStreamObserver.onNext(protoBlockHash);
            }
            hashStreamObserver.onCompleted();
        }
    }

    /**
     * Announce the block's hash to all known peers.
     *
     * @param block The block to announce.
     */
    public void announceBlockRequest(Block block) {
        LOGGER.info("Announcing block to peers.");
        Hash blockHash = block.computeHash();
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(blockHash.getValue()));
        BrabocoinProtos.Hash protoBlockHash = ProtoConverter.toProto(blockHash, BrabocoinProtos.Hash.class);

        for (Peer peer : getPeers()) {
            peer.getAsyncStub().announceBlock(protoBlockHash, new StreamObserver<Empty>() {
                @Override
                public void onNext(Empty value) {
                    LOGGER.log(Level.FINEST, "Received block announce response from peer.");
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(Level.WARNING, "Received block announce error from peer: {0}", t.getMessage());
                }

                @Override
                public void onCompleted() {
                    LOGGER.log(Level.FINEST, "Received block announce completion from peer.");
                }
            });
        }
    }

    /**
     * Announce the transaction's hash to all known peers.
     *
     * @param transaction The transaction to announce.
     */
    public void announceTransactionRequest(Transaction transaction) {
        LOGGER.info("Announcing transaction to peers.");
        Hash transactionHash = transaction.computeHash();
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(transactionHash.getValue()));
        BrabocoinProtos.Hash protoTransactionHash = ProtoConverter.toProto(transactionHash, BrabocoinProtos.Hash.class);

        for (Peer peer : getPeers()) {
            peer.getAsyncStub().announceTransaction(protoTransactionHash, new StreamObserver<Empty>() {
                @Override
                public void onNext(Empty value) {
                    LOGGER.log(Level.FINEST, "Received transaction announce response from peer.");
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(Level.WARNING, "Received transaction announce error from peer: {0}", t.getMessage());
                }

                @Override
                public void onCompleted() {
                    LOGGER.log(Level.FINEST, "Received transaction announce completion from peer.");
                }
            });
        }
    }

    /**
     * Requests any blocks that are mined on top of its current top block.
     * <p>
     * The peer looks up the block corresponding to the received block hash in its main chain.
     * It then returns all the hashes of the blocks on top of this block. Note that only blocks on
     * the main chain are returned. If the requested block is not on the main chain, the node will return no
     * hashes.
     *
     * @param peer         The peer used to seek the blockhain.
     * @param startingHash The hash to start from.
     */
    public void seekBlockchainRequest(Peer peer, Hash startingHash) {
        Iterator<BrabocoinProtos.Hash> hashesAbove = peer
                .getBlockingStub()
                .seekBlockchain(ProtoConverter.toProto(startingHash, BrabocoinProtos.Hash.class));

        List<Hash> hashes = new ArrayList<>();
        hashesAbove.forEachRemaining(h -> hashes.add(ProtoConverter.toDomain(h, Hash.Builder.class)));

        messageQueue.add(() -> getBlocksRequest(hashes, new ArrayList<Peer>() {{
            add(peer);
        }}, false));
    }

    /**
     * Request peers for the top block height.
     *
     * @return A map of peer matching its block height.
     */
    public Map<Peer, Integer> discoverTopBlockHeightRequest() {
        LOGGER.fine("Requesting peers for top block height.");

        Map<Peer, Integer> peerHeights = new HashMap<>();

        for (Peer peer : getPeers()) {
            BrabocoinProtos.BlockHeight protoBlockHeight = peer.getBlockingStub().discoverTopBlockHeight(Empty.newBuilder().build());
            LOGGER.log(Level.FINEST, () -> MessageFormat.format("Received block height from peer ( {0} ): {1}", peer, protoBlockHeight.getHeight()));
            peerHeights.put(peer, protoBlockHeight.getHeight());
        }

        return peerHeights;
    }

    /**
     * The hash of the current top block is sent to the peer.
     * If this hash is present in the main chain of the receiving peer, this peer responds with true, and false otherwise.
     * <p>
     * When the node receives a reply that the block is not present in their main chain,
     * the node will perform an exponential backoff until a hash is found that matches the peer's main chain.
     * <p>
     * If no such block is found and we reach the genesis block, the hash of the genesis block is returned.
     *
     * @param peer The peer to check chain compatibility with.
     * @return The hash of the matched block on the peer's chain.
     */
    public Hash checkChainCompatibleRequest(Peer peer) {
        LOGGER.info("Checking chain compatibility with given peer.");
        int depth = 0;
        Hash currentBlockHash = blockchain.getMainChain().getTopBlock().getHash();
        boolean chainCompatible = false;

        int mainChainHeight = blockchain.getMainChain().getHeight();

        while (!chainCompatible) {
            int nextHeight = mainChainHeight - depth;

            if (nextHeight <= 0) {
                LOGGER.log(Level.FINE, "Genesis block reached.");
                return blockchain.getMainChain().getGenesisBlock().getHash();
            } else {
                currentBlockHash = blockchain.getMainChain().getBlockAtHeight(nextHeight).getHash();
            }

            depth = depth == 0 ? 1 : depth * 2;

            Hash finalCurrentBlockHash = currentBlockHash;
            int finalDepth = depth;
            LOGGER.log(Level.FINEST, () -> MessageFormat.format("Checking block hash at depth {0} : {1}", finalDepth, ByteUtil.toHexString(finalCurrentBlockHash.getValue())));

            BrabocoinProtos.ChainCompatibility chainCompatibility = peer.getBlockingStub().checkChainCompatible(
                    ProtoConverter.toProto(currentBlockHash, BrabocoinProtos.Hash.class)
            );
            chainCompatible = chainCompatibility.getCompatible();
        }

        LOGGER.log(Level.FINE, "Chain compatiblity found.");
        return currentBlockHash;
    }

    /**
     * Requests transactions from (a given list of) peers, given the transaction hashes.
     * Also determine whether or not to propagate when transactions are received.
     *
     * @param hashes    The hashes to request.
     * @param peers     The peers to request transactions from.
     * @param propagate Whether or not to propagate.
     */
    public void getTransactionRequest(List<Hash> hashes, List<Peer> peers, boolean propagate) {
        LOGGER.info("Getting a list of transactions from peers.");

        for (Peer peer : peers) {
            // TODO: Check if async processing is done correctly.
            StreamObserver<BrabocoinProtos.Hash> hashStreamObserver = peer.getAsyncStub().getTransactions(new StreamObserver<BrabocoinProtos.Transaction>() {
                @Override
                public void onNext(BrabocoinProtos.Transaction value) {
                    LOGGER.log(Level.FINEST, () -> {
                        try {
                            return MessageFormat.format("Received peer transaction: {0}", JsonFormat.printer().print(value));
                        } catch (InvalidProtocolBufferException e) {
                            LOGGER.log(Level.WARNING, "Could not log the JSON format of the response message.", e);
                        }

                        return "";
                    });
                    onReceiveTransaction(ProtoConverter.toDomain(value, Transaction.Builder.class), propagate);
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(Level.WARNING, "Peer returned an error while getting transaction: {0}", t.getMessage());
                }

                @Override
                public void onCompleted() {
                    LOGGER.log(Level.FINE, "Peer transaction stream completed.");
                }
            });

            for (Hash hash : hashes) {
                BrabocoinProtos.Hash protoBlockHash = ProtoConverter.toProto(hash, BrabocoinProtos.Hash.class);
                hashStreamObserver.onNext(protoBlockHash);
            }
            hashStreamObserver.onCompleted();
        }
    }

    public void seekTransactionPoolRequest() {
        // TODO: Implement, which peer to use? for logic, see seekBlockchainRequest.
    }

    //================================================================================
    // Getters
    //================================================================================

    /**
     * Get a copy of the set of peers.
     *
     * @return Set of peers.
     */
    public Set<Peer> getPeers() {
        LOGGER.fine("Creating a copy of the set of peers.");
        return peerProcessor.copyPeers();
    }

    /**
     * Handles the request for a block.
     * TODO: Describe logic
     *
     * @param blockHash Hash of the block to get.
     * @return Block instance or null if not found.
     */
    public Block getBlock(@NotNull Hash blockHash) {
        LOGGER.fine("Block requested by hash.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(blockHash.getValue())));
        try {
            return blockchain.getBlock(blockHash);
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Database error while trying to acquire block, error: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the block hashes above a given block hash.
     *
     * @param blockHash The block hash to indicate the starting position.
     * @return List over block hashes above the given block or null if not found on the main chain or the block is not found.
     */
    public List<Hash> getBlocksAbove(Hash blockHash) {
        LOGGER.fine("Block hashes requested above a given block hash.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(blockHash.getValue())));

        try {
            final IndexedBlock indexedBlock = blockchain.getIndexedBlock(blockHash);

            if (indexedBlock == null) {
                return new ArrayList<>();
            }

            boolean inMainChain = blockchain.getMainChain().contains(indexedBlock);

            if (!inMainChain) {
                return new ArrayList<>();
            }

            List<Hash> blocksAbove = new ArrayList<>();
            final int chainHeight = blockchain.getMainChain().getHeight();

            for (int i = indexedBlock.getBlockInfo().getBlockHeight() + 1; i <= chainHeight; i++) {
                IndexedBlock block = blockchain.getMainChain().getBlockAtHeight(i);
                if (block != null) {
                    blocksAbove.add(block.getHash());
                } else {
                    LOGGER.log(Level.SEVERE, "Could not get indexed block in main chain.");
                    throw new IllegalStateException("Intermediate block in chain not found.");
                }
            }

            return blocksAbove;
        } catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Indexed block not available: {0}", e.getMessage());
        }

        return new ArrayList<>();
    }

    /**
     * Gets the height of the top block.
     *
     * @return Top block height.
     */
    public int getTopBlockHeight() {
        LOGGER.fine("Top block height requested.");
        int height = blockchain.getMainChain().getHeight();
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Top height: {0}", height));
        return height;
    }

    /**
     * Handles the request for a transaction.
     * TODO: Describe logic
     *
     * @param transactionHash Hash of the transaction to get.
     * @return Transaction instance or null if not found.
     */
    public Transaction getTransaction(@NotNull Hash transactionHash) {
        LOGGER.fine("Transaction requested by hash.");
        LOGGER.log(Level.FINEST, () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(transactionHash.getValue())));

        return transactionPool.findValidatedTransaction(transactionHash);
    }

    /**
     * Get an iterator for all transaction hashes in the transaction pool.
     *
     * @return Transaction hash iterator.
     */
    public Set<Hash> getTransactionHashSet() {
        LOGGER.fine("Transaction iterator creation requested.");
        return transactionPool.getValidatedTransactionHashes();
    }
}

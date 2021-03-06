package org.brabocoin.brabocoin.node;

import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.config.BraboConfig;
import org.brabocoin.brabocoin.dal.ChainUTXODatabase;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.listeners.NotificationListener;
import org.brabocoin.brabocoin.listeners.PeerSetChangedListener;
import org.brabocoin.brabocoin.listeners.ReorganizeChainListener;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.RejectedBlock;
import org.brabocoin.brabocoin.model.RejectedTransaction;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.node.state.State;
import org.brabocoin.brabocoin.processor.BlockProcessor;
import org.brabocoin.brabocoin.processor.PeerProcessor;
import org.brabocoin.brabocoin.processor.ProcessedTransactionResult;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.proto.services.NodeGrpc;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.validation.ValidationStatus;
import org.brabocoin.brabocoin.validation.block.BlockValidationResult;
import org.brabocoin.brabocoin.validation.block.rules.DuplicateStorageBlkRule;
import org.brabocoin.brabocoin.validation.rule.RuleBookFailMarker;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Represents a node environment.
 */
public class NodeEnvironment implements NetworkMessageListener, PeerSetChangedListener {

    private static final Logger LOGGER = Logger.getLogger(NodeEnvironment.class.getName());

    /*
     * Timers
     */
    private Timer mainLoopTimer;

    /*
     * Data holders
     */
    private int servicePort;
    private int loopInterval;
    private int updatePeerInterval;
    private Blockchain blockchain;
    private ChainUTXODatabase chainUTXODatabase;
    private TransactionPool transactionPool;
    private Queue<Runnable> messageQueue;
    private final List<NotificationListener> notificationListeners;
    private final List<NetworkMessageListener> networkMessageListeners;
    private final List<ReorganizeChainListener> reorganizeChainListeners;
    private final int maxSequentialOrphanBlocks;
    private int sequentialOrphanBlockCount = 0;
    private SortedSet<NetworkMessage> networkMessages;
    private AtomicBoolean isUpdatingBlockchain;

    /*
     * Processors
     */
    private BlockProcessor blockProcessor;
    private PeerProcessor peerProcessor;
    private TransactionProcessor transactionProcessor;

    /**
     * Construct a new node environment.
     *
     * @param state
     *     The state for the node.
     */
    public NodeEnvironment(@NotNull State state) {
        this.servicePort = state.getConfig().getServicePort();
        this.loopInterval = state.getConfig().getLoopInterval();
        this.updatePeerInterval = state.getConfig().getUpdatePeerInterval();
        this.blockchain = state.getBlockchain();
        this.chainUTXODatabase = state.getChainUTXODatabase();
        this.blockProcessor = state.getBlockProcessor();
        this.peerProcessor = state.getPeerProcessor();
        this.transactionPool = state.getTransactionPool();
        this.transactionProcessor = state.getTransactionProcessor();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.maxSequentialOrphanBlocks = state.getConfig().getMaxSequentialOrphanBlocks();
        notificationListeners = new ArrayList<>();
        networkMessageListeners = new ArrayList<>();
        reorganizeChainListeners = new ArrayList<>();
        isUpdatingBlockchain = new AtomicBoolean(false);
        networkMessages = new ConcurrentSkipListSet<>(NetworkMessage::compareTo);

        peerProcessor.addPeerSetChangedListener(this);
        networkMessageListeners.add(this.peerProcessor);
    }

    /**
     * Setup the environment.
     * This instantiates the following:
     * <ol>
     * <li>Bootstrapping process of peers.</li>
     * <li>Start the main loop.</li>
     * <li>Initiate blockchain update.</li>
     * <li>Initiate transaction pool population.</li>
     * </ol>
     */
    public void setup() {
        LOGGER.info("Setting up the node environment.");
        peerProcessor.bootstrap();
        LOGGER.info("Environment setup done.");

        mainLoopTimer = new Timer();
        mainLoopTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                main();
            }
        }, 0, loopInterval);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(
            peerProcessor::updatePeers,
            0,
            updatePeerInterval,
            TimeUnit.SECONDS
        );

        seekTransactionPoolRequest();
    }

    /**
     * Gracefully shutdown the node environment, cancelling the main loop timer.
     */
    public void stop() {
        mainLoopTimer.cancel();

        peerProcessor.shutdownPeers();
    }

    /**
     * The main execution logic, ran every {@link BraboConfig#loopInterval} millisecond by the
     * Timer {@link #mainLoopTimer}.
     */
    private void main() {
        while (!messageQueue.isEmpty()) {
            try {
                messageQueue.remove().run();
            }
            catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    LOGGER.finest("Peer did not respond or is unavailable for request.");
                }
                else {
                    LOGGER.log(Level.WARNING, e, () -> "Peer request failed.");
                }
            }
            catch (Exception e) {
                LOGGER.log(Level.SEVERE, e, () -> "Exception in message queue loop.");
            }
        }
    }

    /**
     * Update the blockchain, requesting the top heights of all peers.
     * Also call {@link #seekBlockchainRequest} on the peer with the longest chain, if the chain
     * is longer than the current chain.
     */
    private synchronized void updateBlockchain() {
        Set<Peer> peers = getPeers();
        updateBlockchain(peers.toArray(new Peer[0]));
    }

    /**
     * Update the blockchain, requesting the top heights of the given peers.
     * Also call {@link #seekBlockchainRequest} on the peer with the longest chain, if the chain
     * is longer than the current chain.
     */
    private synchronized void updateBlockchain(Peer... peers) {
        LOGGER.info("Update blockchain.");
        Map<Peer, Integer> topBlockHeights = discoverTopBlockHeightRequest(peers);
        Optional<Map.Entry<Peer, Integer>> maxHeightEntryOptional = topBlockHeights
            .entrySet()
            .stream()
            .max(Comparator.comparing(Map.Entry::getValue));

        if (!maxHeightEntryOptional.isPresent()) {
            LOGGER.warning("Could not retrieve max height from peers, no entry found.");
            return;
        }

        Peer maxHeightPeer = maxHeightEntryOptional.get().getKey();
        Integer maxHeight = maxHeightEntryOptional.get().getValue();
        LOGGER.log(
            Level.FINE,
            () -> MessageFormat.format(
                "Max height peer found: {0} at value {1}",
                maxHeightPeer,
                maxHeight
            )
        );

        if (maxHeight > blockchain.getMainChain().getHeight()) {
            isUpdatingBlockchain.set(true);
            reorganizeChainListeners.forEach(ReorganizeChainListener::onStartOrganization);

            // Peer has a longer chain, need to update.
            Hash matchingBlockHash = checkChainCompatibleRequest(maxHeightPeer);

            seekBlockchainRequest(maxHeightPeer, matchingBlockHash);

            reorganizeChainListeners.forEach(ReorganizeChainListener::onFinishOrganization);
            isUpdatingBlockchain.set(false);
        }
    }


    //================================================================================
    // Peer management
    //================================================================================

    /**
     * Attempts to add the client peer to the list of peers.
     * Used when a client handshakes with this node's service.
     *
     * @param address
     *     The address of the client.
     * @param port
     *     The port of the Client
     */
    public void addClientPeer(InetAddress address, int port) {
        if (getPeers().stream()
            .anyMatch(p -> p.getAddress().equals(address) && p.getPort() == port)) {
            return;
        }

        Peer clientPeer;
        try {
            clientPeer = new Peer(address, port);
        }
        catch (MalformedSocketException e) {
            LOGGER.log(Level.WARNING, "Could not add client peer: {0}", e.getMessage());
            return;
        }

        peerProcessor.addPeer(clientPeer);
    }

    /**
     * Get all peers matching this client address
     *
     * @param clientAddress
     *     The address to match.
     * @return The list of peers matching the address.
     */
    public List<Peer> findClientPeers(InetAddress clientAddress) {
        return peerProcessor.findClientPeers(clientAddress);
    }

    /**
     * Propagate a blocking stub consuming lambda expression to all known peers.
     *
     * @param blockingStubConsumer
     *     The lambda expression evaluated on all peer's blocking stubs.
     */
    private void propagateMessageBlocking(
        Consumer<NodeGrpc.NodeBlockingStub> blockingStubConsumer) {
        for (NodeGrpc.NodeBlockingStub stub : getPeerStubs()) {
            try {
                blockingStubConsumer.accept(stub);
            }
            catch (StatusRuntimeException e) {
                LOGGER.log(
                    Level.SEVERE,
                    MessageFormat.format("Exception while propagating message: {0}", e.getMessage())
                );
            }
        }

        LOGGER.info("Message propagated to all peers.");
    }

    public void addNetworkMessageListener(NetworkMessageListener listener) {
        networkMessageListeners.add(listener);
    }

    public void removeNetworkMessageListener(NetworkMessageListener listener) {
        networkMessageListeners.remove(listener);
    }

    public void addReorganizeChainListener(ReorganizeChainListener listener) {
        reorganizeChainListeners.add(listener);
    }

    public void removeReorganizeChainListener(ReorganizeChainListener listener) {
        reorganizeChainListeners.remove(listener);
    }

    public SortedSet<NetworkMessage> getNetworkMessages() {
        return Collections.unmodifiableSortedSet(networkMessages);
    }

    @Override
    public void onIncomingMessage(NetworkMessage message, boolean isUpdate) {
        if (!isUpdate) {
            networkMessages.add(message);
        }
        networkMessageListeners.forEach(l -> l.onIncomingMessage(message, isUpdate));
    }

    @Override
    public void onOutgoingMessage(NetworkMessage message, boolean isUpdate) {
        if (!isUpdate) {
            networkMessages.add(message);
        }
        networkMessageListeners.forEach(l -> l.onOutgoingMessage(message, isUpdate));
    }

    @Override
    public void onPeerAdded(Peer peer) {
        peer.addNetworkMessageListener(this);
        networkMessageListeners.forEach(l -> l.onOutgoingMessage(null, false));

        // Sync with this peer
        messageQueue.add(() -> updateBlockchain(peer));
    }

    @Override
    public void onPeerRemoved(Peer peer) {
        peer.removeNetworkMessageListeners(this);
    }

    //================================================================================
    // Callbacks
    //================================================================================

    /**
     * Callback on receival of block after a {@code getBlocks} message.
     *
     * @param block
     *     The received block
     * @param peers
     *     The peers for which we request the parent block, if needed.
     * @param propagate
     *     Whether or not to propagate the message to peers.
     * @return Whether the block is either valid or orphan.
     */
    private boolean onReceiveBlock(Block block, List<Peer> peers, boolean propagate) {
        if (propagate) {
            notificationListeners.forEach(l -> l.receivedBlock(block));
        }

        try {
            LOGGER.info("Received new block from peer.");
            BlockValidationResult processedBlockStatus = blockProcessor.processNewBlock(
                block,
                false
            );
            LOGGER.log(
                Level.FINEST,
                () -> MessageFormat.format("Processed new block: {0}", processedBlockStatus)
            );

            if (processedBlockStatus.getStatus() == ValidationStatus.VALID) {
                sequentialOrphanBlockCount = 0;
            }

            switch (processedBlockStatus.getStatus()) {
                case ORPHAN:
                    sequentialOrphanBlockCount++;
                    if (sequentialOrphanBlockCount > maxSequentialOrphanBlocks) {
                        sequentialOrphanBlockCount = 0;
                        messageQueue.add(this::updateBlockchain);
                    }
                    else {
                        messageQueue.add(() -> getBlocksRequest(
                            Collections.singletonList(block.getPreviousBlockHash()),
                            peers,
                            false
                        ));
                    }
                    // Fall-through intended
                case VALID:
                    if (propagate) {
                        final BrabocoinProtos.Hash protoBlockHash = ProtoConverter.toProto(
                            block.getHash(),
                            BrabocoinProtos.Hash.class
                        );
                        // TODO: We actually want to use async stub here, but that went wrong
                        // before (Cancelled exception by GRPC).
                        messageQueue.add(() -> propagateMessageBlocking(
                            s -> s.announceBlock(protoBlockHash))
                        );
                    }
                    return true;
                case INVALID:
                    LOGGER.log(Level.FINE, "Block invalid.");

                    // Block already stored is not a trigger to fail.
                    RuleBookFailMarker failMarker = processedBlockStatus.getFailMarker();
                    if (failMarker != null && failMarker.getFailedRule()
                        .equals(DuplicateStorageBlkRule.class)) {
                        return true;
                    }

                    return false;
            }
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Could not store received block: {0}", e.getMessage());
        }

        return false;
    }

    /**
     * Handles the receival of a new block.
     * Tries to store the block in the blockchain.
     *
     * @param blockHash
     *     Hash of the new block.
     * @param peers
     *     The peers for which we request the block, if needed.
     */
    public void onReceiveBlockHash(@NotNull Hash blockHash,
                                   @NotNull List<Peer> peers) {
        LOGGER.fine("Block hash received.");
        LOGGER.log(
            Level.FINEST,
            () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(blockHash.getValue()))
        );

        try {
            if (blockchain.isBlockStored(blockHash)) {
                LOGGER.log(Level.FINE, "Block was already stored, ignoring.");
                return;
            }

            messageQueue.add(() -> getBlocksRequest(
                Collections.singletonList(blockHash),
                peers,
                true
            ));
        }
        catch (DatabaseException e) {
            LOGGER.log(
                Level.SEVERE,
                "Could not check if block was already stored: {0}",
                e.getMessage()
            );
        }
    }

    /**
     * Handles the receival of a new transaction.
     * Checks whether the transaction is already processed, and if not requests the transaction
     * from the peers.
     *
     * @param transactionHash
     *     Hash of the new block.
     * @param peers
     *     The peers to request the transaction from.
     */
    public void onReceiveTransactionHash(@NotNull Hash transactionHash,
                                         List<Peer> peers) {
        LOGGER.fine("Transaction hash received.");
        LOGGER.log(
            Level.FINEST,
            () -> MessageFormat.format(
                "Hash: {0}",
                ByteUtil.toHexString(transactionHash.getValue())
            )
        );

        if (transactionPool.contains(transactionHash)) {
            LOGGER.log(Level.FINE, "Transaction was already processed, ignoring.");
            return;
        }

        messageQueue.add(() -> getTransactionRequest(
            Collections.singletonList(transactionHash),
            peers,
            true
        ));
    }

    /**
     * Callback on receival of transaction after a {@code getTransactions} message.
     *
     * @param transaction
     *     The received transaction
     * @param propagate
     *     Whether or not to propagate the message to peers.
     */
    private void onReceiveTransaction(Transaction transaction, boolean propagate) {
        if (propagate) {
            notificationListeners.forEach(l -> l.receivedTransaction(transaction));
        }

        try {
            ProcessedTransactionResult result = transactionProcessor.processNewTransaction(
                transaction
            );

            switch (result.getStatus()) {
                case VALID:
                    if (propagate) {
                        final BrabocoinProtos.Hash protoTransactionHash = ProtoConverter.toProto(
                            transaction.getHash(),
                            BrabocoinProtos.Hash.class
                        );
                        // TODO: We actually want to use async stub here, but that went wrong
                        // before (Cancelled exception by GRPC).
                        messageQueue.add(() -> propagateMessageBlocking(s -> s.announceTransaction(
                            protoTransactionHash)));
                    }
                    break;

                case ORPHAN:
                case INVALID:
                    LOGGER.log(Level.FINE, "Transaction invalid or orphan.");
                    break;
            }

            if (propagate) {
                // Propagate any remaining transactions that became valid.
                for (Transaction t : result.getValidatedOrphans()) {
                    final BrabocoinProtos.Hash protoTransactionHash = ProtoConverter.toProto(
                        t.getHash(),
                        BrabocoinProtos.Hash.class
                    );
                    messageQueue.add(() -> propagateMessageBlocking(
                        s -> s.announceTransaction(protoTransactionHash))
                    );
                }
            }
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Could not store received block: {0}", e.getMessage());
        }

    }


    //================================================================================
    // Requests
    //================================================================================

    /**
     * Default non blocking request for blocks.
     *
     * @param hashes
     *     The list of block hashes to fetch.
     * @param peers
     *     The list of peers used to request the blocks.
     * @param propagate
     *     Whether or not to propagate an announce message to all peers.
     */
    public void getBlocksRequest(List<Hash> hashes, List<Peer> peers,
                                 boolean propagate) {
        try {
            getBlocksRequest(hashes, peers, propagate, false);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            LOGGER.severe("Interrupted exception while calling a non-blocking request for blocks");
        }
    }

    /**
     * Tries to acquire the blocks from the given peers using the {@code getBlocks} message,
     * given the block hashes.
     * Also propagates an announce block message if {@code propagate} is set to true.
     *
     * @param hashes
     *     The list of block hashes to fetch.
     * @param peers
     *     The list of peers used to request the blocks.
     * @param propagate
     *     Whether or not to propagate an announce message to all peers.
     * @param blocking
     *     Whether or not to wait for the blocks to be received.
     */
    public void getBlocksRequest(List<Hash> hashes, List<Peer> peers,
                                 boolean propagate,
                                 boolean blocking) throws InterruptedException {
        LOGGER.info("Getting a list of blocks from peers.");

        final CountDownLatch latch = new CountDownLatch(peers.size());

        for (Peer peer : peers) {
            Context.CancellableContext cancellableContext = Context.current().withCancellation();

            StreamObserver<BrabocoinProtos.Block> blockStreamObserver =
                new StreamObserver<BrabocoinProtos.Block>() {
                    @Override
                    public void onNext(BrabocoinProtos.Block value) {
                        LOGGER.log(Level.FINEST, () -> {
                            try {
                                return MessageFormat.format(
                                    "Received peer block: {0}",
                                    JsonFormat.printer().print(value)
                                );
                            }
                            catch (InvalidProtocolBufferException e) {
                                LOGGER.log(
                                    Level.WARNING,
                                    "Could not log the JSON format of the response message.",
                                    e
                                );
                            }

                            return "";
                        });
                        Block receivedBlock = ProtoConverter.toDomain(
                            value,
                            Block.Builder.class
                        );
                        if (receivedBlock == null) {
                            LOGGER.log(
                                Level.SEVERE,
                                "Protobuf parsing of received block failed."
                            );
                            return;
                        }
                        if (hashes.contains(receivedBlock.getHash())) {
                            if (!onReceiveBlock(receivedBlock, peers, propagate)) {
                                cancellableContext.cancel(new Throwable(
                                    "Invalid block received, cancelling context."));
                            }
                        }
                        else {
                            LOGGER.log(
                                Level.WARNING,
                                "Peer sent block that was not requested"
                            );
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOGGER.log(
                            Level.WARNING,
                            "Peer returned an error while getting block: {0}",
                            t.getMessage()
                        );
                        latch.countDown();
                        cancellableContext.close();
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.log(Level.FINE, "Peer block stream completed.");
                        latch.countDown();
                        cancellableContext.close();
                    }
                };


            cancellableContext.run(() -> {
                StreamObserver<BrabocoinProtos.Hash> hashStreamObserver = peer.getAsyncStub()
                    .getBlocks(blockStreamObserver);

                for (Hash hash : hashes) {
                    BrabocoinProtos.Hash protoBlockHash = ProtoConverter.toProto(
                        hash,
                        BrabocoinProtos.Hash.class
                    );
                    hashStreamObserver.onNext(protoBlockHash);
                }
                hashStreamObserver.onCompleted();
            });

            if (blocking) {
                latch.await();
            }
        }
    }

    /**
     * Announce the block's hash to all known peers.
     *
     * @param block
     *     The block to announce.
     */
    public void announceBlockRequest(Block block) {
        LOGGER.info("Announcing block to peers.");
        Hash blockHash = block.getHash();
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(blockHash.getValue()));
        BrabocoinProtos.Hash protoBlockHash = ProtoConverter.toProto(
            blockHash,
            BrabocoinProtos.Hash.class
        );

        for (Peer peer : getPeers()) {
            peer.getAsyncStub().announceBlock(protoBlockHash, new StreamObserver<Empty>() {
                @Override
                public void onNext(Empty value) {
                    LOGGER.log(Level.FINEST, "Received block announce response from peer.");
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(
                        Level.WARNING,
                        "Received block announce error from peer: {0}",
                        t.getMessage()
                    );
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
     * @param transaction
     *     The transaction to announce.
     */
    public void announceTransactionRequest(Transaction transaction) {
        LOGGER.info("Announcing transaction to peers.");
        Hash transactionHash = transaction.getHash();
        LOGGER.log(Level.FINEST, "Hash: {0}", ByteUtil.toHexString(transactionHash.getValue()));
        BrabocoinProtos.Hash protoTransactionHash = ProtoConverter.toProto(
            transactionHash,
            BrabocoinProtos.Hash.class
        );

        for (Peer peer : getPeers()) {
            peer.getAsyncStub()
                .announceTransaction(protoTransactionHash, new StreamObserver<Empty>() {
                    @Override
                    public void onNext(Empty value) {
                        LOGGER.log(
                            Level.FINEST,
                            "Received transaction announce response from peer."
                        );
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOGGER.log(
                            Level.WARNING,
                            "Received transaction announce error from peer: {0}",
                            t.getMessage()
                        );
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.log(
                            Level.FINEST,
                            "Received transaction announce completion from peer."
                        );
                    }
                });
        }
    }

    /**
     * Requests any blocks that are mined on top of its current top block.
     * <p>
     * The peer looks up the block corresponding to the received block hash in its main chain.
     * It then returns all the hashes of the blocks on top of this block. Note that only
     * blocks on
     * the main chain are returned. If the requested block is not on the main chain, the node
     * will return no
     * hashes.
     *
     * @param peer
     *     The peer used to seek the blockchain.
     * @param startingHash
     *     The hash to start from.
     */
    public void seekBlockchainRequest(Peer peer, Hash startingHash) {
        LOGGER.info("Seek blockchain request.");
        Iterator<BrabocoinProtos.Hash> hashesAbove = peer.getBlockingStub()
            .seekBlockchain(ProtoConverter.toProto(startingHash, BrabocoinProtos.Hash.class));

        List<Hash> hashes = new ArrayList<>();
        hashesAbove.forEachRemaining(
            h -> hashes.add(ProtoConverter.toDomain(h, Hash.Builder.class))
        );

        if (hashes.size() > 0) {
            try {
                getBlocksRequest(
                    hashes,
                    Collections.singletonList(peer),
                    false,
                    true
                );
            }
            catch (InterruptedException e) {
                LOGGER.severe("Could not get blocks for blockchain seeking");
            }
        }
    }

    /**
     * Request peers for the top block height.
     *
     * @return A map of peer matching its block height.
     */
    public Map<Peer, Integer> discoverTopBlockHeightRequest(Peer... peers) {
        LOGGER.fine("Requesting peers for top block height.");

        Map<Peer, Integer> peerHeights = new HashMap<>();

        for (Peer peer : peers) {
            BrabocoinProtos.BlockHeight protoBlockHeight = peer.getBlockingStub()
                .discoverTopBlockHeight(Empty.newBuilder().build());
            LOGGER.log(
                Level.FINEST,
                () -> MessageFormat.format(
                    "Received block height from peer ( {0} ): {1}",
                    peer,
                    protoBlockHeight.getHeight()
                )
            );
            peerHeights.put(peer, protoBlockHeight.getHeight());
        }

        return peerHeights;
    }

    /**
     * The hash of the current top block is sent to the peer.
     * If this hash is present in the main chain of the receiving peer, this peer responds with
     * true, and false otherwise.
     * <p>
     * When the node receives a reply that the block is not present in their main chain,
     * the node will perform an exponential backoff until a hash is found that matches the
     * peer's
     * main chain.
     * <p>
     * If no such block is found and we reach the genesis block, the hash of the genesis
     * block is
     * returned.
     *
     * @param peer
     *     The peer to check chain compatibility with.
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
            }
            else {
                currentBlockHash = blockchain.getMainChain()
                    .getBlockAtHeight(nextHeight)
                    .getHash();
            }

            depth = depth == 0 ? 1 : depth * 2;

            Hash finalCurrentBlockHash = currentBlockHash;
            int finalDepth = depth;
            LOGGER.log(
                Level.FINEST,
                () -> MessageFormat.format(
                    "Checking block hash at depth {0} : {1}",
                    finalDepth,
                    ByteUtil.toHexString(finalCurrentBlockHash.getValue())
                )
            );

            BrabocoinProtos.ChainCompatibility chainCompatibility = peer.getBlockingStub()
                .checkChainCompatible(
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
     * @param hashes
     *     The hashes to request.
     * @param peers
     *     The peers to request transactions from.
     * @param propagate
     *     Whether or not to propagate.
     */
    public void getTransactionRequest(List<Hash> hashes, List<Peer> peers,
                                      boolean propagate) {
        LOGGER.info("Getting a list of transactions from peers.");

        for (Peer peer : peers) {
            // TODO: Check if async processing is done correctly.
            StreamObserver<BrabocoinProtos.Hash> hashStreamObserver = peer.getAsyncStub()
                .getTransactions(new StreamObserver<BrabocoinProtos.Transaction>() {
                    @Override
                    public void onNext(BrabocoinProtos.Transaction value) {
                        LOGGER.log(Level.FINEST, () -> {
                            try {
                                return MessageFormat.format(
                                    "Received peer transaction: {0}",
                                    JsonFormat.printer().print(value)
                                );
                            }
                            catch (InvalidProtocolBufferException e) {
                                LOGGER.log(
                                    Level.WARNING,
                                    "Could not log the JSON format of the response message.",
                                    e
                                );
                            }

                            return "";
                        });
                        Transaction transaction = ProtoConverter.toDomain(
                            value,
                            Transaction.Builder.class
                        );
                        if (transaction == null) {
                            LOGGER.log(
                                Level.SEVERE,
                                "Protobuf parsing of received transaction failed."
                            );
                            return;
                        }
                        if (hashes.contains(transaction.getHash())) {
                            onReceiveTransaction(transaction, propagate);
                        }
                        else {
                            LOGGER.log(
                                Level.WARNING,
                                "Peer sent transaction that was not requested"
                            );
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        LOGGER.log(
                            Level.WARNING,
                            "Peer returned an error while getting transaction: {0}",
                            t.getMessage()
                        );
                        t.getCause().printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        LOGGER.log(Level.FINE, "Peer transaction stream completed.");
                    }
                });

            for (Hash hash : hashes) {
                BrabocoinProtos.Hash protoBlockHash = ProtoConverter.toProto(
                    hash,
                    BrabocoinProtos.Hash.class
                );
                hashStreamObserver.onNext(protoBlockHash);
            }
            hashStreamObserver.onCompleted();
        }
    }


    /**
     * Requests the transaction pool hashes on each peer.
     * Also calls {@link #getTransactionRequest} for all gathered hashes to all peers.
     */
    public void seekTransactionPoolRequest() {
        LOGGER.info("Seek transaction pool request.");
        List<Hash> hashes = new ArrayList<>();
        for (Peer peer : getPeers()) {
            Iterator<BrabocoinProtos.Hash> transactionHashes = peer
                .getBlockingStub()
                .seekTransactionPool(Empty.newBuilder().build());

            transactionHashes.forEachRemaining(
                h -> hashes.add(ProtoConverter.toDomain(h, Hash.Builder.class))
            );
        }
        if (hashes.size() > 0) {
            messageQueue.add(() -> getTransactionRequest(
                hashes,
                new ArrayList<>(getPeers()),
                false
            ));
        }
    }

    /**
     * Process the newly mined block and announce the block to all peers, if valid.
     *
     * @param block
     *     The newly mined block.
     * @return The validation status of the processed block.
     * @throws DatabaseException
     *     If the block could not be processed.
     */
    public ValidationStatus processNewlyMinedBlock(
        @NotNull Block block) throws DatabaseException {
        ValidationStatus status = blockProcessor.processNewBlock(block, true).getStatus();
        if (status == ValidationStatus.VALID) {
            announceBlockRequest(block);
        }

        return status;
    }

    public ValidationStatus processNewlyCreatedTransaction(
        @NotNull Transaction transaction) {
        ValidationStatus status;
        try {
            status = transactionProcessor.processNewTransaction(transaction).getStatus();
        }
        catch (DatabaseException e) {
            status = ValidationStatus.INVALID;
        }

        messageQueue.add(() -> announceTransactionRequest(transaction));

        return status;
    }


    //================================================================================
    // Getters
    //================================================================================

    /**
     * Get whether the environment is updating the blockchain
     *
     * @return Whether or not the blockchain is updating.
     */
    public boolean isUpdatingBlockchain() {
        return isUpdatingBlockchain.get();
    }

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
     * Gets the block from the blockchain.
     *
     * @param blockHash
     *     Hash of the block to get.
     * @return Block instance or null if not found.
     */
    public Block getBlock(@NotNull Hash blockHash) {
        LOGGER.fine("Block requested by hash.");
        LOGGER.log(
            Level.FINEST,
            () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(blockHash.getValue()))
        );
        try {
            Block block = blockchain.getBlock(blockHash);
            if (block != null) {
                return block;
            }

            Block orphanBlock = blockchain.getOrphan(blockHash);
            if (orphanBlock != null) {
                return orphanBlock;
            }

            // Propagate rejected blocks if the client asked for it
            Optional<RejectedBlock> rejectedBlock = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                    blockchain.recentRejectsIterator(),
                    Spliterator.ORDERED
                ), false)
                .filter(b -> b.getBlock().getHash().equals(blockHash))
                .findFirst();

            return rejectedBlock.map(RejectedBlock::getBlock).orElse(null);
        }
        catch (DatabaseException e) {
            LOGGER.log(
                Level.SEVERE,
                "Database error while trying to acquire block, error: {0}",
                e.getMessage()
            );
            return null;
        }
    }

    /**
     * Gets the block hashes above a given block hash.
     *
     * @param blockHash
     *     The block hash to indicate the starting position.
     * @return List over block hashes above the given block or null if not found on the main
     * chain or the block is not found.
     */
    public List<Hash> getBlocksAbove(Hash blockHash) {
        LOGGER.fine("Block hashes requested above a given block hash.");
        LOGGER.log(
            Level.FINEST,
            () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(blockHash.getValue()))
        );

        try {
            IndexedBlock indexedBlock = blockchain.getIndexedBlock(blockHash);

            if (indexedBlock == null) {
                return Collections.emptyList();
            }

            boolean inMainChain = blockchain.getMainChain().contains(indexedBlock);

            if (!inMainChain) {
                return Collections.emptyList();
            }

            List<Hash> blocksAbove = new ArrayList<>();

            while (indexedBlock != null) {
                indexedBlock = blockchain.getMainChain().getNextBlock(indexedBlock);
                if (indexedBlock != null) {
                    blocksAbove.add(indexedBlock.getHash());
                }
            }

            return blocksAbove;
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Indexed block not available: {0}", e.getMessage());
        }

        return Collections.emptyList();
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
     * Gets the validated transaction by the given transaction hash.
     *
     * @param transactionHash
     *     Hash of the transaction to get.
     * @return Transaction instance or null if not found.
     */
    public Transaction getTransaction(@NotNull Hash transactionHash) {
        LOGGER.fine("Transaction requested by hash.");
        LOGGER.log(
            Level.FINEST,
            () -> MessageFormat.format(
                "Hash: {0}",
                ByteUtil.toHexString(transactionHash.getValue())
            )
        );

        Transaction validTx = transactionPool.findValidatedTransaction(transactionHash);
        if (validTx != null) {
            return validTx;
        }

        Optional<RejectedTransaction> recentlyRejectedTx = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                transactionPool.recentRejectsIterator(),
                Spliterator.ORDERED
            ), false)
            .filter(t -> t.getTransaction().getHash().equals(transactionHash))
            .findFirst();

        return recentlyRejectedTx.map(RejectedTransaction::getTransaction).orElse(null);
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

    /**
     * Check whether the given block hash is contained in the main chain.
     *
     * @param blockHash
     *     Hash of the block to check.
     * @return True if it is contained in the main chain.
     */
    public boolean isChainCompatible(@NotNull Hash blockHash) {
        LOGGER.fine("Chain compatibility check requested for block hash.");
        LOGGER.log(
            Level.FINEST,
            () -> MessageFormat.format("Hash: {0}", ByteUtil.toHexString(blockHash.getValue()))
        );

        IndexedBlock indexedBlock = null;
        try {
            indexedBlock = blockchain.getIndexedBlock(blockHash);
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Indexed block not available: {0}", e.getMessage());
        }

        if (indexedBlock == null) {
            LOGGER.log(Level.FINEST, "Hash not found");
            return false;
        }

        final boolean contains = blockchain.getMainChain().contains(indexedBlock);

        LOGGER.log(
            Level.FINEST,
            () -> MessageFormat.format("Hash contained in main chain: {0}", contains)
        );

        return contains;
    }

    /**
     * Get the blocking stubs of all peers.
     *
     * @return List of blocking stubs.
     */
    private List<NodeGrpc.NodeBlockingStub> getPeerStubs() {
        return getPeers().stream().map(Peer::getBlockingStub).collect(Collectors.toList());
    }

    public void addNotificationListener(NotificationListener notificationListener) {
        this.notificationListeners.add(notificationListener);
    }

    public void removeNotificationListener(NotificationListener notificationListener) {
        this.notificationListeners.remove(notificationListener);
    }

    public void addMessageQueueEvent(Consumer<NodeEnvironment> event) {
        this.messageQueue.add(() -> event.accept(this));
    }
}

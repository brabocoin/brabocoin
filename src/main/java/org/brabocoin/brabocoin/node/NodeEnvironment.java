package org.brabocoin.brabocoin.node;

import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.dal.BlockDatabase;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.dal.LevelDB;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.messages.HandshakeResponse;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.node.config.BraboConfigProvider;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Represents a node environment.
 */
public class NodeEnvironment {
    protected BraboConfig config;

    private BlockDatabase database;
    private Set<Peer> peers = new HashSet<>();
    private Converter converter = Converter.create();
    private Map<Hash, Transaction> transactionPool = new HashMap<>();

    public NodeEnvironment() throws DatabaseException {
        config = new BraboConfigProvider().getConfig().bind("brabo", BraboConfig.class);

        // Create blockStore directory if not exists
        File blockStoreDirectory = new File(config.blockStoreDirectory());
        if (!blockStoreDirectory.exists()){
            blockStoreDirectory.mkdirs();
        }

        database = new BlockDatabase(createKeyValueStorage(), blockStoreDirectory);
    }

    public NodeEnvironment(BlockDatabase database) throws DatabaseException {
        this();
        this.database = database;
    }

    public NodeEnvironment(Map<Hash, Transaction> transactionPool) throws DatabaseException {
        this();
        this.transactionPool = transactionPool;
    }

    protected KeyValueStore createKeyValueStorage() {
        return new LevelDB(new File(config.databaseDirectory()));
    }

    /**
     * Adds a peer to the list of peers known to this node.
     *
     * @param peer The peer to add to the peer list known to this node.
     */
    public void addPeer(Peer peer) {
        peers.add(peer);
    }

    /**
     * Adds a list of peers to the list of peers known to this node.
     *
     * @param peer The peers to add to the peer list known to this node.
     */
    public void addPeers(List<Peer> peer) {
        peers.addAll(peer);
    }

    /**
     * Get a copy of the list of peers.
     *
     * @return List of peers.
     */
    public List<Peer> getPeers() {
        return new ArrayList<>(peers);
    }

    /**
     * Setup the environment, loading the config and bootstrapping peers.
     */
    public void setup() {
        bootstrap();
    }

    /**
     * Initializes the set of peers.
     */
    private void instantiateBootstrapPeers() {
        for (final String peerSocket : config.bootstrapPeers()) {
            try {
                peers.add(new Peer(peerSocket));
            } catch (MalformedSocketException e) {
                // TODO: Handle invalid peer socket representation in the config.
                // Exit throwing an error to the user or skip this peer?
                // Definitely log this.
            }
        }
    }

    /**
     * Handles the receival of a new block.
     * TODO: Describe logic
     *
     * @param blockHash Hash of the new block.
     */
    public void onReceiveBlockHash(@NotNull Hash blockHash) {
        // TODO: Implement and log
    }

    /**
     * Handles the receival of a new transaction.
     * TODO: Describe logic
     *
     * @param transactionHash Hash of the new block.
     */
    public void onReceiveTransaction(@NotNull Hash transactionHash) {
        // TODO: Implement and log
    }

    /**
     * Handles the request for a block.
     * TODO: Describe logic
     *
     * @param blockHash Hash of the block to get.
     * @return Block instance or null if not found.
     */
    public Block getBlock(@NotNull Hash blockHash) {
        try {
            return database.findBlock(blockHash);
        } catch (DatabaseException e) {
            // TODO: Log
            return null;
        }
    }

    /**
     * Handles the request for a transaction.
     * TODO: Describe logic
     *
     * @param transactionHash Hash of the transaction to get.
     * @return Transaction instance or null if not found.
     */
    public Transaction getTransaction(@NotNull Hash transactionHash) {
        return null;
    }

    /**
     * Get an iterator for all transaction hashes in the transaction pool.
     *
     * @return Transaction hash iterator.
     */
    public Iterator<Hash> getTransactionIterator() {
        return transactionPool.keySet().iterator();
    }

    /**
     * Gets the height of the top block.
     *
     * @return Top block height.
     */
    public long getTopBlockHeight() {
        // TODO: magic here
        return 0;
    }

    /**
     * Check whether the given block hash is contained in the main chain.
     *
     * @param blockHash Hash of the block to check.
     * @return True if it is contained in the main chain.
     */
    public boolean isChainCompatible(@NotNull Hash blockHash) {
        // TODO: Magic here
        return false;
    }

    /**
     * Gets the block hashes above a given block hash.
     *
     * @param blockHash The block hash to indicate the starting position.
     * @return Iterator over block hashes above the given block.
     */
    public Iterator<Hash> getBlocksAbove(Hash blockHash) {
        return new Iterator<Hash>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Hash next() {
                return null;
            }
        };
    }

    /**
     * TODO: Create JavaDoc
     */
    private void bootstrap() {
        // Populate bootstrap peers
        instantiateBootstrapPeers();
        // A list of peers for which we need to do a handshake
        List<Peer> handshakePeers = new ArrayList<>(getPeers());

        if (handshakePeers.size() <= 0) {
            // TODO: Log to user, we can not bootstrap without any bootstrap peers
            return;
        }

        while (getPeers().size() < config.targetPeerCount() && handshakePeers.size() > 0) {
            Peer handshakePeer = handshakePeers.remove(0);
            try {
                // Perform a handshake with the peer
                BrabocoinProtos.HandshakeResponse protoResponse = handshakePeer.blockingStub
                        .withDeadlineAfter(config.bootstrapDeadline(), TimeUnit.MILLISECONDS)
                        .handshake(Empty.newBuilder().build());
                HandshakeResponse response = converter.toDomain(HandshakeResponse.Builder.class, protoResponse).createHandshakeResponse();

                // We got a response from the current handshake peer, register this peer as valid
                addPeer(handshakePeer);

                // Add the discovered peers to the list of handshake peers
                for (final String peerSocket : response.getPeers()) {
                    try {
                        final Peer discoveredPeer = new Peer(peerSocket);
                        handshakePeers.add(discoveredPeer);
                    } catch (final Exception e) {
                        // Bootstrap peer returned a malformed or invalid peer
                    }
                }
            } catch (StatusRuntimeException e) {
                // TODO: handle peer errors on handshake, log
            }
        }

        // TODO: Check whether the bootstrap peers were valid.
    }
}

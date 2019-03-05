package org.brabocoin.brabocoin.testutil;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.dal.HashMapDB;
import org.brabocoin.brabocoin.dal.KeyValueStore;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.node.Peer;
import org.brabocoin.brabocoin.node.state.DeploymentState;
import org.brabocoin.brabocoin.processor.PeerProcessor;
import org.brabocoin.brabocoin.util.BigIntegerUtil;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.validation.consensus.Consensus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;


/**
 * Test state environment.
 */
public class TestState extends DeploymentState {

    public TestState(@NotNull MockLegacyConfig config) throws DatabaseException {
        this(config, new Consensus());
    }

    public TestState(@NotNull MockLegacyConfig config, Consensus consensus) throws DatabaseException {
        super(
            config.toBraboConfig(),
            new Consensus(consensus) {
                @Override
                public Hash getTargetValue() {
                    return new Hash(ByteString.copyFrom(
                        BigIntegerUtil.getMaxBigInteger(33).toByteArray()));
                }
            },
            (creation, creator) -> creator.apply(new Destructible<>("testpassphrase"::toCharArray))
        );
    }

    @Override
    protected KeyValueStore createBlockStorage() {
        return new HashMapDB();
    }

    @Override
    protected KeyValueStore createUtxoStorage() {
        return new HashMapDB();
    }

    @Override
    protected KeyValueStore createWalletPoolUtxoStorage() {
        return new HashMapDB();
    }

    @Override
    protected KeyValueStore createWalletChainUtxoStorage() {
        return new HashMapDB();
    }

    @Override
    protected PeerProcessor createPeerProcessor() {
        return new PeerProcessor(new HashSet<>(), config) {
            @Override
            protected synchronized boolean filterPeer(Peer peer) {
                return !(peer.isLocal() && peer.getPort() == config.getServicePort());
            }
        };
    }
}

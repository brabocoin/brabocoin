package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.BlockchainListener;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.dal.CompositeReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.TransactionPool;
import org.brabocoin.brabocoin.dal.TransactionPoolListener;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.dal.UTXOSetListener;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.exceptions.InsufficientInputException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.UnsignedTransaction;
import org.brabocoin.brabocoin.model.crypto.KeyPair;
import org.brabocoin.brabocoin.model.crypto.PrivateKey;
import org.brabocoin.brabocoin.model.crypto.Signature;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.processor.BlockProcessorListener;
import org.brabocoin.brabocoin.util.Destructible;
import org.brabocoin.brabocoin.validation.Consensus;
import org.brabocoin.brabocoin.wallet.generation.KeyGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The wallet data structure.
 */
public class Wallet implements Iterable<KeyPair>, BlockchainListener,
                               BlockProcessorListener, TransactionPoolListener {

    private static final Logger LOGGER = Logger.getLogger(Wallet.class.getName());

    /**
     * The public and private key collection.
     */
    private final @NotNull List<KeyPair> keyPairs;

    /**
     * Transaction history.
     */
    private final @NotNull TransactionHistory transactionHistory;

    /**
     * Consensus used in this wallet.
     */
    private final @NotNull Consensus consensus;

    /**
     * The signer used to sign transactions with.
     */
    private final @NotNull Signer signer;

    /**
     * The key generator used to generate private keys.
     */
    private final @NotNull KeyGenerator keyGenerator;

    /**
     * The wallet chain UTXO set.
     */
    private final @NotNull UTXODatabase walletChainUtxoSet;

    /**
     * The wallet pool UTXO set.
     */
    private final @NotNull UTXODatabase walletPoolUtxoSet;

    /**
     * The wallet composite UTXO set.
     */
    private final @NotNull ReadonlyUTXOSet walletCompositeUtxoSet;

    /**
     * The cipher used to create private keys.
     */
    private final @NotNull Cipher privateKeyCipher;

    /**
     * The inputs used in the wallet.
     */
    private final @NotNull Set<Input> usedInputs;

    /**
     * The chain UTXO set.
     */
    private final ReadonlyUTXOSet chainUtxo;

    /**
     * The pool UTXO set.
     */
    private final ReadonlyUTXOSet poolUtxo;

    private final @NotNull Blockchain blockchain;

    /**
     * Cached mining address.
     */
    private @Nullable Hash miningAddress;

    /**
     * Key pair generated listeners.
     */
    private List<KeyPairListener> keyPairListeners = new ArrayList<>();

    /**
     * BalanceListener list
     */
    private List<BalanceListener> balanceListeners = new ArrayList<>();

    /**
     * Create a wallet for a given public to private key map.
     *
     * @param keyPairs
     *     Key map
     * @param chainUtxo
     *     The chain UTXO set to watch for changes to the chain UTXO to update the wallet UTXO
     *     and used inputs.
     * @param poolUtxo
     *     The pool UTXO set to watch for changes to the pool UTXO to update the wallet UTXO
     * @param transactionPool
     */
    public Wallet(@NotNull List<KeyPair> keyPairs,
                  @NotNull TransactionHistory transactionHistory,
                  @NotNull Consensus consensus,
                  @NotNull Signer signer, @NotNull KeyGenerator keyGenerator,
                  @NotNull Cipher privateKeyCipher,
                  @NotNull UTXODatabase walletChainUtxoSet,
                  @NotNull UTXODatabase walletPoolUtxoSet,
                  @NotNull ReadonlyUTXOSet chainUtxo,
                  @NotNull ReadonlyUTXOSet poolUtxo,
                  @NotNull Blockchain blockchain,
                  @NotNull TransactionPool transactionPool) {
        this.keyPairs = new ArrayList<>(keyPairs);
        this.transactionHistory = transactionHistory;
        this.consensus = consensus;
        this.signer = signer;
        this.keyGenerator = keyGenerator;
        this.privateKeyCipher = privateKeyCipher;
        this.walletChainUtxoSet = walletChainUtxoSet;
        this.walletPoolUtxoSet = walletPoolUtxoSet;
        this.chainUtxo = chainUtxo;
        this.poolUtxo = poolUtxo;
        this.blockchain = blockchain;
        this.usedInputs = new HashSet<>();

        // Add listeners
        this.poolUtxo.addListener(new PoolListener());
        this.chainUtxo.addListener(new ChainListener());
        this.blockchain.addListener(this);
        transactionPool.addListener(this);
        this.walletCompositeUtxoSet = new CompositeReadonlyUTXOSet(
            walletChainUtxoSet,
            walletPoolUtxoSet
        );
    }

    /**
     * Get the collection of public keys.
     *
     * @return All public keys known to the wallet
     */
    public @NotNull Collection<PublicKey> getPublicKeys() {
        return keyPairs.stream().map(KeyPair::getPublicKey).collect(Collectors.toList());
    }

    /**
     * Get the collection of private keys.
     *
     * @return All private keys known to the wallet
     */
    public @NotNull Collection<PrivateKey> getPrivateKeys() {
        return keyPairs.stream().map(KeyPair::getPrivateKey).collect(Collectors.toList());
    }

    /**
     * Finds a key pair.
     *
     * @param publicKey
     *     The public key to find the key pair for
     * @return The key pair or {@code null} if not found
     */
    public @Nullable KeyPair findKeyPair(@NotNull PublicKey publicKey) {
        return keyPairs.stream().filter(p -> p.getPublicKey().equals(publicKey))
            .findFirst()
            .orElse(null);
    }

    /**
     * Check whether the wallet contains this address.
     *
     * @param address
     *     The address to find.
     * @return Whether this wallet has the key pair corresponding to this address.
     */
    public boolean hasAddress(@NotNull Hash address) {
        return keyPairs.stream().anyMatch(p -> p.getPublicKey().getHash().equals(address));
    }

    /**
     * Find the private key for a public key.
     *
     * @param publicKey
     *     The public key to find the private key for
     * @return Private key or {@code null} if not found
     */
    public @Nullable PrivateKey findPrivateKey(@NotNull PublicKey publicKey) {
        return Optional.ofNullable(findKeyPair(publicKey)).map(KeyPair::getPrivateKey).orElse(null);
    }

    /**
     * Whether the private key corresponding to the given public key is encrypted.
     *
     * @param publicKey
     *     The public key used to search the private key
     * @return Whether the matching private key is encrypted
     */
    public boolean isKeyPairEncrypted(@NotNull PublicKey publicKey) {
        KeyPair pair = findKeyPair(publicKey);
        if (pair == null) {
            throw new IllegalStateException("Public key does not exist in keymap.");
        }

        return pair.getPrivateKey().isEncrypted();
    }

    /**
     * Attempts to sign an unsigned transaction.
     * <p>
     * When successful, return a {@link TransactionSigningResult} with the resulting signed
     * transaction. When unsuccessful, return a {@link TransactionSigningResult} that contains
     * the key pair that was locked.
     *
     * @param unsignedTransaction
     *     Unsigned transaction
     * @return A transaction signing result
     */
    public TransactionSigningResult signTransaction(
        UnsignedTransaction unsignedTransaction) throws DatabaseException, DestructionException {
        List<PrivateKey> privateKeys = new ArrayList<>();
        List<Signature> signatures = new ArrayList<>();

        for (Input input : unsignedTransaction.getInputs()) {
            UnspentOutputInfo outputInfo = walletCompositeUtxoSet.findUnspentOutputInfo(input);
            if (outputInfo == null) {
                throw new IllegalStateException(
                    "Could not find output info for input in the transaction");
            }

            PublicKey publicKey = getPublicKeys().stream()
                .filter(pub -> pub.getHash().equals(outputInfo.getAddress()))
                .findFirst()
                .orElse(null);

            if (publicKey == null) {
                throw new IllegalStateException(
                    "Referenced output uses address not found in wallet.");
            }

            KeyPair keyPair = findKeyPair(publicKey);

            if (keyPair == null) {
                throw new IllegalStateException(
                    "Could not find key pair belonging to public key.");
            }

            PrivateKey privateKey = keyPair.getPrivateKey();

            if (!privateKey.isUnlocked()) {
                return TransactionSigningResult.privateKeyLocked(keyPair);
            }

            privateKeys.add(privateKey);
        }

        Collection<Integer> handledIndices = new ArrayList<>();
        for (int i = 0; i < privateKeys.size(); i++) {
            if (handledIndices.contains(i)) {
                continue;
            }

            PrivateKey privateKey = privateKeys.get(i);
            Destructible<BigInteger> privateKeyValue = privateKey.getKey();
            for (int j = i; j < privateKeys.size(); j++) {
                if (privateKeys.get(j).equals(privateKey)) {
                    handledIndices.add(j);
                    signatures.add(
                        j,
                        signer.signMessage(
                            unsignedTransaction.getSignableTransactionData(),
                            Objects.requireNonNull(privateKeyValue.getReference().get())
                        )
                    );
                }
            }

            privateKeyValue.destruct();
        }

        return TransactionSigningResult.signed(
            unsignedTransaction.sign(signatures)
        );
    }

    /**
     * Generates and adds a plain key pair to the wallet.
     *
     * @return Key pair that was generated.
     */
    public @NotNull KeyPair generatePlainKeyPair() throws DestructionException {
        Destructible<BigInteger> randomBigInteger = keyGenerator.generateKey(consensus.getCurve()
            .getDomain()
            .getN());
        PrivateKey privateKey = PrivateKey.plain(randomBigInteger.getReference().get());
        PublicKey publicKey = consensus.getCurve().getPublicKeyFromPrivateKey(
            Objects.requireNonNull(randomBigInteger.getReference().get())
        );

        randomBigInteger.destruct();

        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        keyPairs.add(keyPair);

        keyPairListeners.forEach(l -> l.onKeyPairGenerated(keyPair));

        return keyPair;
    }

    /**
     * Generates and adds an encrypted key pair to the wallet.
     *
     * @param passphrase
     *     The passphrase to encrypt the private key with
     * @return Key pair that was generated.
     */
    public @NotNull KeyPair generateEncryptedKeyPair(
        @NotNull Destructible<char[]> passphrase) throws DestructionException, CipherException {
        Destructible<BigInteger> randomBigInteger = keyGenerator.generateKey(consensus.getCurve()
            .getDomain()
            .getN());
        // Also destructs the passphrase and big integer
        PublicKey publicKey = consensus.getCurve().getPublicKeyFromPrivateKey(
            Objects.requireNonNull(randomBigInteger.getReference().get())
        );
        PrivateKey privateKey = PrivateKey.encrypted(
            randomBigInteger,
            passphrase,
            privateKeyCipher
        );

        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        keyPairs.add(keyPair);

        randomBigInteger.destruct();

        keyPairListeners.forEach(l -> l.onKeyPairGenerated(keyPair));

        return keyPair;
    }

    @Override
    public @NotNull Iterator<KeyPair> iterator() {
        return keyPairs.iterator();
    }

    @Override
    public void onSyncWithUTXOSetStarted() {
        // Do not listen for blockchain updates when syncing (internally) with the UTXO set
        this.blockchain.removeListener(this);
    }

    @Override
    public void onSyncWithUTXOSetFinished() {
        // Listen for blockchain updates when syncing (internally) with the UTXO set is finished
        this.blockchain.addListener(this);
    }

    @Override
    public void onTopBlockConnected(@NotNull IndexedBlock indexedBlock) {
        Block block = getBlock(indexedBlock);

        // Find my transactions that might be confirmed now
        // Actually we want to find transactions that use an address in this wallet for an input
        // Because this information is already removed from the UTXO set
        // The below implementation is the best-we-can-get
        block.getTransactions().stream()
            .map(t -> transactionHistory.findUnconfirmedTransaction(t.getHash()))
            .filter(Objects::nonNull)
            .forEach(t -> {
                transactionHistory.removeUnconfirmedTransaction(t.getHash());
                transactionHistory.addConfirmedTransaction(
                    new ConfirmedTransaction(
                        t.getTransaction(),
                        block.getBlockHeight(),
                        t.getTimeReceived(),
                        t.getAmount()
                    )
                );
            });

        // Find transactions that use an address contained in this wallet
        block.getTransactions().stream()
            .filter(t -> t.getOutputs().stream().anyMatch(o -> hasAddress(o.getAddress())))
            .forEach(t -> {
                // Note that we do not need to remove from unconfirmed, because these would have
                // been removed above

                // Note that in this case, when one of the inputs is ours, these can no longer be
                // discovered (as they have disappeared from the UTXO sets)
                // Therefore, this might fail if someone is using your private key
                // But this should not happen.
                transactionHistory.addConfirmedTransaction(
                    new ConfirmedTransaction(
                        t,
                        block.getBlockHeight(),
                        Instant.now().getEpochSecond(),
                        computeNetAmount(t)
                    )
                );
            });

        balanceListeners.forEach(BalanceListener::onBalanceChanged);
    }

    @Override
    public void onTopBlockDisconnected(@NotNull IndexedBlock indexedBlock) {
        Block block = getBlock(indexedBlock);

        // Demote matching transactions in history from confirmed to unconfirmed
        block.getTransactions().stream()
            .map(t -> transactionHistory.findConfirmedTransaction(t.getHash()))
            .filter(Objects::nonNull)
            .forEach(t -> {
                transactionHistory.removeConfirmedTransaction(t.getHash());
                transactionHistory.addUnconfirmedTransaction(
                    new UnconfirmedTransaction(
                        t.getTransaction(),
                        t.getTimeReceived(),
                        t.getAmount()
                    )
                );
            });

        balanceListeners.forEach(BalanceListener::onBalanceChanged);
    }

    @Override
    public void onTransactionAddedToPool(@NotNull Transaction transaction) {
        // Add to unconfirmed transactions if an output matches and address from the wallet
        if (transaction.getOutputs().stream().anyMatch(o -> hasAddress(o.getAddress()))) {
            if (transactionHistory.findUnconfirmedTransaction(transaction.getHash()) == null) {
                transactionHistory.addUnconfirmedTransaction(
                    new UnconfirmedTransaction(
                        transaction,
                        Instant.now().getEpochSecond(),
                        computeNetAmount(transaction)
                    )
                );
            }
        }


        // Add to unconfirmed transactions if an input matches and address from the wallet
        // Note that the tx is valid because it is present in the pool, thus the inputs are present
        // in either the chain or pool UTXO
        if (transaction.getInputs().stream().anyMatch(i -> {
            UnspentOutputInfo info;
            try {
                info = chainUtxo.findUnspentOutputInfo(i);
                if (info == null) {
                    info = poolUtxo.findUnspentOutputInfo(i);
                }
            }
            catch (DatabaseException e) {
                return false;
            }

            return info != null && hasAddress(info.getAddress());
        })) {
            if (transactionHistory.findUnconfirmedTransaction(transaction.getHash()) == null) {
                transactionHistory.addUnconfirmedTransaction(
                    new UnconfirmedTransaction(
                        transaction,
                        Instant.now().getEpochSecond(),
                        computeNetAmount(transaction)
                    )
                );
            }
        }
    }

    private long computeNetAmount(@NotNull Transaction transaction) {
        long out = transaction.getOutputs().stream()
            .filter(o -> hasAddress(o.getAddress()))
            .mapToLong(Output::getAmount)
            .sum();

        long in = transaction.getInputs().stream()
            .map(i -> {
                UnspentOutputInfo info;
                try {
                    info = chainUtxo.findUnspentOutputInfo(i);
                    if (info == null) {
                        info = poolUtxo.findUnspentOutputInfo(i);
                    }
                }
                catch (DatabaseException e) {
                    return null;
                }

                if (info != null && hasAddress(info.getAddress())) {
                    return info;
                }

                return null;
            })
            .filter(Objects::nonNull)
            .mapToLong(UnspentOutputInfo::getAmount)
            .sum();

        return out - in;
    }

    private @NotNull Block getBlock(@NotNull IndexedBlock indexedBlock) {
        Block block;
        try {
            block = blockchain.getBlock(indexedBlock);
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Wallet transaction history could not be updated.", e);
            throw new RuntimeException("Wallet transaction history could not be updated.", e);
        }

        if (block == null) {
            LOGGER.log(Level.SEVERE, "Wallet transaction history could not be updated.");
            throw new RuntimeException("Wallet transaction history could not be updated.");
        }

        return block;
    }

    /**
     * Computes the confirmed or spendable balance in the chain, using the wallet UTXO set and the
     * {@link #usedInputs}.
     *
     * @param spendable
     *     Whether to compute the confirmed or spendable balance.
     * @return Confirmed balance
     */
    public long computeBalance(boolean spendable) {
        return computeKeyPairBalance(spendable, null);
    }

    /**
     * Computes the confirmed or spendable balance for a given key pair, using the wallet UTXO set
     * and the {@link #usedInputs}.
     *
     * @param spendable
     *     Whether to compute the confirmed or spendable balance.
     * @param keyPair
     *     The key pair to calculate the balance for.
     * @return Confirmed balance
     */
    public long computeKeyPairBalance(boolean spendable, KeyPair keyPair) {
        long sum = 0;

        ReadonlyUTXOSet balanceUtxo = spendable ? walletCompositeUtxoSet : walletChainUtxoSet;

        for (Map.Entry<Input, UnspentOutputInfo> entry : balanceUtxo) {
            UnspentOutputInfo info = entry.getValue();

            if (keyPair != null && !info.getAddress().equals(keyPair.getPublicKey().getHash())) {
                continue;
            }

            boolean immatureCoinbase = consensus.immatureCoinbase(blockchain.getMainChain()
                .getHeight(), info);

            if (immatureCoinbase) {
                continue;
            }

            if (spendable) {
                if (!usedInputs.contains(entry.getKey())) {
                    sum += info.getAmount();
                }
            }
            else {
                sum += info.getAmount();
            }
        }

        return sum;
    }

    /**
     * Computes the immature pending coinbase block reward.
     *
     * @return Immature coinbase balance.
     */
    public long computeImmatureCoinbase() {
        return computeImmatureCoinbase(null);
    }

    /**
     * Computes the immature pending coinbase block reward for a given keypair.
     *
     * @param keyPair
     *     The keypair to calculate the balance for.
     * @return Immature coinbase balance.
     */
    public long computeImmatureCoinbase(KeyPair keyPair) {
        long sum = 0;

        for (Map.Entry<Input, UnspentOutputInfo> entry : walletChainUtxoSet) {
            UnspentOutputInfo info = entry.getValue();

            if (keyPair != null && !info.getAddress().equals(keyPair.getPublicKey().getHash())) {
                continue;
            }

            if (consensus.immatureCoinbase(blockchain.getMainChain()
                .getHeight(), info)) {
                sum += info.getAmount();
            }
        }

        return sum;
    }

    public @NotNull TransactionHistory getTransactionHistory() {
        return transactionHistory;
    }

    public ReadonlyUTXOSet getCompositeUtxoSet() {
        return walletCompositeUtxoSet;
    }

    public void addKeyPairListener(KeyPairListener keyPairListener) {
        keyPairListeners.add(keyPairListener);
    }

    public void removeKeyPairListener(KeyPairListener keyPairListener) {
        keyPairListeners.remove(keyPairListener);
    }

    public Set<Input> getUsedInputs() {
        return new HashSet<>(usedInputs);
    }

    public void addBalanceListener(BalanceListener listener) {
        balanceListeners.add(listener);
    }

    class ChainListener implements UTXOSetListener {

        @Override
        public void onOutputUnspent(@NotNull Hash transactionHash, int outputIndex,
                                    @NotNull UnspentOutputInfo info) {
            addUnspentOutputInfo(transactionHash, outputIndex, info, walletChainUtxoSet);
            balanceListeners.forEach(BalanceListener::onBalanceChanged);
        }

        @Override
        public void onOutputSpent(@NotNull Hash transactionHash, int outputIndex) {
            setOutputSpent(transactionHash, outputIndex, walletChainUtxoSet);

            usedInputs.remove(new Input(transactionHash, outputIndex));
            balanceListeners.forEach(BalanceListener::onBalanceChanged);
        }
    }

    class PoolListener implements UTXOSetListener {

        @Override
        public void onOutputUnspent(@NotNull Hash transactionHash, int outputIndex,
                                    @NotNull UnspentOutputInfo info) {
            addUnspentOutputInfo(transactionHash, outputIndex, info, walletPoolUtxoSet);
            balanceListeners.forEach(BalanceListener::onBalanceChanged);
        }

        @Override
        public void onOutputSpent(@NotNull Hash transactionHash, int outputIndex) {
            setOutputSpent(transactionHash, outputIndex, walletPoolUtxoSet);
            balanceListeners.forEach(BalanceListener::onBalanceChanged);
        }
    }

    public void addUsedInput(Input input) {
        usedInputs.add(input);
        balanceListeners.forEach(BalanceListener::onBalanceChanged);
    }

    private void addUnspentOutputInfo(Hash transactionHash, int outputIndex,
                                      UnspentOutputInfo info, UTXODatabase utxoDatabase) {
        // Filter UTXO from addresses in this wallet and add to the wallet UTXO
        if (!hasAddress(info.getAddress())) {
            return;
        }

        try {
            utxoDatabase.addUnspentOutputInfo(transactionHash, outputIndex, info);
            LOGGER.info(() -> MessageFormat.format(
                "Added {0} cents as spendable amount to wallet for address {1}.",
                info.getAmount(),
                PublicKey.getBase58AddressFromHash(info.getAddress())
            ));
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Wallet UTXO set could not be updated.", e);
            throw new RuntimeException("Wallet UTXO set could not be updated.", e);
        }

        balanceListeners.forEach(BalanceListener::onBalanceChanged);
    }

    private void setOutputSpent(Hash transactionHash, int outputIndex, UTXODatabase utxoDatabase) {
        // Set the UTXO as spent in the wallet UTXO set as well.
        try {
            utxoDatabase.setOutputSpent(transactionHash, outputIndex);
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Wallet UTXO set could not be updated.", e);
            throw new RuntimeException("Wallet UTXO set could not be updated.", e);
        }
    }

    public Map<Input, UnspentOutputInfo> findInputs(long output,
                                                    boolean allowEqual) throws InsufficientInputException {
        return findInputs(output, 0, Collections.emptyList(), allowEqual);
    }

    public Map<Input, UnspentOutputInfo> findInputs(long targetOutput, long startInput,
                                                    Collection<Input> skippedInputs,
                                                    boolean allowEqual) throws InsufficientInputException {
        long inputSum = startInput;
        Map<Input, UnspentOutputInfo> foundInputs = new HashMap<>();

        for (Map.Entry<Input, UnspentOutputInfo> info : getCompositeUtxoSet()) {
            if (allowEqual && (inputSum >= targetOutput)) {
                break;
            }
            else if (!allowEqual && (inputSum > targetOutput)) {
                break;
            }

            Input input = info.getKey();

            // Prevent using used inputs
            if (getUsedInputs().contains(input)) {
                continue;
            }

            // Prevent duplicates
            if (skippedInputs.contains(input)) {
                continue;
            }

            // Prevent coinbase maturity failure
            if (consensus.immatureCoinbase(
                blockchain.getMainChain().getHeight(),
                info.getValue()
            )) {
                continue;
            }

            inputSum += info.getValue().getAmount();

            foundInputs.put(info.getKey(), info.getValue());
        }

        if ((allowEqual && (inputSum < targetOutput)) || (!allowEqual && (inputSum <= targetOutput))) {
            throw new InsufficientInputException(
                MessageFormat.format(
                    "Could only gather {0} while trying to match {1}",
                    inputSum,
                    targetOutput
                )
            );
        }

        return foundInputs;
    }
}

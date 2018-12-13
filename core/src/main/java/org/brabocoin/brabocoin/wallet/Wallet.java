package org.brabocoin.brabocoin.wallet;

import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.BlockchainListener;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.PublicKey;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.dal.ReadonlyUTXOSet;
import org.brabocoin.brabocoin.dal.UTXODatabase;
import org.brabocoin.brabocoin.dal.UTXOSetListener;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The wallet data structure.
 */
public class Wallet implements Iterable<KeyPair>, UTXOSetListener, BlockchainListener, BlockProcessorListener {

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
     * The wallet UTXO set.
     */
    private final @NotNull UTXODatabase utxoSet;

    /**
     * The cipher used to create private keys.
     */
    private final @NotNull Cipher privateKeyCipher;

    private final @NotNull Blockchain blockchain;

    /**
     * Cached mining address.
     */
    private @Nullable Hash miningAddress;

    /**
     * Create a wallet for a given public to private key map.
     *
     * @param keyPairs
     *     Key map
     * @param utxoSet
     *     The UTXO set for the wallet, containing only unspent outputs for the addresses
     *     corresponding to the keys contained in the wallet.
     * @param watchedUtxoSet
     *     The UTXO set that must be watched for updates, which would normally be the UTXO sets
     *     of the transaction pool and the blockchain.
     */
    public Wallet(@NotNull List<KeyPair> keyPairs,
                  @NotNull TransactionHistory transactionHistory, @NotNull Consensus consensus,
                  @NotNull Signer signer, @NotNull KeyGenerator keyGenerator,
                  @NotNull Cipher privateKeyCipher, @NotNull UTXODatabase utxoSet,
                  @NotNull ReadonlyUTXOSet watchedUtxoSet, @NotNull Blockchain blockchain) {
        this.keyPairs = new ArrayList<>(keyPairs);
        this.transactionHistory = transactionHistory;
        this.consensus = consensus;
        this.signer = signer;
        this.keyGenerator = keyGenerator;
        this.privateKeyCipher = privateKeyCipher;
        this.utxoSet = utxoSet;
        this.blockchain = blockchain;

        // Add listeners
        watchedUtxoSet.addListener(this);
        this.blockchain.addListener(this);
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
            UnspentOutputInfo outputInfo = utxoSet.findUnspentOutputInfo(input);
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

        for (PrivateKey privateKey : privateKeys) {
            Destructible<BigInteger> privateKeyValue = privateKey.getKey();
            signatures.add(
                signer.signMessage(
                    unsignedTransaction.getSignableTransactionData(),
                    Objects.requireNonNull(privateKeyValue.getReference().get())
                )
            );
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

        return keyPair;
    }

    @Override
    public @NotNull Iterator<KeyPair> iterator() {
        return keyPairs.iterator();
    }

    @Override
    public void onOutputUnspent(@NotNull Hash transactionHash, int outputIndex,
                                @NotNull UnspentOutputInfo info) {
        // Filter UTXO from addresses in this wallet and add to the wallet UTXO
        if (!hasAddress(info.getAddress())) {
            return;
        }

        try {
            utxoSet.addUnspentOutputInfo(transactionHash, outputIndex, info);
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
    }

    @Override
    public void onOutputSpent(@NotNull Hash transactionHash, int outputIndex) {
        // Set the UTXO as spent in the wallet UTXO set as well
        try {
            utxoSet.setOutputSpent(transactionHash, outputIndex);
        }
        catch (DatabaseException e) {
            LOGGER.log(Level.SEVERE, "Wallet UTXO set could not be updated.", e);
            throw new RuntimeException("Wallet UTXO set could not be updated.", e);
        }
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

        // Find transactions that pay to an address contained in this wallet
        block.getTransactions().stream()
            .filter(t -> t.getOutputs().stream().anyMatch(o -> hasAddress(o.getAddress())))
            .forEach(t -> transactionHistory.addConfirmedTransaction(
                new ConfirmedTransaction(t, block.getBlockHeight())
            ));

        // Find my transactions that might be confirmed now
        block.getTransactions().stream()
            .filter(t -> transactionHistory.findUnconfirmedTransaction(t.getHash()) != null)
            .forEach(t -> {
                transactionHistory.removeUnconfirmedTransaction(t.getHash());
                transactionHistory.addConfirmedTransaction(
                    new ConfirmedTransaction(t, block.getBlockHeight())
                );
            });
    }

    @Override
    public void onTopBlockDisconnected(@NotNull IndexedBlock indexedBlock) {
        Block block = getBlock(indexedBlock);

        // Demote matching transactions in history from confirmed to unconfirmed
        block.getTransactions().stream()
            .filter(t -> transactionHistory.findConfirmedTransaction(t.getHash()) != null)
            .forEach(t -> {
                transactionHistory.removeConfirmedTransaction(t.getHash());
                transactionHistory.addUnconfirmedTransaction(t);
            });
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
     * Retrieve the address used for mining.
     *
     * @return The address used for mining.
     */
    public @NotNull Hash getMiningAddress() {
        if (miningAddress == null) {
            miningAddress = this.keyPairs.stream()
                .filter(keyPair -> !keyPair.getPrivateKey().isEncrypted())
                .findFirst()
                .map(keyPair -> keyPair.getPublicKey().getHash())
                .orElseThrow(() -> new IllegalStateException("No mining address could be found."));
        }

        return miningAddress;
    }

    public @NotNull TransactionHistory getTransactionHistory() {
        return transactionHistory;
    }
}

package org.brabocoin.brabocoin.model.crypto;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.annotation.ProtoClass;
import net.badata.protobuf.converter.annotation.ProtoField;
import org.brabocoin.brabocoin.crypto.cipher.BouncyCastleAES;
import org.brabocoin.brabocoin.crypto.cipher.Cipher;
import org.brabocoin.brabocoin.exceptions.CipherException;
import org.brabocoin.brabocoin.exceptions.DestructionException;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.Destructible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.LambdaExceptionUtil.rethrowSupplier;

/**
 * Represents an encrypted or plain private key.
 */
@ProtoClass(BrabocoinProtos.PrivateKey.class)
public class PrivateKey implements ProtoModel<PrivateKey> {

    private static final Logger LOGGER = Logger.getLogger(PrivateKey.class.getName());

    /**
     * The ByteString representation of the encrypted or plain private key.
     */
    @ProtoField
    private final ByteString value;

    /**
     * Whether the value is encrypted using a cipher.
     */
    @ProtoField
    private final boolean encrypted;

    /**
     * The cipher used for encryption, if applicable.
     */
    @Nullable
    private final Cipher cipher;

    /**
     * Private key prefix for writing to disk.
     */
    private final static byte[] PRIVATE_KEY_PREFIX = ByteString.copyFromUtf8("PRVKEY")
        .toByteArray();

    /**
     * The temporary storage when an encrypted {@link PrivateKey} is unlocked.
     */
    private Destructible<byte[]> unlockedValue;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PrivateKey that = (PrivateKey)o;
        return encrypted == that.encrypted &&
            Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, encrypted);
    }

    /**
     * Private constructor for a private key for a given big integer.
     *
     * @param value
     *     The big integer value.
     */
    private PrivateKey(BigInteger value) {
        this.encrypted = false;
        this.value = ByteString.copyFrom(value.toByteArray());
        this.cipher = null;
    }

    /**
     * Private constructor for a private key for an encrypted big integer.
     *
     * @param value
     *     The big integer value.
     */
    private PrivateKey(Destructible<BigInteger> value, Destructible<char[]> passphrase,
                       @NotNull Cipher cipher) throws CipherException, DestructionException {
        this.encrypted = true;
        this.cipher = cipher;

        // Note, we assume the encryption method does not store a hard reference to the retrieved
        // value or passphrase.
        if (value.isDestroyed()) {
            throw new IllegalArgumentException("Value of the private key was already destroyed");
        }
        else if (passphrase.isDestroyed()) {
            throw new IllegalArgumentException("Value of the passphrase was already destroyed");
        }


        Destructible<byte[]> valueArray = new Destructible<>(
            () -> Objects.requireNonNull(value.getReference().get()).toByteArray()
        );

        Destructible<byte[]> prefixedValueArray = new Destructible<>(
            () -> {

                byte[] combined = new byte[
                    PRIVATE_KEY_PREFIX.length +
                        Objects.requireNonNull(valueArray.getReference().get()).length
                    ];
                System.arraycopy(
                    PRIVATE_KEY_PREFIX,
                    0,
                    combined,
                    0,
                    PRIVATE_KEY_PREFIX.length
                );
                System.arraycopy(
                    Objects.requireNonNull(valueArray.getReference().get()),
                    0,
                    combined,
                    PRIVATE_KEY_PREFIX.length,
                    Objects.requireNonNull(valueArray.getReference().get()).length
                );
                return combined;
            }
        );

        this.value = ByteString.copyFrom(
            cipher.encrypt(
                prefixedValueArray.getReference().get(),
                passphrase.getReference().get()
            ));

        value.destruct();
        passphrase.destruct();
        valueArray.destruct();
        prefixedValueArray.destruct();
    }

    /**
     * Constructs an encrypted private key given raw bytes.
     *
     * @param rawBytes
     *     The encrypted bytes
     * @param cipher
     *     The cipher used for encryption and decryption
     */
    private PrivateKey(ByteString rawBytes, Cipher cipher) {
        this.value = rawBytes;
        this.encrypted = true;
        this.cipher = cipher;
    }

    /**
     * Construct a private key for a given big integer with encryption and passphrase.
     * The passphrase and value are destroyed after setting the encrypted value.
     *
     * @param value
     *     The destructible big integer value.
     * @param passphrase
     *     The destructible passphrase to encrypt the value with.
     * @param cipher
     *     The cipher to encrypt the value with.
     */
    public static PrivateKey encrypted(Destructible<BigInteger> value,
                                       Destructible<char[]> passphrase,
                                       Cipher cipher) throws CipherException, DestructionException {
        return new PrivateKey(value, passphrase, cipher);
    }

    /**
     * Construct a private key for a given big integer.
     *
     * @param value
     *     The big integer value.
     */
    public static PrivateKey plain(BigInteger value) {
        return new PrivateKey(value);
    }

    /**
     * Unlocks the current private key, storing a destructible decrypted {@link #unlockedValue}
     * of the private key value.
     *
     * @param passphrase
     *     The destructible passphrase used to decrypt the value.
     * @throws CipherException
     *     When decryption fails
     * @throws DestructionException
     *     When the passphrase could not be destructed
     */
    public void unlock(
        Destructible<char[]> passphrase) throws CipherException, DestructionException {
        if (!encrypted) {
            throw new IllegalStateException("Unlocking is ill-defined for plain private keys");
        }

        Destructible<byte[]> unlockedPrefixedValue = new Destructible<>(
            rethrowSupplier(() -> cipher.decyrpt(
                value.toByteArray(),
                passphrase.getReference().get()
            ))
        );

        byte[] readPrefix = Arrays.copyOfRange(
            Objects.requireNonNull(unlockedPrefixedValue.getReference().get()),
            0,
            PRIVATE_KEY_PREFIX.length
        );
        if (!Arrays.equals(readPrefix, PRIVATE_KEY_PREFIX)) {
            throw new CipherException("Prefix mismatch");
        }

        this.unlockedValue = new Destructible<>(() -> Arrays.copyOfRange(
            Objects.requireNonNull(unlockedPrefixedValue.getReference().get()),
            PRIVATE_KEY_PREFIX.length,
            Objects.requireNonNull(unlockedPrefixedValue.getReference().get()).length
        ));

        passphrase.destruct();
    }

    /**
     * Whether the current private key is locked.
     *
     * @return True when the wallet is not encrypted or if it is encrypted
     * and the {@link #unlockedValue} exists and is not already destroyed
     */
    public boolean isUnlocked() {
        return !encrypted || (unlockedValue != null && !unlockedValue.isDestroyed());
    }

    /**
     * Gets the value of this wallet.
     * If not {@link #encrypted}, just parse the value as Big Integer,
     * if {@link #encrypted}, get the {@link #unlockedValue}.
     * <p>
     * Note, this method locks an unlocked private key.
     * The user is responsible for destroying the returned (plain) big integer.
     *
     * @return A destructible BigInteger private key
     * @throws DestructionException
     *     When the unlocked value could not be destructed
     */
    public Destructible<BigInteger> getKey() throws DestructionException {
        if (!encrypted) {
            return new Destructible<>(() -> new BigInteger(value.toByteArray()));
        }

        if (unlockedValue == null) {
            throw new IllegalStateException("Private key not unlocked.");
        }

        if (unlockedValue.isDestroyed()) {
            throw new IllegalStateException("Private key unlocked value already destroyed.");
        }

        Destructible<BigInteger> unlockedBigInteger = new Destructible<>(() -> new BigInteger(
            Objects.requireNonNull(unlockedValue.getReference().get())
        ));

        unlockedValue.destruct();

        return unlockedBigInteger;
    }

    public ByteString getValue() {
        return value;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    @Override
    public Class<? extends ProtoBuilder> getBuilder() {
        return Builder.class;
    }

    @ProtoClass(BrabocoinProtos.PrivateKey.class)
    public static class Builder implements ProtoBuilder<PrivateKey> {

        @ProtoField
        private ByteString value;

        @ProtoField
        private boolean encrypted;

        @Override
        public PrivateKey build() {
            if (encrypted) {
                try {
                    return new PrivateKey(value, new BouncyCastleAES());
                }
                catch (CipherException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            return PrivateKey.plain(new BigInteger(value.toByteArray()));
        }

        public void setValue(ByteString value) {
            this.value = value;
        }

        public void setEncrypted(boolean encrypted) {
            this.encrypted = encrypted;
        }
    }
}

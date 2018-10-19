package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.proto.ProtoBuilder;
import org.brabocoin.brabocoin.model.proto.ProtoModel;
import org.brabocoin.brabocoin.node.config.BraboConfig;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

/**
 * Provides the functionality of storing the blocks in the database.
 */
public class BlockDatabase {

    private static final ByteString KEY_PREFIX_BLOCK = ByteString.copyFromUtf8("b");
    private static final ByteString KEY_PREFIX_FILE = ByteString.copyFromUtf8("f");
    private static final ByteString KEY_CURRENT_FILE = ByteString.copyFromUtf8("l");

    /**
     * Max block file size in bytes.
     */
    private final long maxFileSize;

    private final @NotNull KeyValueStore storage;

    private final @NotNull File directory;

    /**
     * Creates a new block database using provided the key-value store and directory for the
     * block files.
     *
     * @param storage
     *         The key-value store to use for the database.
     * @param config
     *         The config used for this block database.
     * @throws DatabaseException
     *         When the database could not be initialized.
     */
    public BlockDatabase(@NotNull KeyValueStore storage, BraboConfig config) throws DatabaseException {
        this.storage = storage;
        this.directory = new File(config.blockStoreDirectory());
        this.maxFileSize = config.maxBlockFileSize();

        initialize();
    }

    private void initialize() throws DatabaseException {
        ByteString key = getCurrentFileKey();

        if (storage.get(key) == null) {
            registerNewBlockFile(0);
        }

        // Create blockStore directory if not exists
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Check if directory exists
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Parameter blockFileDirectory is not a directory.");
        }
    }

    private ByteString getCurrentFileKey() {
        return KEY_CURRENT_FILE;
    }

    private void registerNewBlockFile(int fileNumber) throws DatabaseException {
        BlockFileInfo fileInfo = BlockFileInfo.createEmpty();
        setBlockFileInfo(fileNumber, fileInfo);
        setCurrentFileNumber(fileNumber);
    }

    /**
     * Set the file information record in the database.
     *
     * @param fileNumber
     *         The file number of which to set the information record.
     * @param fileInfo
     *         The file information record to store.
     * @throws DatabaseException
     *         When the file information could not be stored.
     */
    private void setBlockFileInfo(int fileNumber, @NotNull BlockFileInfo fileInfo) throws DatabaseException {
        ByteString key = getFileKey(fileNumber);
        ByteString value = getRawProtoValue(fileInfo, BrabocoinStorageProtos.BlockFileInfo.class);

        store(key, value);
    }

    private ByteString getFileKey(int fileNumber) {
        return KEY_PREFIX_FILE.concat(ByteUtil.toByteString(fileNumber));
    }

    private <D extends ProtoModel<D>, P extends Message> ByteString getRawProtoValue(D domainObject, Class<P> protoClass) {
        return ProtoConverter.toProto(domainObject, protoClass).toByteString();
    }

    private void store(ByteString key, ByteString value) throws DatabaseException {
        storage.put(key, value);
    }

    /**
     * Stores a block on disk.
     * <p>
     * Writes the full block data to a block file on disk and records a {@link BlockInfo} record in
     * the database.
     *
     * @param block
     *         The block to store.
     * @param validated
     *         Whether the block is validated.
     * @return The block information of the added block.
     * @throws DatabaseException
     *         When the block could not be stored.
     */
    public @NotNull BlockInfo storeBlock(@NotNull Block block, boolean validated) throws DatabaseException {
        Hash hash = block.computeHash();
        ByteString key = getBlockKey(hash);

        // Check if block is already stored
        if (storage.has(key)) {
            return setBlockValidationStatus(hash, validated);
        }

        // Get serialized block
        BrabocoinProtos.Block protoBlock = ProtoConverter.toProto(block,
                BrabocoinProtos.Block.class
        );
        int size = protoBlock.getSerializedSize();

        // Get file number to write to
        int fileNumber = nextFileNumber(size);

        // Position in file where the block is be written
        long offsetInFile = writeProtoToFile(fileNumber, protoBlock);

        // Write new file info to database
        updateFileInfo(fileNumber, block, size);

        // Write the block info to database
        BlockInfo blockInfo = new BlockInfo(block.getPreviousBlockHash(),
                block.getMerkleRoot(),
                block.getTargetValue(),
                block.getNonce(),
                block.getTimestamp(),
                block.getBlockHeight(),
                block.getTransactions().size(),
                validated,
                fileNumber,
                offsetInFile,
                size
        );

        ByteString value = getRawProtoValue(blockInfo, BrabocoinStorageProtos.BlockInfo.class);
        store(key, value);

        return blockInfo;
    }

    /**
     * Sets the validation status of the block with the given hash.
     *
     * @param blockHash
     *         The hash of the block.
     * @param validated
     *         The validation status.
     * @return The updated block information.
     * @throws DatabaseException
     *         When the block is not stored in the database, or when the new status could not be
     *         written to the database.
     */
    public @NotNull BlockInfo setBlockValidationStatus(@NotNull Hash blockHash, boolean validated) throws DatabaseException {
        ByteString key = getBlockKey(blockHash);
        BlockInfo info = findBlockInfo(blockHash);

        if (info == null) {
            throw new DatabaseException("Block is not stored in database.");
        }

        BlockInfo newInfo = new BlockInfo(info.getPreviousBlockHash(),
                info.getMerkleRoot(),
                info.getTargetValue(),
                info.getNonce(),
                info.getTimestamp(),
                info.getBlockHeight(),
                info.getTransactionCount(),
                validated,
                info.getFileNumber(),
                info.getOffsetInFile(),
                info.getSizeInFile()
        );

        ByteString value = getRawProtoValue(newInfo, BrabocoinStorageProtos.BlockInfo.class);
        store(key, value);

        return info;
    }

    private void updateFileInfo(int fileNumber, @NotNull Block block, int serializedSize) throws DatabaseException {
        BlockFileInfo fileInfo = findBlockFileInfo(fileNumber);

        if (fileInfo == null) {
            throw new DatabaseException("Block file info was not found.");
        }

        BlockFileInfo newFileInfo = new BlockFileInfo(fileInfo.getNumberOfBlocks() + 1,
                fileInfo.getSize() + serializedSize,
                Math.min(fileInfo.getLowestBlockHeight(), block.getBlockHeight()),
                Math.max(fileInfo.getHighestBlockHeight(), block.getBlockHeight()),
                Math.min(fileInfo.getLowestBlockTimestamp(), block.getTimestamp()),
                Math.max(fileInfo.getHighestBlockTimestamp(), block.getTimestamp())
        );

        setBlockFileInfo(fileNumber, newFileInfo);
    }

    private long writeProtoToFile(int fileNumber, @NotNull MessageLite proto) throws DatabaseException {
        String fileName = getBlockFileName(fileNumber);
        long offsetInFile;
        try (FileOutputStream outputStream = new FileOutputStream(fileName, true)) {
            offsetInFile = outputStream.getChannel().position();
            proto.writeTo(outputStream);
        }
        catch (IOException e) {
            throw new DatabaseException("Data could not be written to disk.", e);
        }

        return offsetInFile;
    }

    private int nextFileNumber(int blockSize) throws DatabaseException {
        int current = getCurrentFileNumber();

        BlockFileInfo fileInfo = findBlockFileInfo(current);

        if (fileInfo == null) {
            throw new DatabaseException("Current file info was not found.");
        }

        if (fileInfo.getSize() + blockSize <= maxFileSize) {
            return current;
        } else {
            registerNewBlockFile(current + 1);
            return current + 1;
        }
    }

    /**
     * Find the block with the given block hash.
     *
     * @param hash
     *         The hash of the block to find.
     * @return The full block with the given hash, or {@code null} if no block with that hash is
     * stored in the database.
     * @throws DatabaseException
     *         When the block could not be retrieved.
     */
    public @Nullable Block findBlock(@NotNull Hash hash) throws DatabaseException {
        BlockInfo blockInfo = findBlockInfo(hash);

        if (blockInfo == null) {
            return null;
        }

        ByteString rawBlock = readRawBlockFromFile(blockInfo);
        return parseProtoValue(rawBlock, Block.Builder.class, BrabocoinProtos.Block.parser());
    }

    /**
     * Find the block information from the database for the block with the given hash.
     *
     * @param hash
     *         The hash of the block to find the block information for.
     * @return The block information from the database for the given block hash, or {@code null}
     * when the block hash is not present in the database.
     * @throws DatabaseException
     *         When the block information could not be retrieved.
     */
    public @Nullable BlockInfo findBlockInfo(@NotNull Hash hash) throws DatabaseException {
        ByteString key = getBlockKey(hash);
        ByteString value = retrieve(key);

        return parseProtoValue(value,
                BlockInfo.Builder.class,
                BrabocoinStorageProtos.BlockInfo.parser()
        );
    }

    private ByteString readRawBlockFromFile(@NotNull BlockInfo blockInfo) throws DatabaseException {
        String fileName = getBlockFileName(blockInfo.getFileNumber());
        byte[] data;

        try (InputStream inputStream = new FileInputStream(fileName)) {
            long offset = blockInfo.getOffsetInFile();
            int size = blockInfo.getSizeInFile();

            data = new byte[size];

            inputStream.skip(offset);
            inputStream.read(data);
        }
        catch (IOException e) {
            throw new DatabaseException("Block could not be read from file.", e);
        }

        return ByteString.copyFrom(data);
    }

    private <D extends ProtoModel<D>, B extends ProtoBuilder<D>, P extends Message> @Nullable D parseProtoValue(@Nullable ByteString value, @NotNull Class<B> domainClassBuilder, @NotNull Parser<P> parser) throws DatabaseException {
        try {
            return ProtoConverter.parseProtoValue(value, domainClassBuilder, parser);
        }
        catch (InvalidProtocolBufferException e) {
            throw new DatabaseException("Data could not be parsed", e);
        }
    }

    private ByteString getBlockKey(@NotNull Hash hash) {
        return KEY_PREFIX_BLOCK.concat(hash.getValue());
    }

    private @Nullable ByteString retrieve(ByteString key) throws DatabaseException {
        return storage.get(key);
    }

    private String getBlockFileName(int fileNumber) {
        return Paths.get(this.directory.getPath(), "blk" + fileNumber + ".dat").toString();
    }

    /**
     * Check whether the block with the given hash is stored in the database.
     *
     * @param hash
     *         The hash of the block.
     * @return Whether the block is stored in the database.
     * @throws DatabaseException
     *         When the block information could not be retrieved.
     */
    public boolean hasBlock(@NotNull Hash hash) throws DatabaseException {
        ByteString key = getBlockKey(hash);
        return storage.has(key);
    }

    /**
     * Find the file information for the given file number.
     *
     * @param fileNumber
     *         The file number to find the file information for.
     * @return The file information record, or {@code null} if the record for the given file
     * number is not present in the database.
     * @throws DatabaseException
     *         When the file information could not be retrieved.
     */
    public @Nullable BlockFileInfo findBlockFileInfo(int fileNumber) throws DatabaseException {
        ByteString key = getFileKey(fileNumber);
        ByteString value = retrieve(key);

        return parseProtoValue(value,
                BlockFileInfo.Builder.class,
                BrabocoinStorageProtos.BlockFileInfo.parser()
        );
    }

    private int getCurrentFileNumber() throws DatabaseException {
        ByteString key = getCurrentFileKey();
        ByteString value = retrieve(key);

        if (value == null) {
            throw new DatabaseException("Current file number could not be found.");
        }

        return ByteUtil.toInt(value);
    }

    private void setCurrentFileNumber(int fileNumber) throws DatabaseException {
        ByteString key = getCurrentFileKey();
        ByteString value = ByteUtil.toByteString(fileNumber);

        store(key, value);
    }
}

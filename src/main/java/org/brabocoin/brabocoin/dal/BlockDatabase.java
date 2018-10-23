package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import org.brabocoin.brabocoin.chain.IndexedBlock;
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
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.brabocoin.brabocoin.util.ByteUtil.toHexString;

/**
 * Provides the functionality of storing the blocks in the database.
 */
public class BlockDatabase {
    private static final Logger LOGGER = Logger.getLogger(BlockDatabase.class.getName());

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
     *     The key-value store to use for the database.
     * @param config
     *     The config used for this block database.
     * @throws DatabaseException
     *     When the database could not be initialized.
     */
    public BlockDatabase(@NotNull KeyValueStore storage, BraboConfig config) throws DatabaseException {
        this.storage = storage;
        this.directory = new File(config.blockStoreDirectory());
        this.maxFileSize = config.maxBlockFileSize();

        initialize();
    }

    private void initialize() throws DatabaseException {
        LOGGER.info("Initializing BlockDatabase.");
        ByteString key = getCurrentFileKey();

        if (storage.get(key) == null) {
            LOGGER.fine("Current file key not found.");
            registerNewBlockFile(0);
            LOGGER.fine("Current file key created.");
        }

        // Create blockStore directory if not exists
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                LOGGER.severe("Block storage directory could not be created.");
                throw new DatabaseException("Block storage directory could not be created.");
            }
        }

        // Check if directory exists
        if (!directory.isDirectory()) {
            LOGGER.severe("Block storage directory is not a directory.");
            throw new DatabaseException("Block storage directory is not a directory.");
        }
    }

    private ByteString getCurrentFileKey() {
        return KEY_CURRENT_FILE;
    }

    /**
     * Set the file information record in the database.
     *
     * @param fileNumber
     *     The file number of which to set the information record.
     * @param fileInfo
     *     The file information record to store.
     * @throws DatabaseException
     *     When the file information could not be stored.
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
     * <p>
     * Note that the revert file information is not available when
     * {@link BlockDatabase#storeBlockUndo(IndexedBlock, BlockUndo)} has not yet been called on
     * the block. In that case, the offset and size field for the revert file are set to {@code 0}.
     *
     * @param block
     *     The block to store.
     * @param validated
     *     Whether the block is validated.
     * @return The block information of the added block.
     * @throws DatabaseException
     *     When the block could not be stored.
     */
    public @NotNull BlockInfo storeBlock(@NotNull Block block, boolean validated) throws DatabaseException {
        LOGGER.fine("Storing block.");
        Hash hash = block.computeHash();
        ByteString key = getBlockKey(hash);
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));

        // Check if block is already stored
        if (storage.has(key)) {
            LOGGER.fine("Block is already stored.");
            return setBlockValidationStatus(hash, validated);
        }

        // Get serialized block
        BrabocoinProtos.Block protoBlock = ProtoConverter.toProto(block,
            BrabocoinProtos.Block.class
        );
        int size = protoBlock.getSerializedSize();
        LOGGER.log(Level.FINEST, "size: {0}", size);

        // Get file number to write to
        int fileNumber = nextFileNumber(size);
        LOGGER.log(Level.FINEST, "fileNumber: {0}", fileNumber);

        // Position in file where the block is be written
        long offsetInFile = writeProtoToFile(getBlockFileName(fileNumber), protoBlock);
        LOGGER.log(Level.FINEST, "offsetInFile: {0}", offsetInFile);

        // Write new file info to database
        updateFileInfo(fileNumber, block, size);
        LOGGER.log(Level.FINE, "Updated file info.");

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
            size,
            0,
            0
        );
        LOGGER.fine("Created BlockInfo for block to be stored.");

        ByteString value = getRawProtoValue(blockInfo, BrabocoinStorageProtos.BlockInfo.class);
        LOGGER.log(Level.FINEST, "value: {0}", toHexString(value));
        store(key, value);

        return blockInfo;
    }

    /**
     * Stores the block undo data and returns the updated block info record.
     *
     * @param block
     *     The block to store the undo data of.
     * @param undo
     *     The block undo data.
     * @return The updated block information record.
     * @throws DatabaseException
     *     When the undo data could not be stored.
     */
    public @NotNull BlockInfo storeBlockUndo(@NotNull IndexedBlock block,
                                             @NotNull BlockUndo undo) throws DatabaseException {
        LOGGER.fine("Store block undo data.");
        BlockInfo info = block.getBlockInfo();

        BrabocoinStorageProtos.BlockUndo protoUndo = ProtoConverter.toProto(undo,
            BrabocoinStorageProtos.BlockUndo.class
        );
        int revertSize = protoUndo.getSerializedSize();
        LOGGER.finest(() -> MessageFormat.format("revertSize={0}", revertSize));

        long offsetInRevertFile = writeProtoToFile(getRevertFileName(info.getFileNumber()),
            protoUndo
        );
        LOGGER.finest(() -> MessageFormat.format("offsetInRevertFile={0}", offsetInRevertFile));

        BlockInfo newInfo = new BlockInfo(info.getPreviousBlockHash(),
            info.getMerkleRoot(),
            info.getTargetValue(),
            info.getNonce(),
            info.getTimestamp(),
            info.getBlockHeight(),
            info.getTransactionCount(),
            info.isValidated(),
            info.getFileNumber(),
            info.getOffsetInFile(),
            info.getSizeInFile(),
            offsetInRevertFile,
            revertSize
        );

        ByteString key = getBlockKey(block.getHash());
        ByteString value = getRawProtoValue(newInfo, BrabocoinStorageProtos.BlockInfo.class);
        store(key, value);

        return newInfo;
    }

    private long writeProtoToFile(String fileName, @NotNull MessageLite proto) throws DatabaseException {
        long offsetInFile;
        try (FileOutputStream outputStream = new FileOutputStream(fileName, true)) {
            offsetInFile = outputStream.getChannel().position();
            proto.writeTo(outputStream);
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Data could not be written to disk.", e);
            throw new DatabaseException("Data could not be written to disk.", e);
        }

        return offsetInFile;
    }

    private String getRevertFileName(int fileNumber) {
        return Paths.get(this.directory.getPath(), "rev" + fileNumber + ".dat").toString();
    }

    private ByteString getBlockKey(@NotNull Hash hash) {
        LOGGER.fine("Getting block key.");
        ByteString key = KEY_PREFIX_BLOCK.concat(hash.getValue());
        LOGGER.log(Level.FINEST, "Block key: {0}", toHexString(key));
        return key;
    }

    /**
     * Sets the validation status of the block with the given hash.
     *
     * @param blockHash
     *     The hash of the block.
     * @param validated
     *     The validation status.
     * @return The updated block information.
     * @throws DatabaseException
     *     When the block is not stored in the database, or when the new status could not be
     *     written to the database.
     */
    public @NotNull BlockInfo setBlockValidationStatus(@NotNull Hash blockHash,
                                                       boolean validated) throws DatabaseException {
        LOGGER.fine("Set block validation status.");

        ByteString key = getBlockKey(blockHash);
        BlockInfo info = findBlockInfo(blockHash);

        if (info == null) {
            LOGGER.warning("Block is not stored in database.");
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
            info.getSizeInFile(),
            info.getOffsetInRevertFile(),
            info.getSizeInRevertFile()
        );

        ByteString value = getRawProtoValue(newInfo, BrabocoinStorageProtos.BlockInfo.class);
        store(key, value);

        return info;
    }

    private void updateFileInfo(int fileNumber, @NotNull Block block, int serializedSize) throws DatabaseException {
        LOGGER.fine("Updating file info.");
        BlockFileInfo fileInfo = findBlockFileInfo(fileNumber);

        if (fileInfo == null) {
            LOGGER.severe("Could not find file info.");
            throw new DatabaseException("Block file info was not found.");
        }
        LOGGER.fine("Found file info.");

        BlockFileInfo newFileInfo = new BlockFileInfo(fileInfo.getNumberOfBlocks() + 1,
            fileInfo.getSize() + serializedSize,
            Math.min(fileInfo.getLowestBlockHeight(), block.getBlockHeight()),
            Math.max(fileInfo.getHighestBlockHeight(), block.getBlockHeight()),
            Math.min(fileInfo.getLowestBlockTimestamp(), block.getTimestamp()),
            Math.max(fileInfo.getHighestBlockTimestamp(), block.getTimestamp())
        );
        LOGGER.fine("Created new file info for updated version.");

        setBlockFileInfo(fileNumber, newFileInfo);
    }

    private int nextFileNumber(int blockSize) throws DatabaseException {
        LOGGER.fine("Getting next file number.");
        int current = getCurrentFileNumber();

        BlockFileInfo fileInfo = findBlockFileInfo(current);

        if (fileInfo == null) {
            LOGGER.severe("Current file info was not found.");
            throw new DatabaseException("Current file info was not found.");
        }
        LOGGER.fine("Found file info.");

        if (fileInfo.getSize() + blockSize <= maxFileSize) {
           LOGGER.fine("Block fits in current file.");
            return current;
        } else {
            LOGGER.fine("Registering next block file, block does not fit in current file.");
            registerNewBlockFile(current + 1);
            return current + 1;
        }
    }

    private void registerNewBlockFile(int fileNumber) throws DatabaseException {
        LOGGER.fine("Registering new block file.");
        BlockFileInfo fileInfo = BlockFileInfo.createEmpty();
        LOGGER.fine("Setting block file info and file number.");
        setBlockFileInfo(fileNumber, fileInfo);
        setCurrentFileNumber(fileNumber);
    }

    /**
     * Find the block with the given block hash.
     *
     * @param hash
     *     The hash of the block to find.
     * @return The full block with the given hash, or {@code null} if no block with that hash is
     * stored in the database.
     * @throws DatabaseException
     *     When the block could not be retrieved.
     */
    public @Nullable Block findBlock(@NotNull Hash hash) throws DatabaseException {
        LOGGER.fine("Finding block for a given hash.");
        LOGGER.log(Level.FINEST, "Hash: {0}", toHexString(hash.getValue()));
        BlockInfo blockInfo = findBlockInfo(hash);

        if (blockInfo == null) {
            LOGGER.fine("Block info not found.");
            return null;
        }
        LOGGER.fine("Block info found.");

        ByteString rawBlock = readRawBlockFromFile(blockInfo);
        LOGGER.log(Level.FINEST, "Raw block data: {0}", toHexString(rawBlock));
        return parseProtoValue(rawBlock, Block.Builder.class, BrabocoinProtos.Block.parser());
    }

    /**
     * Find the block information from the database for the block with the given hash.
     *
     * @param hash
     *     The hash of the block to find the block information for.
     * @return The block information from the database for the given block hash, or {@code null}
     * when the block hash is not present in the database.
     * @throws DatabaseException
     *     When the block information could not be retrieved.
     */
    public @Nullable BlockInfo findBlockInfo(@NotNull Hash hash) throws DatabaseException {
        LOGGER.fine("Finding block info for a given hash.");
        ByteString key = getBlockKey(hash);
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        ByteString value = retrieve(key);
        LOGGER.log(Level.FINEST, "value: {0}", toHexString(value));

        return parseProtoValue(value,
            BlockInfo.Builder.class,
            BrabocoinStorageProtos.BlockInfo.parser()
        );
    }

    private @NotNull ByteString readRawBlockFromFile(@NotNull BlockInfo blockInfo) throws DatabaseException {
        LOGGER.fine("Read raw block from file.");
        String fileName = getBlockFileName(blockInfo.getFileNumber());
        LOGGER.log(Level.FINEST, "filename: {0}", fileName);
        long offset = blockInfo.getOffsetInFile();
        LOGGER.log(Level.FINEST, "offset: {0}", offset);
        int size = blockInfo.getSizeInFile();
        LOGGER.log(Level.FINEST, "size: {0}", size);

        return readBytesFromFile(fileName, offset, size);
    }

    private <D extends ProtoModel<D>, B extends ProtoBuilder<D>, P extends Message> @Nullable D parseProtoValue(@Nullable ByteString value, @NotNull Class<B> domainClassBuilder, @NotNull Parser<P> parser) throws DatabaseException {
        try {
            return ProtoConverter.parseProtoValue(value, domainClassBuilder, parser);
        }
        catch (InvalidProtocolBufferException e) {
            LOGGER.log(Level.SEVERE, "Data could not be parsed: {0}", e.getMessage());
            throw new DatabaseException("Data could not be parsed.", e);
        }
    }

    private @Nullable ByteString retrieve(ByteString key) throws DatabaseException {
        LOGGER.fine("Getting ByteString from storage.");
        return storage.get(key);
    }

    private String getBlockFileName(int fileNumber) {
        LOGGER.fine("Getting block file name.");
        String path = Paths.get(this.directory.getPath(), "blk" + fileNumber + ".dat").toString();
        LOGGER.log(Level.FINEST, "path: {0}", path);
        return path;
    }

    private @NotNull ByteString readBytesFromFile(@NotNull String fileName, long offset,
                                                  int size) throws DatabaseException {
        LOGGER.fine("Read raw bytes from file.");
        byte[] data;

        try (InputStream inputStream = new FileInputStream(fileName)) {
            data = new byte[size];

            inputStream.skip(offset);
            inputStream.read(data);
            LOGGER.fine("Read file into input stream.");
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not read file: {0}", e.getMessage());
            throw new DatabaseException("Data could not be read from file.", e);
        }

        return ByteString.copyFrom(data);
    }

    /**
     * Retrieve the block undo data from the database.
     *
     * @param hash
     *     The hash of the block of which to find the undo data of, or {@code null} when the undo
     *     data could not be found.
     * @return The block undo data.
     * @throws DatabaseException
     *     When the block undo data could not be retrieved.
     */
    public @Nullable BlockUndo findBlockUndo(@NotNull Hash hash) throws DatabaseException {
        BlockInfo info = findBlockInfo(hash);

        if (info == null) {
            return null;
        }

        return findBlockUndo(info);
    }

    /**
     * Retrieve the block undo data from the database.
     *
     * @param info
     *     The block information record of the block of which to find the undo data of, or {@code
     *     null} when the undo data could not be found.
     * @return The block undo data.
     * @throws DatabaseException
     *     When the block undo data could not be retrieved.
     */
    public @Nullable BlockUndo findBlockUndo(@NotNull BlockInfo info) throws DatabaseException {
        ByteString rawUndo = readRawUndoFromFile(info);
        return parseProtoValue(rawUndo,
            BlockUndo.Builder.class,
            BrabocoinStorageProtos.BlockUndo.parser()
        );
    }

    private @NotNull ByteString readRawUndoFromFile(@NotNull BlockInfo blockInfo) throws DatabaseException {
        LOGGER.fine("Read raw undo data from file.");
        String fileName = getRevertFileName(blockInfo.getFileNumber());
        LOGGER.log(Level.FINEST, "filename: {0}", fileName);
        long offset = blockInfo.getOffsetInRevertFile();
        LOGGER.log(Level.FINEST, "offset: {0}", offset);
        int size = blockInfo.getSizeInRevertFile();
        LOGGER.log(Level.FINEST, "size: {0}", size);

        return readBytesFromFile(fileName, offset, size);
    }

    /**
     * Check whether the block with the given hash is stored in the database.
     *
     * @param hash
     *     The hash of the block.
     * @return Whether the block is stored in the database.
     * @throws DatabaseException
     *     When the block information could not be retrieved.
     */
    public boolean hasBlock(@NotNull Hash hash) throws DatabaseException {
        ByteString key = getBlockKey(hash);
        return storage.has(key);
    }

    /**
     * Find the file information for the given file number.
     *
     * @param fileNumber
     *     The file number to find the file information for.
     * @return The file information record, or {@code null} if the record for the given file
     * number is not present in the database.
     * @throws DatabaseException
     *     When the file information could not be retrieved.
     */
    public @Nullable BlockFileInfo findBlockFileInfo(int fileNumber) throws DatabaseException {
        LOGGER.fine("Getting block file info.");
        ByteString key = getFileKey(fileNumber);
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        ByteString value = retrieve(key);
        LOGGER.log(Level.FINEST, "value: {0}", toHexString(value));

        return parseProtoValue(value,
            BlockFileInfo.Builder.class,
            BrabocoinStorageProtos.BlockFileInfo.parser()
        );
    }

    private int getCurrentFileNumber() throws DatabaseException {
        LOGGER.fine("Getting current file number.");
        ByteString key = getCurrentFileKey();
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        ByteString value = retrieve(key);
        LOGGER.log(Level.FINEST, "value: {0}", toHexString(value));

        if (value == null) {
            LOGGER.severe("Current file number could not be found.");
            throw new DatabaseException("Current file number could not be found.");
        }
        LOGGER.fine("Got current file number.");

        return ByteUtil.toInt(value);
    }

    private void setCurrentFileNumber(int fileNumber) throws DatabaseException {
        LOGGER.fine("Setting current file number.");
        ByteString key = getCurrentFileKey();
        LOGGER.log(Level.FINEST, "key: {0}", toHexString(key));
        ByteString value = ByteUtil.toByteString(fileNumber);
        LOGGER.log(Level.FINEST, "value: {0}", toHexString(value));

        store(key, value);
    }
}

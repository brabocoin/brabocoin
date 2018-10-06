package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ByteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Provides the functionality of storing the blocks in the database.
 */
public class BlockDatabase {

    private static final ByteString KEY_PREFIX_BLOCK = ByteString.copyFromUtf8("b");
    private static final ByteString KEY_PREFIX_FILE = ByteString.copyFromUtf8("f");
    private static final ByteString KEY_CURRENT_FILE = ByteString.copyFromUtf8("l");

    private static final Converter PROTO_CONVERTER = Converter.create();

    /**
     * Max block file size in bytes.
     */
    private static final long MAX_FILE_SIZE = 128_000_000; // 128 MB

    private final @NotNull KeyValueStore storage;

    private final @NotNull File directory;

    /**
     * Creates a new block database using provided the key-value store and directory for the
     * block files.
     *
     * @param storage
     *         The key-value store to use for the database.
     * @param blockFileDirectory
     *         The directory in which to store the block files.
     */
    public BlockDatabase(@NotNull KeyValueStore storage, @NotNull File blockFileDirectory) {
        if (!blockFileDirectory.isDirectory()) {
            throw new IllegalArgumentException("Parameter blockFileDirectory is not a directory.");
        }

        this.storage = storage;
        this.directory = blockFileDirectory;
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
     * @throws DatabaseException
     *         When the block could not be stored.
     */
    public void storeBlock(@NotNull Block block, boolean validated) throws DatabaseException {
        // Get serialized block
        BrabocoinProtos.Block protoBlock = PROTO_CONVERTER.toProtobuf(BrabocoinProtos.Block.class,
                block
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

        byte[] key = getBlockKey(block.computeHash());
        byte[] value = getRawProtoValue(blockInfo, BrabocoinStorageProtos.BlockInfo.class);
        store(key, value);
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
        try {
            String fileName = getBlockFileName(fileNumber);
            FileOutputStream outputStream = new FileOutputStream(fileName, true);

            long offsetInFile = outputStream.getChannel().position();

            proto.writeTo(outputStream);
            outputStream.close();

            return offsetInFile;
        }
        catch (IOException e) {
            throw new DatabaseException("Data could not be written to disk.", e);
        }
    }

    private int nextFileNumber(int blockSize) throws DatabaseException {
        int current = getCurrentFileNumber();

        BlockFileInfo fileInfo = findBlockFileInfo(current);

        if (fileInfo == null) {
            throw new DatabaseException("Current file info was not found.");
        }

        if (fileInfo.getSize() + blockSize <= MAX_FILE_SIZE) {
            return current;
        } else {
            registerNewBlockFile(current + 1);
            return current + 1;
        }
    }

    private void registerNewBlockFile(int fileNumber) throws DatabaseException {
        BlockFileInfo fileInfo = BlockFileInfo.createEmpty();
        setBlockFileInfo(fileNumber, fileInfo);
        setCurrentFileNumber(fileNumber);
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

        byte[] rawBlock = readRawBlockFromFile(blockInfo);
        return parseProtoValue(rawBlock, Block.class, BrabocoinProtos.Block.parser());
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
        byte[] key = getBlockKey(hash);
        byte[] value = retrieve(key);

        return parseProtoValue(value, BlockInfo.class, BrabocoinStorageProtos.BlockInfo.parser());
    }

    private byte[] readRawBlockFromFile(@NotNull BlockInfo blockInfo) throws DatabaseException {
        String fileName = getBlockFileName(blockInfo.getFileNumber());

        try {
            InputStream inputStream = new FileInputStream(fileName);

            long offset = blockInfo.getOffsetInFile();
            int size = blockInfo.getSizeInFile();

            byte[] data = new byte[size];

            inputStream.skip(offset);
            inputStream.read(data);

            return data;
        }
        catch (IOException e) {
            throw new DatabaseException("Block could not be read from file.", e);
        }
    }

    private <D, P extends Message> @Nullable D parseProtoValue(@Nullable byte[] value,
                                                               @NotNull Class<D> domainClass,
                                                               @NotNull Parser<P> parser) throws DatabaseException {
        if (value == null) {
            return null;
        }

        P proto;
        try {
            proto = parser.parseFrom(value);
        }
        catch (InvalidProtocolBufferException e) {
            throw new DatabaseException("Data could not be parsed.", e);
        }

        return PROTO_CONVERTER.toDomain(domainClass, proto);
    }

    private byte[] getBlockKey(@NotNull Hash hash) {
        return KEY_PREFIX_BLOCK.concat(hash.getValue()).toByteArray();
    }

    private @Nullable byte[] retrieve(byte[] key) throws DatabaseException {
        return storage.get(key);
    }

    private String getBlockFileName(int fileNumber) {
        return this.directory.getPath() + File.pathSeparator + "blk" + fileNumber + ".dat";
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
    public void setBlockFileInfo(int fileNumber, @NotNull BlockFileInfo fileInfo) throws DatabaseException {
        byte[] key = getFileKey(fileNumber);
        byte[] value = getRawProtoValue(fileInfo, BrabocoinStorageProtos.BlockFileInfo.class);

        store(key, value);
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
        byte[] key = getFileKey(fileNumber);
        byte[] value = retrieve(key);

        return parseProtoValue(value,
                BlockFileInfo.class,
                BrabocoinStorageProtos.BlockFileInfo.parser()
        );
    }

    private byte[] getFileKey(int fileNumber) {
        return KEY_PREFIX_FILE.concat(ByteUtil.toByteString(fileNumber)).toByteArray();
    }

    private <D, P extends Message> byte[] getRawProtoValue(D domainObject, Class<P> protoClass) {
        return PROTO_CONVERTER.toProtobuf(protoClass, domainObject).toByteArray();
    }

    private int getCurrentFileNumber() throws DatabaseException {
        byte[] key = getCurrentFileKey();
        byte[] value = retrieve(key);

        if (value == null) {
            throw new DatabaseException("Current file number could not be found.");
        }

        return ByteUtil.toInt(value);
    }

    private void setCurrentFileNumber(int fileNumber) throws DatabaseException {
        byte[] key = getCurrentFileKey();
        byte[] value = ByteUtil.toByteString(fileNumber).toByteArray();

        store(key, value);
    }

    private byte[] getCurrentFileKey() {
        return KEY_CURRENT_FILE.toByteArray();
    }

    private void store(byte[] key, byte[] value) throws DatabaseException {
        storage.put(key, value);
    }
}

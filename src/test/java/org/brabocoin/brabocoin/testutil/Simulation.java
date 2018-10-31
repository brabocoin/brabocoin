package org.brabocoin.brabocoin.testutil;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.model.dal.BlockInfo;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Output;
import org.brabocoin.brabocoin.model.Signature;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.util.ByteUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Simulation {
    private static Random RANDOM = new Random();

    public static List<Block> randomBlockChainGenerator(int length) {
        return randomBlockChainGenerator(length, new Hash(ByteUtil.toByteString(0)), 0);
    }

    public static List<Block> randomBlockChainGenerator(int length, Hash previousHash, int startBlockHeight) {
        List<Block> list = new ArrayList<>();

        for (int i = startBlockHeight; i < length + startBlockHeight; i++) {
            Block block = randomBlock(previousHash, i, 5, 5, 30);
            previousHash = block.computeHash();
            list.add(block);
        }

        return list;
    }

    public static List<IndexedBlock> randomIndexedBlockChainGenerator(int length) {
        return randomIndexedBlockChainGenerator(length, new Hash(ByteUtil.toByteString(0)), 0);
    }

    public static List<IndexedBlock> randomIndexedBlockChainGenerator(int length, Hash previousHash, int startBlockHeight) {
        List<IndexedBlock> list = new ArrayList<>();
        long creationTime = new Date().getTime();

        for (int i = startBlockHeight; i < startBlockHeight + length; i++) {
            Block block = randomBlock(
                previousHash,

                i,
                5, 5, 30);

            BlockInfo info = new BlockInfo(
                block.getPreviousBlockHash(),
                block.getMerkleRoot(),
                block.getTargetValue(),
                block.getNonce(),
                block.getTimestamp(),
                block.getBlockHeight(),
                block.getTransactions().size(),
                false,
                0,
                0,
                0,
                -1,
                -1
            );

            previousHash = block.computeHash();
            IndexedBlock indexedBlock = new IndexedBlock(previousHash, info);
            list.add(indexedBlock);
        }

        return list;
    }

    public static Block randomBlock(Hash previousHash, int blockHeight, int transactionInputBound, int transactionOutputBound, int transactionsBound) {
        long creationTime = new Date().getTime();

        return new Block(
                previousHash,
                randomHash(),
                randomHash(),
                randomByteString(),
                creationTime,
                blockHeight,
                repeatedBuilder(() -> randomTransaction(transactionInputBound,transactionOutputBound), transactionsBound));
    }

    public static Transaction randomTransaction(int inputBound, int outputBound) {
        List<Input> inputs = repeatedBuilder(Simulation::randomInput, inputBound);
        List<Output> outputs = repeatedBuilder(Simulation::randomOutput, outputBound);
        return new Transaction(inputs, outputs);
    }

    public static <U> List<U> repeatedBuilder(Callable<U> builder, int bound) {
        return IntStream.range(0, RANDOM.nextInt(bound) + 1).mapToObj(i -> {
            try {
                return builder.call();
            } catch (Exception e) {
                return null;
            }
        }).collect(Collectors.toList());
    }

    public static Input randomInput() {
        return new Input(new Signature(), randomHash(), RANDOM.nextInt(5));
    }

    public static Output randomOutput() {
        return new Output(randomHash(), RANDOM.nextInt(1000));
    }

    public static Hash randomHash() {
        return new Hash(randomByteString());
    }

    public static ByteString randomByteString() {
        byte[] randomBytes = new byte[64];
        RANDOM.nextBytes(randomBytes);
        return ByteString.copyFrom(randomBytes);
    }
}

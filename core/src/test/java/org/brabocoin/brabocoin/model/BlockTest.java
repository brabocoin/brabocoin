package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockTest{
    @Test
    void testComputeHash() {
        ByteString dummyByteString = ByteString.copyFromUtf8("dummyValue");
        Hash dummyHash = new Hash(dummyByteString);


        List<Input> inputList = new ArrayList<Input>() {{
            add(new Input(Simulation.randomSignature(), dummyHash, 0));
            add(new Input(Simulation.randomSignature(), dummyHash, 1));
        }};

        List<Output> outputList = new ArrayList<Output>() {{
            add(new Output(dummyHash, 1984L));
        }};

        List<Transaction> transactionList = new ArrayList<Transaction>() {{
            add(new Transaction(inputList, outputList));
        }};

        Block block = new Block(dummyHash, dummyHash, dummyHash, Simulation.randomBigInteger(), 7, transactionList);
        Hash blockHash = block.getHash();
        assertNotNull(blockHash);
    }
}
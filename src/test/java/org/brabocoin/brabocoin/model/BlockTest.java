package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
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
            add(new Input(new Signature(), dummyHash, 0));
            add(new Input(new Signature(), dummyHash, 1));
        }};

        List<Output> outputList = new ArrayList<Output>() {{
            add(new Output(dummyHash, 1984L));
        }};

        List<Transaction> transactionList = new ArrayList<Transaction>() {{
            add(new Transaction(inputList, outputList));
        }};

        Block block = new Block(dummyHash, dummyHash, dummyHash, dummyByteString, 13L, 7L, transactionList);
        Hash blockHash = block.computeHash();
        assertNotNull(blockHash);
    }
}
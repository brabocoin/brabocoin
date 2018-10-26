package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransactionTest {
    static Hash transactionHash = new Hash(ByteString.copyFromUtf8("test"));
    static List<Input> inputList;
    static List<Output> outputList;

    @BeforeAll
    static void setUp() {
        inputList = new ArrayList<Input>() {{
            add(new Input(new Signature(), transactionHash, 0));
            add(new Input(new Signature(), transactionHash, 0));
        }};

        outputList = new ArrayList<Output>() {{
            add(new Output(new Hash(ByteString.copyFromUtf8("output0")), 12L));
            add(new Output(new Hash(ByteString.copyFromUtf8("output1")), 34L));
        }};
    }

    @Test
    void protoConvertToDTOTransactionTest() {
        Transaction transaction = new Transaction(inputList, outputList);
        BrabocoinProtos.Transaction protoTransaction = ProtoConverter.toProto(transaction, BrabocoinProtos.Transaction.class);


        assertEquals(2, protoTransaction.getInputsCount());
        assertEquals(2, protoTransaction.getOutputsCount());
    }

    @Test
    void protoConvertToDOMTransactionTest() {
        Transaction transaction = new Transaction(inputList, outputList);
        BrabocoinProtos.Transaction protoTransaction = ProtoConverter.toProto(transaction, BrabocoinProtos.Transaction.class);
        Transaction transactionReflection = ProtoConverter.toDomain(protoTransaction, Transaction.Builder.class);

        assertEquals(2, transactionReflection.getInputs().size());
        assertEquals(2, transactionReflection.getOutputs().size());
    }

    @Test
    void testComputeHash() {
        Transaction transaction = new Transaction(inputList, outputList);
        Hash hash = transaction.computeHash();
        assertNotNull(hash);
    }
}

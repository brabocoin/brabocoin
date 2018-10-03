package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {
    static Hash transactionHash = new Hash(ByteString.copyFromUtf8("test"));
    static List<Input> inputList;
    static List<Output> outputList;

    @BeforeAll
    static void setUp() {
        inputList = new ArrayList<Input>() {{
            add(new Input(new Signature(), transactionHash));
            add(new Input(new Signature(), transactionHash));
        }};

        outputList = new ArrayList<Output>() {{
            add(new Output(new Hash(ByteString.copyFromUtf8("output0")), 12L));
            add(new Output(new Hash(ByteString.copyFromUtf8("output1")), 34L));
        }};
    }

    @Test
    void protoConvertToDTOTransactionTest() {
        Transaction transaction = new Transaction(inputList, outputList);
        BrabocoinProtos.Transaction protoTransaction = Converter.create()
                .toProtobuf(BrabocoinProtos.Transaction.class, transaction);


        assertEquals(2, protoTransaction.getInputsCount());
        assertEquals(2, protoTransaction.getOutputsCount());
    }

    @Test
    void protoConvertToDOMTransactionTest() {
        Transaction transaction = new Transaction(inputList, outputList);
        BrabocoinProtos.Transaction protoTransaction = Converter.create()
                .toProtobuf(BrabocoinProtos.Transaction.class, transaction);
        Transaction transactionReflection = Converter.create()
                .toDomain(Transaction.Builder.class, protoTransaction).createTransaction();

        assertEquals(2, transactionReflection.getInputs().size());
        assertEquals(2, transactionReflection.getOutputs().size());
    }
}
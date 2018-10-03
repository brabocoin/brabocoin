package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InputTest {
    @Test
    void protoConvertInputToDTOTest() {
        String transactionHash = "test";
        Input input = new Input(new Signature(), new Hash(ByteString.copyFromUtf8(transactionHash)));
        BrabocoinProtos.Input protoInput = Converter.create()
                .toProtobuf(BrabocoinProtos.Input.class, input);

        assertEquals(transactionHash, protoInput.getReferencedTransaction().getValue().toStringUtf8());
    }

    @Test
    void protoConvertInputToDOMTest() {
        String transactionHash = "test";
        Input input = new Input(new Signature(), new Hash(ByteString.copyFromUtf8(transactionHash)));
        BrabocoinProtos.Input protoInput = Converter.create()
                .toProtobuf(BrabocoinProtos.Input.class, input);
        Input inputReflecion = Converter.create()
                .toDomain(Input.Builder.class, protoInput).createInput();

        assertEquals(transactionHash, inputReflecion.getReferencedTransaction().getValue().toStringUtf8());
    }
}
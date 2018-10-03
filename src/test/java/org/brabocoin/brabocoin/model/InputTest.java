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

    static Transaction coinbase;

    @BeforeAll
    static void setUp() {
        coinbase = new Transaction(new ArrayList<>(), new ArrayList<>());
    }

    @Test
    void protoConvertInputToDTOTest() {
        Input input = new Input(new Signature(), coinbase);
        BrabocoinProtos.Input protoInput = Converter.create()
                .toProtobuf(BrabocoinProtos.Input.class, input);

        assertEquals(0, protoInput.getReferencedTransaction().getInputsCount());
    }

    @Test
    void protoConvertInputToDOMTest() {
        Input input = new Input(new Signature(), coinbase);
        BrabocoinProtos.Input protoInput = Converter.create()
                .toProtobuf(BrabocoinProtos.Input.class, input);
        Input inputReflecion = Converter.create()
                .toDomain(Input.Builder.class, protoInput).createInput();

        assertEquals(0, inputReflecion.getReferencedTransaction().getInputs().size());
    }
}
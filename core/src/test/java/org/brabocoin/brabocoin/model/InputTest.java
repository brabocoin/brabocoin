package org.brabocoin.brabocoin.model;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.testutil.Simulation;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputTest {
    @Test
    void protoConvertInputToDTOTest() {
        String transactionHash = "test";
        Input input = new Input(Simulation.randomSignature(), new Hash(ByteString.copyFromUtf8(transactionHash)), 0);
        BrabocoinProtos.Input protoInput = ProtoConverter.toProto(input, BrabocoinProtos.Input.class);

        assertEquals(transactionHash, protoInput.getReferencedTransaction().getValue().toStringUtf8());
    }

    @Test
    void protoConvertInputToDOMTest() {
        String transactionHash = "test";
        Input input = new Input(Simulation.randomSignature(), new Hash(ByteString.copyFromUtf8(transactionHash)), 0);
        BrabocoinProtos.Input protoInput = ProtoConverter.toProto(input, BrabocoinProtos.Input.class);
        Input inputReflecion = ProtoConverter.toDomain(protoInput, Input.Builder.class);

        assertEquals(transactionHash, inputReflecion.getReferencedTransaction().getValue().toStringUtf8());
    }
}

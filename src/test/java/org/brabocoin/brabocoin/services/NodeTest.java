package org.brabocoin.brabocoin.services;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void getBlockTest() throws IOException {
        final Node n1 = new Node(8980, 8981);
        final Node n2 = new Node(8981, 8980);

        n1.start();
        n2.start();

        String echoText = "abcdef";

        Hash hash = new Hash.Builder().setValue(ByteString.copyFromUtf8(echoText)).createHash();
        BrabocoinProtos.Hash protoHash = Converter.create().toProtobuf(BrabocoinProtos.Hash.class, hash);
        BrabocoinProtos.Block protoBlock = n1.blockingStub.getBlock(protoHash);
        Block block = Converter.create().toDomain(Block.Builder.class, protoBlock).createBlock();

        assertEquals(echoText, block.getPreviousBlockHash().getValue().toStringUtf8());

        n1.stop();
        n2.stop();
    }
}
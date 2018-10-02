package org.brabocoin.brabocoin;

import com.google.protobuf.ByteString;
import net.badata.protobuf.converter.Converter;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.services.Node;

public class LocalTest {
    public static void main(final String[] args) throws Exception {
        final Node n1 = new Node(8980, 8981);
        final Node n2 = new Node(8981, 8980);

        n1.start();
        n2.start();

        Hash hash = new Hash.Builder().setValue(ByteString.copyFromUtf8("abcdef")).createHash();
        BrabocoinProtos.Hash protoHash = Converter.create().toProtobuf(BrabocoinProtos.Hash.class, hash);
        BrabocoinProtos.Block protoBlock = n1.blockingStub.getBlock(protoHash);
        Block block = Converter.create().toDomain(Block.Builder.class, protoBlock).createBlock();
        System.out.println(block.getPreviousBlockHash().getValue().toString());
    }
}

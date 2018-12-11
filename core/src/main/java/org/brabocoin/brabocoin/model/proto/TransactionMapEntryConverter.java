package org.brabocoin.brabocoin.model.proto;

import net.badata.protobuf.converter.type.TypeConverter;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Convert a map of confirmed transactions to a list of entries.
 */
public class TransactionMapEntryConverter implements TypeConverter<Map<Hash,
    Transaction>, List<BrabocoinStorageProtos.TransactionMapEntry>> {

    @Override
    public Map<Hash, Transaction> toDomainValue(Object instance) {
        return ((List<BrabocoinStorageProtos.TransactionMapEntry>)instance).stream()
            .collect(Collectors.toMap(
                entry -> ProtoConverter.toDomain(entry.getKey(), Hash.Builder.class),
                entry -> ProtoConverter.toDomain(
                    entry.getValue(),
                    Transaction.Builder.class
                )
            ));
    }

    @Override
    public List<BrabocoinStorageProtos.TransactionMapEntry> toProtobufValue(
        Object instance) {
        return ((Map<Hash, Transaction>)instance).entrySet().stream()
            .map(
                entry -> BrabocoinStorageProtos.TransactionMapEntry.newBuilder()
                    .setKey(ProtoConverter.toProto(entry.getKey(), BrabocoinProtos.Hash.class))
                    .setValue(ProtoConverter.toProto(
                        entry.getValue(),
                        BrabocoinProtos.Transaction.class
                    ))
                    .build()
            )
            .collect(Collectors.toList());
    }
}

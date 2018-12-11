package org.brabocoin.brabocoin.model.proto;

import net.badata.protobuf.converter.type.TypeConverter;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.wallet.ConfirmedTransaction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Convert a map of confirmed transactions to a list of entries.
 */
public class ConfirmedTransactionMapEntryConverter implements TypeConverter<Map<Hash,
    ConfirmedTransaction>, List<BrabocoinStorageProtos.ConfirmedTransactionMapEntry>> {

    @Override
    public Map<Hash, ConfirmedTransaction> toDomainValue(Object instance) {
        return ((List<BrabocoinStorageProtos.ConfirmedTransactionMapEntry>)instance).stream()
            .collect(Collectors.toMap(
                entry -> ProtoConverter.toDomain(entry.getKey(), Hash.Builder.class),
                entry -> ProtoConverter.toDomain(
                    entry.getValue(),
                    ConfirmedTransaction.Builder.class
                )
            ));
    }

    @Override
    public List<BrabocoinStorageProtos.ConfirmedTransactionMapEntry> toProtobufValue(
        Object instance) {
        return ((Map<Hash, ConfirmedTransaction>)instance).entrySet().stream()
            .map(
                entry -> BrabocoinStorageProtos.ConfirmedTransactionMapEntry.newBuilder()
                    .setKey(ProtoConverter.toProto(entry.getKey(), BrabocoinProtos.Hash.class))
                    .setValue(ProtoConverter.toProto(
                        entry.getValue(),
                        BrabocoinStorageProtos.ConfirmedTransaction.class
                    ))
                    .build()
            )
            .collect(Collectors.toList());
    }
}

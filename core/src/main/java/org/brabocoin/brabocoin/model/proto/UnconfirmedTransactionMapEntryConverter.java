package org.brabocoin.brabocoin.model.proto;

import net.badata.protobuf.converter.type.TypeConverter;
import org.brabocoin.brabocoin.model.Hash;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.brabocoin.brabocoin.proto.model.BrabocoinProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.brabocoin.brabocoin.wallet.UnconfirmedTransaction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Convert a map of confirmed transactions to a list of entries.
 */
public class UnconfirmedTransactionMapEntryConverter implements TypeConverter<Map<Hash,
    UnconfirmedTransaction>, List<BrabocoinStorageProtos.UnconfirmedTransactionMapEntry>> {

    @Override
    public Map<Hash, UnconfirmedTransaction> toDomainValue(Object instance) {
        return ((List<BrabocoinStorageProtos.UnconfirmedTransactionMapEntry>)instance).stream()
            .collect(Collectors.toMap(
                entry -> ProtoConverter.toDomain(entry.getKey(), Hash.Builder.class),
                entry -> ProtoConverter.toDomain(
                    entry.getValue(),
                    UnconfirmedTransaction.Builder.class
                )
            ));
    }

    @Override
    public List<BrabocoinStorageProtos.UnconfirmedTransactionMapEntry> toProtobufValue(
        Object instance) {
        return ((Map<Hash, UnconfirmedTransaction>)instance).entrySet().stream()
            .map(
                entry -> BrabocoinStorageProtos.UnconfirmedTransactionMapEntry.newBuilder()
                    .setKey(ProtoConverter.toProto(entry.getKey(), BrabocoinProtos.Hash.class))
                    .setValue(ProtoConverter.toProto(
                        entry.getValue(),
                        BrabocoinStorageProtos.UnconfirmedTransaction.class
                    ))
                    .build()
            )
            .collect(Collectors.toList());
    }
}

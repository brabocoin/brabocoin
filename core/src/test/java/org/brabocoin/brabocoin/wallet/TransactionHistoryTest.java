package org.brabocoin.brabocoin.wallet;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.proto.dal.BrabocoinStorageProtos;
import org.brabocoin.brabocoin.util.ProtoConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests {@link TransactionHistory}
 */
class TransactionHistoryTest {

    private TransactionHistory transactionHistory;

    @BeforeEach
    void setUp() {
        transactionHistory = new TransactionHistory(new HashMap<>(), new HashMap<>());
    }

    @Test
    void convertToProtoEmpty() {
        ByteString txHistoryBytes = ProtoConverter.toProtoBytes(
            transactionHistory,
            BrabocoinStorageProtos.TransactionHistory.class
        );

        assertNotNull(txHistoryBytes);
    }
}

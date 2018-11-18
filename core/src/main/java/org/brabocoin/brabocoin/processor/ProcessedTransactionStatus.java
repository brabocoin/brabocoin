package org.brabocoin.brabocoin.processor;

/**
 * Status of an transaction in the transaction pool.
 */
public enum ProcessedTransactionStatus {
    INVALID,
    ALREADY_STORED,
    INDEPENDENT,
    DEPENDENT,
    ORPHAN
}

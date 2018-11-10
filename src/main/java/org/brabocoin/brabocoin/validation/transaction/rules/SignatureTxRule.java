package org.brabocoin.brabocoin.validation.transaction.rules;

import com.deliveredtechnologies.rulebook.annotation.Given;
import com.deliveredtechnologies.rulebook.annotation.Rule;
import com.deliveredtechnologies.rulebook.annotation.When;
import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.chain.Blockchain;
import org.brabocoin.brabocoin.chain.IndexedBlock;
import org.brabocoin.brabocoin.crypto.Signer;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.brabocoin.brabocoin.model.Block;
import org.brabocoin.brabocoin.model.Input;
import org.brabocoin.brabocoin.model.Transaction;
import org.brabocoin.brabocoin.model.dal.UnspentOutputInfo;
import org.brabocoin.brabocoin.processor.TransactionProcessor;
import org.brabocoin.brabocoin.validation.transaction.TransactionRule;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transaction rule
 * <p>
 * All signatures of the input must be valid.
 */
@Rule(name = "Signature rule")
public class SignatureTxRule extends TransactionRule {
    @Given("transactionProcessor")
    private TransactionProcessor transactionProcessor;

    @Given("signer")
    private Signer signer;

    @Given("blockchain")
    private Blockchain blockchain;

    @When
    public boolean valid() {
        List<SignatureVerificationData> data = transaction.getInputs()
                .stream()
                .map(i -> {
                    try {
                        return new SignatureVerificationData(
                                i,
                                transactionProcessor.findUnspentOutputInfo(i)
                        );
                    } catch (DatabaseException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.toList());

        /*
         * TODO: Do not get the block from disk?
         * This is just a placeholder and extremely ugly way to get the transaction signable data
         */
        data.forEach(o -> {
            try {
                IndexedBlock indexedBlock = blockchain.getMainChain().getBlockAtHeight(o.getUnspentOutputInfo().getBlockHeight());
                if (indexedBlock == null)
                    return;

                Block block = blockchain.getBlock(indexedBlock);
                if (block == null)
                    return;

                Optional<Transaction> referencedTransaction = block.getTransactions()
                        .stream()
                        .filter(t -> t.computeHash().equals(o.getInput().getReferencedTransaction()))
                        .findFirst();

                referencedTransaction.ifPresent(t -> o.setData(t.getSignableTransactionData()));
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
        });

        if (data.stream().anyMatch(o -> o.getData() == null)) {
            return false;
        }

        return data.stream()
                .allMatch(t -> signer.verifySignature(
                        t.getInput().getSignature(),
                        t.getUnspentOutputInfo().getAddress(),
                        t.getData()
                ));
    }

    private class SignatureVerificationData {
        private Input input;
        private UnspentOutputInfo unspentOutputInfo;
        private ByteString data;

        public SignatureVerificationData(Input input, UnspentOutputInfo unspentOutputInfo) {
            this.input = input;
            this.unspentOutputInfo = unspentOutputInfo;
        }

        public ByteString getData() {
            return data;
        }

        public UnspentOutputInfo getUnspentOutputInfo() {
            return unspentOutputInfo;
        }

        public Input getInput() {
            return input;
        }

        public void setData(ByteString data) {
            this.data = data;
        }
    }
}

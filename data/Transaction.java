package data;

import util.ThreadSafeStringFormatter;

import java.io.Serializable;

/**
 * Describes a transaction between a Coordinator and Participant during 2 Phase Commit
 */
public class Transaction implements Serializable {

    private static final Object messageIndexLock = new Object();
    private static volatile int messageIndex = 0;

    Operations op;
    String key;
    String value;
    int index;

    /**
     * Creates an instance of a transaction when the transaction requires a key and value
     *
     * @param op type of operation to perform on the KeyValue store
     * @param key describes an entry in the KeyValue store
     * @param value describes a value associated with a key in the KeyValue store
     */
    public Transaction(Operations op, String key, String value) {
        this.op = op;
        this.key = key;
        this.value = value;
        // ensure index assignment and manipulation is atomic
        synchronized (messageIndexLock) {
            this.index = Transaction.messageIndex;
            Transaction.messageIndex += 1;
        }
    }

    /**
     * Get the operation associated with this Transaction
     *
     * @return the operation associated with this Transaction
     */
    public Operations getOp() { return this.op; }

    /**
     * Get the key associated with the operation in this Transaction
     *
     * @return the key associated with the operation in this Transaction
     */
    public String getKey() { return this.key; }

    /**
     * Get the value associated with the key and operation in this Transaction
     *
     * @return the value associated with the key and operation in this Transaction
     */
    public String getValue() { return this.value; }

    /**
     * Gets the unique int ID for the transaction on the Coordinating server
     *
     * @return the unique int ID for the transaction on the Coordinating server
     */
    public int getTransactionIndex() { return this.index; }

    @Override
    public String toString() {
        // print the transaction as a string with the operation, the key, and the unique message index
        // for this transaction
        return ThreadSafeStringFormatter.format(
                "%s %s %d",
                this.op.toString(),
                this.key,
                this.index
        );
    }
}

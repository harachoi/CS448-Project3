package simpledb.tx.concurrency;

import simpledb.tx.Transaction;

public class LockTableWaitDie extends LockTable {
    @Override
    void initialize(Object... dataItems) {
        // does not need to be initialized
    }

    @Override
    void handleIncompatible(Transaction waiting, Transaction holding, LockEntry entry) throws InterruptedException {
        if (waiting.getTimestamp() < holding.getTimestamp()) {
            // waiting older
            wait();
        } else {
            // waiting younger
            throw new InterruptedException();
        }
    }

    @Override
    void handleUnlock(Transaction transaction) {
        // nothing
    }
}

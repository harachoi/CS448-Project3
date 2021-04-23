package simpledb.tx.concurrency;

import simpledb.tx.Transaction;

import java.util.List;
import java.util.concurrent.locks.Lock;

public class LockTableWaitDie extends LockTable {
    @Override
    void initialize(Object... dataItems) {
        // does not need to be initialized
    }

    @Override
    synchronized void handleIncompatible(Transaction waiting, List<LockEntry> holding, LockEntry entry) throws InterruptedException {
        if (isOldest(waiting, holding)) {
            // waiting older
            wait();
        } else {
            // waiting younger
            waiting.abort();
        }
    }

    boolean isOldest(Transaction waiting, List<LockEntry> holding) {
        for (LockEntry entry : holding) {
            if (waiting.getTimestamp() > entry.transaction.getTimestamp())
                return false;
        }
        return true;
    }

    @Override
    void handleUnlock(Transaction transaction) {
        // nothing
    }
}

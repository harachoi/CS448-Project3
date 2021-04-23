package simpledb.tx.concurrency;

import simpledb.tx.Transaction;

import java.util.ArrayList;
import java.util.List;

public class LockTableWoundWait extends LockTable {
    @Override
    void initialize(Object... dataItems) {
        // does not need to be initialized
    }

    @Override
    synchronized void handleIncompatible(Transaction waiting, List<LockEntry> holding, LockEntry entry) throws InterruptedException {
        List<LockEntry> younger = getYounger(waiting, holding);
        if (younger.size() == 0) {
            wait();
        } else {
            for (LockEntry y : younger) {
                y.transaction.abort();
            }
        }
    }

    List<LockEntry> getYounger(Transaction waiting, List<LockEntry> holding) {
        List<LockEntry> younger = new ArrayList<>();
        for (LockEntry entry : holding) {
            if (entry.transaction.getTimestamp() >= waiting.getTimestamp())
                younger.add(entry);
        }
        return younger;
    }

    @Override
    void handleUnlock(Transaction transaction) {
        // nothing
    }
}

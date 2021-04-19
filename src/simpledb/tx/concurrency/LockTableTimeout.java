package simpledb.tx.concurrency;

import simpledb.tx.Transaction;

public class LockTableTimeout extends LockTable {
    public static long MAX_TIME = 5000; // 5 seconds

    @Override
    void initialize(Object... dataItems) {
        // does not need to be initialized
    }

    @Override
    synchronized void handleIncompatible(Transaction waiting, Transaction holding, LockEntry entry) throws InterruptedException{
        long timestamp = System.currentTimeMillis();
        while (!entry.granted && !waitingTooLong(timestamp))
            wait(MAX_TIME);
        if (!entry.granted)
            waiting.abort();
    }

    @Override
    void handleUnlock(Transaction transaction) {
        // nothing
    }

    private boolean waitingTooLong(long starttime) {
        return System.currentTimeMillis() - starttime > MAX_TIME;
    }
}

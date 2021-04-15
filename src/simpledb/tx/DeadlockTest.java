package simpledb.tx;

import simpledb.buffer.BufferMgr;
import simpledb.file.BlockId;
import simpledb.file.FileMgr;
import simpledb.log.LogMgr;
import simpledb.server.SimpleDB;
import simpledb.tx.concurrency.*;

public class DeadlockTest {
    private static final String PADDING = "\t\t\t\t\t";

    private static FileMgr fm;
    private static LogMgr lm;
    private static BufferMgr bm;

    public static void main(String[] args) {
        //initialize the database system
        SimpleDB db = new SimpleDB("deadlocktest", 400, 8);
        fm = db.fileMgr();
        lm = db.logMgr();
        bm = db.bufferMgr();
        ConcurrencyMgr.locktbl = new LockTableGraph();
        new Thread(new T1()).start();
        new Thread(new T2()).start();
    }

    static class T1 implements Runnable {
        public void doTask(Transaction t1) throws InterruptedException {
            BlockId A = new BlockId("testfile", 1);
            BlockId B = new BlockId("testfile", 2);
            t1.pin(A);
            t1.pin(B);

            System.out.println("T1: lock-X(B)");
            t1.setInt(B, 0, 0, false);
            Thread.sleep(1000);
            System.out.println("T1: lock-X(A)");
            t1.setInt(A, 0, 0, false);
            System.out.println("T1: commit");
            t1.commit();
        }
        @Override
        public void run() {
            Transaction t1 = new Transaction(fm, lm, bm);
            t1.setThread(Thread.currentThread());
            t1.setTask((t) -> {
                while (true) {
                    try {
                        doTask(t);
                        break;
                    } catch (InterruptedException | LockAbortException e) {
                        // aborted
                        System.out.println("T1: rollback");
                        t.rollback();
                    }
                }
            });
            t1.runTask();
        }
    }

    static class T2 implements Runnable {
        public void doTask(Transaction t2) throws InterruptedException {
            BlockId A = new BlockId("testfile", 1);
            BlockId B = new BlockId("testfile", 2);
            t2.pin(A);
            t2.pin(B);
            Thread.sleep(50);
            System.out.println(PADDING + "T2: lock-S(A)");
            t2.getInt(A, 0);
            System.out.println(PADDING + "T2: lock-S(B)");
            t2.getInt(B, 0);
            System.out.println(PADDING + "T2: commit");
            t2.commit();
        }

        @Override
        public void run() {
            Transaction t2 = new Transaction(fm, lm, bm);
            t2.setThread(Thread.currentThread());
            t2.setTask((t) -> {
                while (true) {
                    try {
                        doTask(t);
                        break;
                    } catch (InterruptedException | LockAbortException e) {
                        // aborted
                        System.out.println(PADDING + "T2: rollback");
                        t.rollback();
                    }
                }
            });
            t2.runTask();
        }
    }
}

package simpledb.tx;

import simpledb.buffer.BufferMgr;
import simpledb.file.BlockId;
import simpledb.file.FileMgr;
import simpledb.log.LogMgr;
import simpledb.server.SimpleDB;
import simpledb.tx.concurrency.LockAbortException;

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
        new Thread(new T1()).start();
        new Thread(new T2()).start();
    }

    static class T1 implements Runnable {
        public void doTask(Transaction t1) throws InterruptedException {
            try {
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
                System.out.print(PADDING + PADDING + "<");
                t1.commit();
            } catch (LockAbortException e) {
                System.out.println("T1: rollback");
                System.out.print(PADDING + PADDING + "<");
                t1.rollback();
                doTask(t1);
            }
        }
        @Override
        public void run() {
            try {
                Transaction t1 = new Transaction(fm, lm, bm);
                doTask(t1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class T2 implements Runnable {
        public void doTask(Transaction t2) throws InterruptedException {
            try {
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
                System.out.print(PADDING + PADDING + "<");
                t2.commit();
            } catch (LockAbortException e) {
                System.out.println(PADDING + "T2: rollback");
                System.out.print(PADDING + PADDING + "<");
                t2.rollback();
                doTask(t2);
            }
        }

        @Override
        public void run() {
            try {
                Transaction t2 = new Transaction(fm, lm, bm);
                doTask(t2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

package simpledb.tx;

import simpledb.buffer.BufferMgr;
import simpledb.file.BlockId;
import simpledb.file.FileMgr;
import simpledb.log.LogMgr;
import simpledb.server.SimpleDB;
import simpledb.tx.concurrency.*;

import java.io.File;

public class Part1Test1 {
    private static FileMgr fm;
    private static LogMgr lm;
    private static BufferMgr bm;

    public static String FILE_NAME = "part1test1";

    public static int BLOCKS = 30;
    public static int READ_TIME = 50;
    public static int WRITE_TIME = 1500;

    public static void delete(File f) {
        if (f.isDirectory()) {
            for (File ff : f.listFiles())
                delete(ff);
        }
        f.delete();
    }

    public static void main(String[] args) throws InterruptedException {
        //initialize the database system
        Transaction.VERBOSE = false;

        delete(new File(FILE_NAME));
        SimpleDB db = new SimpleDB(FILE_NAME, BLOCKS * 400, BLOCKS * 2);
        fm = db.fileMgr();
        lm = db.logMgr();
        bm = db.bufferMgr();
        LockTableTimeout.MAX_TIME = 1000;
        ConcurrencyMgr.locktbl = new LockTableTimeout();

        new Thread(new ReadTransaction()).start();
        for (int i = 0; i < BLOCKS; i++) {
            new Thread(new WriteTransaction(i)).start();
        }
    }

    static class ReadTransaction implements Runnable {
        public void doTask(Transaction t1) throws InterruptedException {
            BlockId[] blocks = new BlockId[BLOCKS];
            for (int i = 0; i < BLOCKS; i++) {
                blocks[i] = new BlockId("testfile", i);
                t1.pin(blocks[i]);
            }
            for (int i = 0; i < BLOCKS; i++) {
                t1.getInt(blocks[i], 0);
                System.out.printf("ReadTransaction: read %d\n", i);
                Thread.sleep(READ_TIME);
            }
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
                        System.out.println("ReadTransaction: rollback");
                        t.rollback();
                    }
                }
            });
            t1.runTask();
        }
    }

    static class WriteTransaction implements Runnable {
        private int block;

        public WriteTransaction(int block) {
            this.block = block;
        }

        public void doTask(Transaction t1) throws InterruptedException {
            BlockId b = new BlockId("testfile", block);
            t1.pin(b);
            t1.setInt(b, 0, 0, false);
            System.out.printf("WriteTransaction: write %d\n", block);
            Thread.sleep(WRITE_TIME);
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
                        System.out.println("WriteTransaction: rollback");
                        t.rollback();
                    }
                }
            });
            t1.runTask();
        }
    }
}

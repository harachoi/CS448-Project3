package simpledb.tx;

import simpledb.buffer.BufferMgr;
import simpledb.file.BlockId;
import simpledb.file.FileMgr;
import simpledb.log.LogMgr;
import simpledb.server.SimpleDB;
import simpledb.tx.concurrency.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class Part1Test1 {
    enum LockTableType {
        TIMEOUT,
        WAIT_DIE,
        WOUND_WAIT,
        GRAPH
    };

    public static String FILE_NAME = "part1test1";

    public static int BLOCKS = 10;
    public static int READ_TIME = 50;
    public static int WRITE_TIME = 200;

    public static String OUT_FILE = "part1test1-out/time.txt";
    public static int TRIALS = 10;

    public static long timer;

    public static void delete(File f) throws IOException {
        if (!f.exists())
            return;
        if (f.isDirectory()) {
            for (File ff : f.listFiles())
                delete(ff);
        }
        if (!f.delete()) {
            System.out.println("not deleted");
            throw new IOException();
        }
    }

    public static void startTimer() {
        timer = System.currentTimeMillis();
    }

    public static long runTest(LockTableType type) throws InterruptedException, IOException {
        String filename = FILE_NAME + "-" + type;
        if (new File(filename).exists()) {
            delete(new File(filename));
        }
        SimpleDB db = new SimpleDB(filename, 800, BLOCKS * 3);
        switch (type) {
            case TIMEOUT: {
                ConcurrencyMgr.locktbl = new LockTableTimeout();
                break;
            }
            case WAIT_DIE: {
                ConcurrencyMgr.locktbl = new LockTableWaitDie();
                break;
            }
            case WOUND_WAIT: {
                ConcurrencyMgr.locktbl = new LockTableWoundWait();
                break;
            }
            case GRAPH: {
                ConcurrencyMgr.locktbl = new LockTableGraph();
                break;
            }
        }

        Thread[] threads = new Thread[BLOCKS + 1];
        int rollbacks = 0;
        startTimer();
        ReadTransaction rt = new ReadTransaction(db.fileMgr(), db.logMgr(), db.bufferMgr());
        threads[0] = new Thread(rt);
        threads[0].start();
        Thread.sleep(10);
        for (int i = 0; i < BLOCKS; i++) {
            WriteTransaction wt = new WriteTransaction(i, db.fileMgr(), db.logMgr(), db.bufferMgr());
            threads[i + 1] = new Thread(wt);
            threads[i + 1].start();
        }
        for (int i = 0; i < BLOCKS + 1; i++) {
            threads[i].join();
        }
        long time = System.currentTimeMillis() - timer;
        db.fileMgr().closeAll();
        delete(new File(filename));
        return time;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        //initialize the database system
        Transaction.VERBOSE = false;
        LockTableTimeout.MAX_TIME = 100;

        PrintWriter pw = new PrintWriter(new FileOutputStream(OUT_FILE, false));

        for (int i = 10; i <= 50; i += 5) {
            BLOCKS = i;
            pw.println(BLOCKS);
            for (LockTableType type : LockTableType.values()) {
                System.out.println("running " + type + ", blocks: " + BLOCKS);

                long totalTime = 0;
                long minTime = Long.MAX_VALUE;
                long maxTime = Long.MIN_VALUE;
                for (int j = 0; j < TRIALS; j++) {
                    long time = runTest(type);
                    totalTime += time;
                    if (time < minTime)
                        minTime = time;
                    if (time > maxTime)
                        maxTime = time;
                }
                double average = totalTime / (double) TRIALS;
                pw.printf("%d %.2f %d\n", minTime, average, maxTime);

                System.out.printf("%d %.2f %d\n", minTime, average, maxTime);
            }
            pw.flush();
        }
    }

    static class ReadTransaction implements Runnable {
        private FileMgr fm;
        private LogMgr lm;
        private BufferMgr bm;

        public int rollbacks;

        public ReadTransaction(FileMgr fm, LogMgr lm, BufferMgr bm) {
            this.fm = fm;
            this.lm = lm;
            this.bm = bm;
        }

        public void doTask(Transaction t1) throws InterruptedException {
            BlockId[] blocks = new BlockId[BLOCKS];
            for (int i = 0; i < BLOCKS; i++) {
                blocks[i] = new BlockId("testfile", i);
                t1.pin(blocks[i]);
            }
            for (int i = 0; i < BLOCKS; i++) {
                t1.getInt(blocks[i], 0);
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
                        rollbacks++;
                        t.rollback();
                    }
                }
            });
            try {
                t1.runTask();
            } catch (Exception ex) {

            }
        }
    }

    static class WriteTransaction implements Runnable {
        private int block;
        private FileMgr fm;
        private LogMgr lm;
        private BufferMgr bm;

        public int rollbacks = 0;

        public WriteTransaction(int block, FileMgr fm, LogMgr lm, BufferMgr bm) {
            this.block = block;
            this.fm = fm;
            this.lm = lm;
            this.bm = bm;
        }

        public void doTask(Transaction t1) throws InterruptedException {
            Thread.sleep((int)(Math.random() * 50));
            BlockId b = new BlockId("testfile", block);
            t1.pin(b);
            t1.setInt(b, 0, 0, false);
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
                        rollbacks++;
                        t.rollback();
                    }
                }
            });
            try {
                t1.runTask();
            } catch (Exception ex) {

            }
        }
    }
}

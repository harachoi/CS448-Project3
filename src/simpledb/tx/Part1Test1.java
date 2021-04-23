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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Part1Test1 {

    public static FileMgr fm;
    public static LogMgr lm;
    public static BufferMgr bm;

    public static String FILE_NAME = "part1test1";

    // Change these
    public static int BLOCKS = 10; // starting with BLOCKS blocks
    public static int MAX_BLOCKS = 50; // until MAX_BLOCKS blocks
    public static int READ_TIME = 15; // time to read a data item
    public static int WRITE_TIME = 2; // time to write a data item
    public static int TIMEOUT = 1; // timeout for LockTableTimeout
    public static boolean RANDOM_WRITE_SEQ = true; // write in random order
    public static int RANDOM_WRITE_MAX = 50; // write waits for 0~RANDOM_WRITE_MAX seconds randomly

    public static String OUT_FILE = "part1test1-out/time.txt";
    public static int TRIALS = 10;

    public static long timer;

    public static AtomicInteger readRollbacks = new AtomicInteger();
    public static AtomicInteger writeRollbacks = new AtomicInteger();

    public static List<Integer> randTime = new ArrayList<>();

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

    public static long runTest(LockTable.LockTableType type) throws InterruptedException, IOException {
        String filename = FILE_NAME + "-" + type;
        if (new File(filename).exists()) {
            delete(new File(filename));
        }
        SimpleDB db = new SimpleDB(filename, 400 * BLOCKS, BLOCKS * 2);
        fm = db.fileMgr();
        lm = db.logMgr();
        bm = db.bufferMgr();

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
//            case GRAPH: {
//                ConcurrencyMgr.locktbl = new LockTableGraph();
//                break;
//            }
        }

        Thread[] threads = new Thread[BLOCKS + 1];
        startTimer();
        threads[0] = new Thread(new ReadTransaction(System.currentTimeMillis()));
        threads[0].start();
        for (int i = 0; i < BLOCKS; i++) {
            threads[i + 1] = new Thread(new WriteTransaction(System.currentTimeMillis(), i + 1));
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
        LockTableTimeout.MAX_TIME = TIMEOUT;

        OUT_FILE = String.format("part1test1-out/r%dw%dt%drd%d%s.txt",
                READ_TIME,
                WRITE_TIME,
                TIMEOUT,
                RANDOM_WRITE_MAX,
                RANDOM_WRITE_SEQ ? "T" : "F");
        PrintWriter pw = new PrintWriter(new FileOutputStream(OUT_FILE, false));

        for (int i = 10; i <= MAX_BLOCKS; i += 5) {
            BLOCKS = i;
            pw.println(BLOCKS);
            randTime.clear();
            for (int j = 0; j < BLOCKS + 1; j++) {
                randTime.add((int)(Math.random() * RANDOM_WRITE_MAX));
            }
            for (LockTable.LockTableType type : LockTable.LockTableType.values()) {
                System.out.println("running " + type + ", blocks: " + BLOCKS);

                long totalTime = 0;
                long minTime = Long.MAX_VALUE;
                long maxTime = Long.MIN_VALUE;

                int totalReadRollbacks = 0;
                int totalWriteRollbacks = 0;
                for (int j = 0; j < TRIALS; j++) {
                    readRollbacks.set(0);
                    writeRollbacks.set(0);
                    long time = runTest(type);
                    totalReadRollbacks += readRollbacks.get();
                    totalWriteRollbacks += writeRollbacks.get();
                    totalTime += time;
                    if (time < minTime)
                        minTime = time;
                    if (time > maxTime)
                        maxTime = time;
                }
                double average = totalTime / (double) TRIALS;
                pw.printf("%d %.2f %d %d %d\n", minTime, average, maxTime, totalReadRollbacks, totalWriteRollbacks);

                System.out.printf("%d %.2f %d %d %d\n", minTime, average, maxTime, totalReadRollbacks, totalWriteRollbacks);
            }
            pw.flush();
        }
    }

    static class ReadTransaction implements Runnable {
        private long timestamp;

        public ReadTransaction(long timestamp) {
            this.timestamp = timestamp;
        }

        public void doTask(Transaction t1) throws InterruptedException {
            BlockId[] blocks = new BlockId[BLOCKS];
            for (int i = 0; i < BLOCKS; i++) {
                blocks[i] = new BlockId("testfile", i + 1);
                t1.pin(blocks[i]);
            }
            for (int i = 0; i < BLOCKS; i++) {
                t1.getInt(blocks[i], 0);
                Thread.sleep(READ_TIME);
            }
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            t1.releaseAll();
        }

        @Override
        public void run() {
            Transaction t1 = new Transaction(fm, lm, bm);
            t1.setTimestamp(1);
            t1.setThread(Thread.currentThread());
            t1.setTask((t) -> {
                try {
                    doTask(t);
                } catch (InterruptedException | LockAbortException e) {
                    // aborted
                    while (true) {
                        readRollbacks.incrementAndGet();
                        t1.releaseAll();
                        Thread new1 = new Thread(new ReadTransaction(timestamp));
                        new1.start();
                        try {
                            new1.join();
                            break;
                        } catch (InterruptedException interruptedException) {
                        }
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
        private long timestamp;
        private int block;

        public WriteTransaction(long timestamp, int block) {
            this.timestamp = timestamp;
            this.block = block;
        }

        public void doTask(Transaction t2) throws InterruptedException {
            if (RANDOM_WRITE_SEQ)
                Thread.sleep(randTime.get(block));
            else
                Thread.sleep(block);
            BlockId b = new BlockId("testfile", block + 1);
            t2.pin(b);
            t2.setInt(b, 0, 0, false);
            Thread.sleep(WRITE_TIME);
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            t2.releaseAll();
        }

        @Override
        public void run() {
            Transaction t2 = new Transaction(fm, lm, bm);
            t2.setTimestamp(timestamp);
            t2.setThread(Thread.currentThread());
            t2.setTask((t) -> {
                try {
                    doTask(t);
                } catch (InterruptedException | LockAbortException e) {
                    // aborted
                    while (true) {
                        writeRollbacks.incrementAndGet();
                        t2.releaseAll();
                        Thread new2 = new Thread(new WriteTransaction(timestamp, block));
                        new2.start();
                        try {
                            new2.join();
                            break;
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                }
            });
            try {
                t2.runTask();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

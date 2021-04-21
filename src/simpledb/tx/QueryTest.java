package simpledb.tx;

import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.concurrency.ConcurrencyMgr;
import simpledb.tx.concurrency.LockAbortException;
import simpledb.tx.concurrency.LockTable;

import java.io.File;
import java.io.IOException;

public class QueryTest {
    public static SimpleDB db;
    public static String DIR_NAME = "querytest1";

    public static int RECORDS = 1;

    public static void main(String[] args) throws IOException, InterruptedException {
        SimpleDB.BUFFER_SIZE = 100;
        SimpleDB.BLOCK_SIZE = 400;

        for (LockTable.LockTableType type :LockTable.LockTableType.values()) {
            runTest(type);
        }
    }

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

    public static void runTest(LockTable.LockTableType type) throws IOException, InterruptedException {
        String filename = DIR_NAME + "-" + type;
//        File f = new File(filename);
//        delete(f);
        ConcurrencyMgr.locktbl = LockTable.getLockTable(type);
        System.out.println(filename);

        db = new SimpleDB(filename);
        Transaction tx = db.newTx();
        Planner planner = db.planner();

        System.out.println("create table");
        String cmd = "create table T1(A int, B varchar(9))";
        planner.executeUpdate(cmd, tx);
        for (int i=0; i<RECORDS; i++) {
            int a = i;
            String b = "bbb"+a;
            cmd = "insert into T1(A,B) values(" + a + ", '"+ b + "')";
            planner.executeUpdate(cmd, tx);
        }
        tx.commit();

        System.out.println("start");
        Thread[] threads = new Thread[RECORDS + 1];
        threads[0] = new Thread(new T1());
        for (int i = 0; i < RECORDS; i++) {
            threads[i + 1] = new Thread(new T2(i));
        }

        for (int i = 0; i < RECORDS + 1; i++) {
            threads[i].start();
        }
        for (int i = 0; i < RECORDS + 1; i++) {
            threads[i].join();
        }
        System.out.println("done");
//        db.fileMgr().closeAll();
//        delete(f);
    }

    public static class T1 implements Runnable {

        @Override
        public void run() {
            Transaction tx2 = db.newTx();
            tx2.setThread(Thread.currentThread());
            try {
                String qry = "select B from T1";
                Plan p = db.planner().createQueryPlan(qry, tx2);
                Scan s = p.open();
                while (s.next())
//                    System.out.println(s.getString("b"));
                    s.getString("b");
                s.close();
                if (Thread.currentThread().isInterrupted())
                    throw new LockAbortException();
                tx2.commit();
            } catch (Exception ex) {
                ex.printStackTrace();

                tx2.releaseAll();
                Thread t1 = new Thread(new T1());
                t1.start();
                try {
                    t1.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class T2 implements Runnable {
        private int a;

        public T2(int a) {
            this.a = a;
        }

        @Override
        public void run() {
            Transaction tx3 = db.newTx();
            tx3.setThread(Thread.currentThread());
            try {
                String upd = "update T1 set A=1 where A=" + a;
                db.planner().executeUpdate(upd, tx3);
                if (Thread.currentThread().isInterrupted())
                    throw new LockAbortException();
                tx3.commit();
            } catch (Exception ex) {
                ex.printStackTrace();
                tx3.releaseAll();
                Thread t2 = new Thread(new T2(a));
                t2.start();
                try {
                    t2.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

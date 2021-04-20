package simpledb.tx;

import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.Scan;
import simpledb.server.SimpleDB;
import simpledb.tx.concurrency.ConcurrencyMgr;

public class QueryTest {
    public static SimpleDB db;

    public static void main(String[] args) {
        Transaction.VERBOSE = true;
        db = new SimpleDB("plannertest2");
        Transaction tx = db.newTx();
        Planner planner = db.planner();

        String cmd = "create table T1(A int, B varchar(9))";
        planner.executeUpdate(cmd, tx);
        int n = 200;
        System.out.println("Inserting " + n + " records into T1.");
        for (int i=0; i<n; i++) {
            int a = i;
            String b = "bbb"+a;
            cmd = "insert into T1(A,B) values(" + a + ", '"+ b + "')";
            planner.executeUpdate(cmd, tx);
        }
        tx.commit();

        new Thread(new T1()).start();
        new Thread(new T2()).start();
    }

    public static class T1 implements Runnable {

        @Override
        public void run() {
            Transaction tx2 = db.newTx();
            try {
                String qry = "select B from T1";
                Plan p = db.planner().createQueryPlan(qry, tx2);
                Scan s = p.open();
                while (s.next())
                    System.out.println(s.getString("b"));
                s.close();
                tx2.commit();
            } catch (Exception ex) {
                tx2.releaseAll();
                new Thread(new T1()).start();
            }
        }
    }

    public static class T2 implements Runnable {

        @Override
        public void run() {
            Transaction tx3 = db.newTx();
            try {
                String upd = "update T1 set A=1 where A=1";
                db.planner().executeUpdate(upd, tx3);
                tx3.commit();
            } catch (Exception ex) {
                tx3.releaseAll();
                new Thread(new T2()).start();
            }
        }
    }
}

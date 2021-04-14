package simpledb.tx.concurrency;

import java.util.*;
import java.util.concurrent.locks.Lock;

import simpledb.file.BlockId;
import simpledb.tx.Transaction;

/**
 * The lock table, which provides methods to lock and unlock blocks.
 * If a transaction requests a lock that causes a conflict with an
 * existing lock, then that transaction is placed on a wait list.
 * There is only one wait list for all blocks.
 * When the last lock on a block is unlocked, then all transactions
 * are removed from the wait list and rescheduled.
 * If one of those transactions discovers that the lock it is waiting for
 * is still locked, it will place itself back on the wait list.
 * @author Edward Sciore
 */
class LockTable2 {
   enum LockType {
      UNLOCKED,
      SHARED,
      EXCLUSIVE
   }
   class LockEntry {
      Transaction transaction;
      LockType lockType;
      boolean granted;

      public LockEntry(Transaction transaction, LockType lockType, boolean granted) {
         this.transaction = transaction;
         this.lockType = lockType;
         this.granted = granted;
      }
   }

   private static final long MAX_TIME = 10000; // 10 seconds
   
   private Map<BlockId,LinkedList<LockEntry>> locks = new HashMap<>();
   
   /**
    * Grant an SLock on the specified block.
    * If an XLock exists when the method is called,
    * then the calling thread will be placed on a wait list
    * until the lock is released.
    * If the thread remains on the wait list for a certain 
    * amount of time (currently 10 seconds),
    * then an exception is thrown.
    * @param blk a reference to the disk block
    */
   public synchronized void sLock(Transaction transaction, BlockId blk) {
      try {
         if (!locks.containsKey(blk)) {
            locks.put(blk, new LinkedList<>());
         }
         LockEntry entry = new LockEntry(transaction, LockType.SHARED, compatible(blk, LockType.SHARED));
         locks.get(blk).add(entry);

         long timestamp = System.currentTimeMillis();
         while (!entry.granted && !waitingTooLong(timestamp))
            wait(MAX_TIME);
         if (!entry.granted)
            throw new LockAbortException();
      }
      catch(InterruptedException e) {
         throw new LockAbortException();
      }
   }
   
   /**
    * Grant an XLock on the specified block.
    * If a lock of any type exists when the method is called,
    * then the calling thread will be placed on a wait list
    * until the locks are released.
    * If the thread remains on the wait list for a certain 
    * amount of time (currently 10 seconds),
    * then an exception is thrown.
    * @param blk a reference to the disk block
    */
   synchronized void xLock(Transaction transaction, BlockId blk) {
      try {
         if (!locks.containsKey(blk)) {
            locks.put(blk, new LinkedList<>());
         }
         LockEntry entry = new LockEntry(transaction, LockType.EXCLUSIVE, compatible(blk, LockType.EXCLUSIVE));
         locks.get(blk).add(entry);

         long timestamp = System.currentTimeMillis();
         while (!entry.granted && !waitingTooLong(timestamp))
            wait(MAX_TIME);
         if (!entry.granted)
            throw new LockAbortException();
      }
      catch(InterruptedException e) {
         throw new LockAbortException();
      }
   }
   
   /**
    * Release a lock on the specified block.
    * If this lock is the last lock on that block,
    * then the waiting transactions are notified.
    * @param blk a reference to the disk block
    */
   synchronized void unlock(Transaction transaction, BlockId blk) {
      Iterator<LockEntry> it = locks.get(blk).iterator();
      while (it.hasNext()) {
         LockEntry entry = it.next();
         if (entry.transaction.equals(transaction)) {
            it.remove();
            if (locks.get(blk).size() == 0) {
               // no other transaction waiting
               locks.remove(blk);
               return;
            }
            if (lockedIn(blk) == LockType.UNLOCKED) {
               // all lock entries are waiting, grant lock to first in queue
               locks.get(blk).peekFirst().granted = true;
               notifyAll();
            }
         }
      }
   }

   synchronized public LockType lockedIn(BlockId blk) {
      if (!locks.containsKey(blk))
         return LockType.UNLOCKED;
      for (LockEntry entry : locks.get(blk)) {
         if (entry.granted)
            return entry.lockType;
      }
      return LockType.UNLOCKED;
   }

   // implements the compatibility matrix of S and X
   synchronized public boolean compatible(BlockId blk, LockType lockType) {
      LockType blkLock = lockedIn(blk);
      // any lock compatible with no lock
      if (blkLock == LockType.UNLOCKED)
         return true;
      // S compatible with S
      if (lockType == LockType.SHARED && blkLock == LockType.SHARED) {
         return true;
      }
      // otherwise not compatible
      return false;
   }
   
   private boolean waitingTooLong(long starttime) {
      return System.currentTimeMillis() - starttime > MAX_TIME;
   }

}

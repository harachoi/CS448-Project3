package simpledb.tx.concurrency;

import java.util.*;

import simpledb.file.BlockId;
import simpledb.tx.Transaction;

abstract class LockTable {
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

      @Override
      public String toString() {
         return "LockEntry{" +
                 "transaction=" + transaction +
                 ", lockType=" + lockType +
                 ", granted=" + granted +
                 '}';
      }
   }
   
   protected Map<BlockId,LinkedList<LockEntry>> locks = new HashMap<>();

   // initialize data items, used for tree protocol
   abstract void initialize(Object... dataItems);

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
      if (!locks.containsKey(blk)) {
         locks.put(blk, new LinkedList<>());
      }
      LockEntry entry = new LockEntry(transaction, LockType.SHARED, compatible(blk, LockType.SHARED));
      locks.get(blk).add(entry);

      if (!entry.granted) {
         try {
            handleIncompatible(transaction, currentlyHolding(blk), entry);
         } catch (InterruptedException e) {
            throw new LockAbortException();
         }
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
      if (!locks.containsKey(blk)) {
         locks.put(blk, new LinkedList<>());
      }
      LockEntry entry = new LockEntry(transaction, LockType.EXCLUSIVE, compatible(blk, LockType.EXCLUSIVE));
      locks.get(blk).add(entry);

      if (!entry.granted) {
         try {
            Transaction current = currentlyHolding(blk);
            if (current.equals(transaction)) {
               upgrade(blk, transaction);
               return;
            }

            handleIncompatible(transaction, current, entry);
         } catch (InterruptedException e) {
            throw new LockAbortException();
         }
      }
   }

   // when lock cannot be granted, this function is called
   abstract void handleIncompatible(Transaction waiting, Transaction holding, LockEntry entry) throws InterruptedException;
   
   /**
    * Release a lock on the specified block.
    * If this lock is the last lock on that block,
    * then the waiting transactions are notified.
    * @param blk a reference to the disk block
    */
   synchronized void unlock(Transaction transaction, BlockId blk) {
//      System.out.println(transaction.txnum + " unlock " + blk.number());
      handleUnlock(transaction);
      Iterator<LockEntry> it = locks.get(blk).iterator();
      while (it.hasNext()) {
         LockEntry entry = it.next();
         if (entry.transaction.equals(transaction)) {
            it.remove(); // prevent ConcurrentModificationException
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

   synchronized public void upgrade(BlockId blk, Transaction transaction) {
      for (LockEntry entry : locks.get(blk)) {
         if (entry.transaction.equals(transaction) && entry.lockType == LockType.SHARED) {
            entry.lockType = LockType.EXCLUSIVE;
            return;
         }
      }
   }

   abstract void handleUnlock(Transaction transaction);

   synchronized public LockType lockedIn(BlockId blk) {
      if (!locks.containsKey(blk))
         return LockType.UNLOCKED;
      for (LockEntry entry : locks.get(blk)) {
         if (entry.granted)
            return entry.lockType;
      }
      return LockType.UNLOCKED;
   }

   synchronized public Transaction currentlyHolding(BlockId blk) {
      for (LockEntry entry : locks.get(blk)) {
         if (entry.granted)
            return entry.transaction;
      }
      return null;
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


}

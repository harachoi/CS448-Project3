# Changes
## Created class `DeadlockTest`
- Implements figure 18.7 (schedule 2) in the textbook
- enforces order by using `Thread.sleep()`
- when deadlock is detected, roll back

## Changed the lock table implementation
- new class `LockTable2`
- lock table is a hash table indexed on the block
- Each entry of the lock table stores a linked list
- Each entry of the linked list stores the lock request
  - transaction: `Transaction transaction`
  - type of lock: `LockType lockType`
```java
enum LockType {
  UNLOCKED,  
  SHARED,
  EXCLUSIVE
}
```
- stores an extra boolean `granted` to indicate whether the lock is granted

## Requesting locks
- calls to `sLock()` and `xLock()` now require an additional parameter of type `Transaction`
- add request to block entry
- when unlock, check if any other waiting transactions can be granted
- transactions wait for the lock they requested to be granted

## Timestamp
- added a `timestamp` variable that is set automatically when the transaction is created
- retrieve this variable with `getTimestamp()`
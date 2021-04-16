# Changes
## Created class `DeadlockTest`
- Implements figure 18.7 (schedule 2) in the textbook
- enforces order by using `Thread.sleep()`
- when deadlock is detected, roll back

## Changed the lock table implementation
- made it abstract
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
- all deadlock detection/prevention implementation should extend this class

## Requesting locks
- calls to `sLock()` and `xLock()` now require an additional parameter of type `Transaction`
- add request to block entry
- when unlock, check if any other waiting transactions can be granted
- transactions wait for the lock they requested to be granted

## Using the new `LockTable`
- example of this can be seen in 
  - `LockTableOriginal`: the timeout based deadlock detection
  - `LockTableWaitDie`: the wait-die implementation
  - `LockTableWoundWait`: the wound-wait implementation
  - `LockTableGraph`: the wait-for graph implementation
  
- when a lock is requested that is incompatible with the current lock held on a block, `handleIncompatible()` is called
  - `Transaction waiting`: the transaction that requested the lock
  - `Transaction holding`: the transaction that is currently holding the lock
  - `LockEntry entry`: contains information about the block (who is holding, who is waiting, what kind of lock)
- `initialize()` is currently useless, since it is only used in the tree protocol, which I have not yet implemented
- `handleUnlock()` is called when a transaction unlocks, it is currently only used by `LockTableGraph`

## Timestamp
- added a `timestamp` variable that is set automatically when the transaction is created
- retrieve this variable with `getTimestamp()`
- added a `rollbackTime` that is used to determine the victim when using the graph based approach
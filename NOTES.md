# Notes
- search for `?` for undecided implementation plan

## useful files
- `ConcurrencyTest.java`
- `Transaction.java`
- `ConcurrencyMgr.java`
- `LockTable.java`

## general structure

### `ConcurrencyTest`
- creates 3 threads
- thread 1(`A`)
  - get 2 S-locks, one on `blk1` and one on `blk2`
  - after receiving the first lock, wait for 1 sec  
  - releases all locks once done
  
- thread 2(`B`)
  - get an X-lock on `blk2`, and S-lock on `blk1`
  - after receiving the X-lock, wait for 1 sec
  - release all locks once done
  
- thread 3(`C`)
  - wait 500ms
  - get an X-lock on `blk1`, and S-lock on `blk2`
  - after receiving the X-lock, wait for 1 sec
  - release all locks once done
  
- referenced classes and functions
  - `Transaction`
    - `pin()`
    - `getInt()`
    - `setInt()`
    - `commit()`

## useful functions

### `sLock()` in `LockTable`
```java
public synchronized void sLock(BlockId blk)
```
- attempt to acquire an S-lock on block `blk`
- if the block has an X-lock, wait for a maximum of `MAX_TIME`
- `wait()` ends by
    - other threads call `notifyAll()`
    - timer runs out
- after `wait()` end
    - if the block still has an X-lock, throw a `LockAbortException`
    - otherwise, put S-lock of `blk` by the transaction into lock table

### `xLock()` in `LockTable`
```java
synchronized void xLock(BlockId blk)
```
- similar to `sLock()`, but conflict on all other locks

## useful variables

### `MAX_TIME`
```java
private static final long MAX_TIME = 10000; // 10 seconds
```
- max wait time

## Ideas for part 1

### Reading relation with concurrent writes
- one transaction performs read on all data in relation
- multiple writes on different data items (random or in order?)
- expected behavior of the detection & prevention methods
  - setup 1
    - t: tuples
    - T1 read sequentially (i.e t1,t2,t3,...,ti,...,tn)
      - lock-S, read, lock-S, read, ..., unlock, unlock, ...
    - T2, T3, T4,..., Tm: writes to a tuple
      - lock-X, write, unlock
    - TS(T1) < TS(other)
  - timeout
    - T1 reaches ti
      - if T2 is writing ti, T1 waits
      - if T2 is takes longer than `MAX_TIME`, T1 rolls back
    - T2 is writing ti
      - if T1 is still reading, T2 waits
    - worse case: T1 keeps rolling back
      - time: m * T(read)
    - average case: T1 rolls back some of the time
      - time: m * T(read), but less than worse case
    - best case: all read finishes before T1 reaches it
      - time: T(read all)
  - wait-die
    - T1 reaches ti
      - if T2 is writing ti, T1 waits
    - T2 writes ti
      - if T1 is still reading, T2 dies
    - worse case: serial, T1 waits on all write
      - time: T(read all) + m * T(write)
    - average case: some write completes, while some wait for T1 to finish reading
      - time: T(read all) + m / 2 * T(write)
    - best case: all write finishes before T1 needs to access those date items
      - time: T(read all)
  - wound-wait
    - T1 reaches ti
      - if T2 is writing ti, rollback T2
    - T2 writes ti
      - if T1 is still reading, T2 waits
    - same as wait-die?
  - wait-for graph
    - no deadlocks
    - worst case: T1 waits on every write
    - average case: T1 waits on some writes
    - best case: all writes finish before T1 could reach them  
    - time: (n + m) * overhead + T(operation)
      - (n + m) * overhead: for each operation, there is overhead for cycle detection and managing the graph
      - if the cycle detection is done every k operations, this is reduced to ceil(1/k * (n + m)) * overhead
      - also need to include the time for the operation
    - might be slower than wait-die/wound-wait because of overhead?
  - tree protocol
    - time: T(inital overhead) + (n + m) * overhead + T(operation)
    - T(initial overhead): time needed to build tree
    - (n + m) * overhead: time needed to check for validity of lock request
  

## Implementation plan

### setup
- T1 request item held by T2

### wait-die
- if T1 is older, it wait
- if T1 is younger, it dies

### wound-wait
- if T1 is younger, it wait
- if T1 is older, T2 dies

### wait-for graphs
- G = (V, E)
- V: all transactions in the system
- E: (Ti -> Tj)
  - Ti is waiting for Tj
- insert Eij when Ti insert data held by Tj
- remove Eij when Tj releases all data requested by Ti
- when to run cycle detection?
  - not a lot of data items, might run every insert
  - run every x inserts to reduce overhead

### tree protocol
- can only use X-locks
- obtain data items needed
- build tree (how to build?)
  - plan 1: random build
    - shuffle data item array D = {d1, d2, d3,...}
    - d1 is root
    - choose # children and add to d1
    - for each child (in a random order) of d1, perform the same thing
    - repeat until D is empty
- procedure
  - first lock can be on any item
  - Q can be locked only if Q’s parent is locked
  - Data item may be unlock at any time
  - Cannot relock (since the test follow 2PL, this cannot happen)

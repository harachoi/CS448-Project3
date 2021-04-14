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
  - setup
    - t: tuples
    - T1 read sequentially (i.e t1,t2,t3,...,ti,...,tn)
    - T2 writes in order, but not every tuple (e.g. t1,t5,t6,...,ti,...tm)
  - wait-die
    - T2 reaches ti first: if T1 reaches ti while T2 is writing, T1 waits
    - T1 reaches ti first: if T2 reaches ti while T1 is reading, T2 dies
  - wound-wait
    - T2 reaches ti first: if T1 reaches ti while T2 is writing, the writing fails
    - T1 reaches ti first: if T2 reaches ti while 

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

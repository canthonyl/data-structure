### Atomic Boolean Lock

Atomic Boolean Lock is a lock-free data structure that provides atomic access to code region/resource, and can serve as building blocks to other concurrent data structure.  The lock is reentrant - it requires that all repeated calls to acquire are followed by a call to release by the same thread.

An [Atomic Boolean](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/atomic/AtomicBoolean.html) is used to achieve atomicity by ensuring a particular section of code is not under execution before a thread enters.  The reentrant aspect is achieved through the use of a thread local counter to track number of lock acquisitions, allowing a 
thread which has already obtained the lock to pass through unhindered and only releasing the lock when all call stacks which has previously acquired the lock have returned with a call to release the lock.

For simple use case, place acquire and release in a try-finally block around the atomic code block to be accessed by multiple threads:

```java
private final AtomicBooleanLock lock = new AtomicBooleanLock();
private volatile int counter;

//using optimistic lock 
public int incrementAndGet(){
    try {
        lock.acquire();  
        count += 1;
        return count;
    } finally {
        lock.release();
    }
}

```
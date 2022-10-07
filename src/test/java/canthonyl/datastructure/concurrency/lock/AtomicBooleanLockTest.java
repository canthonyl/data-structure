package canthonyl.datastructure.concurrency.lock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AtomicBooleanLockTest {
    
    @Test
    public void testIncrementInt(){
        AtomicBooleanLock lock = new AtomicBooleanLock();
        Counter counter = new Counter();
        
        Runnable incrementCounter = () -> {
            for (int i=0; i<1000; i++) {
                try {
                    lock.acquire();
                    counter.increment();
                } finally {
                    lock.release();
                }
            }
        };

        Thread t0 = new Thread(incrementCounter);
        Thread t1 = new Thread(incrementCounter);
        Thread t2 = new Thread(incrementCounter);
        Thread t3 = new Thread(incrementCounter);
        Thread t4 = new Thread(incrementCounter);

        t0.start();
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        
        try {
            t0.join();
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (InterruptedException ie){
            ie.printStackTrace();
        }
        
        assertEquals(5000, counter.value);
    }

    @Test
    public void testLockReentrantly(){
        AtomicCounter atomicCounter = new AtomicCounter(50, 3000000);

        Runnable reentrantLockOps = () -> {
            for (int i=0; i<3000000/50/5 * 2; i++) {
                atomicCounter.operation();
            }
        };

        Thread t0 = new Thread(reentrantLockOps);
        Thread t1 = new Thread(reentrantLockOps);
        Thread t2 = new Thread(reentrantLockOps);
        Thread t3 = new Thread(reentrantLockOps);
        Thread t4 = new Thread(reentrantLockOps);

        t0.start();
        t1.start();
        t2.start();
        t3.start();
        t4.start();

        try {
            t0.join();
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (InterruptedException ie){
            ie.printStackTrace();
        }

        assertEquals(0, atomicCounter.value);
        assertTrue(atomicCounter.touched);
    }
    
    class Counter {
        private volatile int value;
        public Counter() {}
        public void increment() { value += 1; }
        public void decrement() { value -= 1; }

    }

    class AtomicCounter {
        private volatile int value;
        private volatile int numTimes;
        private volatile boolean touched;
        private final int max;
        private final int blocksize;
        private final AtomicBooleanLock lock;

        public AtomicCounter(int blocksizeVal, int maxVal) {
            lock = new AtomicBooleanLock();
            max = maxVal;
            blocksize = blocksizeVal;
        }

        public void increment(int change){
            try {
                lock.acquire();
                value += change;
            } finally {
                lock.release();
            }
        }

        public void decrement(int change){
            try {
                lock.acquire();
                value -= change;
            } finally {
                lock.release();
            }
        }

        public void operation(){
            try {
                lock.acquire();
                if (numTimes < max/blocksize) {
                    increment(blocksize);
                } else {
                    decrement(blocksize);
                }
                numTimes += 1;
                if (value == max){
                    touched = true;
                }
            } finally {
                lock.release();
            }
        }


    }
    
}

package canthonyl.datastructure.concurrency.lock;

import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicBooleanLock implements OptimisticLock {
    private final ThreadLocal<Integer> lockCount;
    private final AtomicBoolean bool;

    public AtomicBooleanLock() {
        lockCount = ThreadLocal.withInitial(() -> 0);
        bool = new AtomicBoolean();
    }

    @Override
    public void acquire() {
        while (!bool.compareAndSet(lockCount.get() > 0, true)){}
        lockCount.set(lockCount.get()+1);
    }

    @Override
    public void release(){
        lockCount.set(lockCount.get() - 1);
        if (lockCount.get() == 0) {
            bool.set(false);
        }
    }

}

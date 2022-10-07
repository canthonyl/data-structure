package canthonyl.datastructure.concurrency.lock;

public interface OptimisticLock {

    void acquire();

    void release();

}

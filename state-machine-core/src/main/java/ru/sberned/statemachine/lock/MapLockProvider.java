package ru.sberned.statemachine.lock;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jpatuk on 07/12/2016.
 */
public class MapLockProvider implements LockProvider {
    private LoadingCache<Object, Lock> cache = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<Object, Lock>() {
                @Override
                public Lock load(Object key) throws Exception {
                    return new ReentrantLock();
                }
            });

    @Override
    public Lock getLockObject(Object key) {
        try {
            return cache.get(key);
        } catch (ExecutionException e) {
            throw new RuntimeException("something went completely wrong and we can't obtain lock on a key: " + key);
        }
    }
}

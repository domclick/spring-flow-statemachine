package ru.sberned.statemachine.lock;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by empatuk on 07/12/2016.
 */
@Component
public class MapLockProvider<K> implements LockProvider<K> {
    LoadingCache<K, Lock> cache = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<K, Lock>() {
                @Override
                public Lock load(K key) throws Exception {
                    return new ReentrantLock();
                }
            });

    @Override
    public Lock getLockObject(K key) {
        try {
            return cache.get(key);
        } catch (ExecutionException e) {
            throw new RuntimeException("something went completely wrong and we can't obtain lock on a key: " + key);
        }
    }
}

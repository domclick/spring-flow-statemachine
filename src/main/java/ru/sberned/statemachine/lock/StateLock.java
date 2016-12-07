package ru.sberned.statemachine.lock;

import java.util.concurrent.locks.Lock;

/**
 * Created by empatuk on 07/12/2016.
 */
public interface StateLock<K> {
    Lock getLockObject(K key);
}

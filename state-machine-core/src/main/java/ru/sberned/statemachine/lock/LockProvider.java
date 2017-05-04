package ru.sberned.statemachine.lock;

import java.util.concurrent.locks.Lock;

/**
 * Created by jpatuk on 07/12/2016.
 */
public interface LockProvider {
    Lock getLockObject(Object key);
}

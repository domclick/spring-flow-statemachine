package ru.sberned.statemachine.state;

import java.util.List;

/**
 * Created by empatuk on 09/11/2016.
 */
public interface BeforeTransition<T> {
    void beforeTransition(T item);
}

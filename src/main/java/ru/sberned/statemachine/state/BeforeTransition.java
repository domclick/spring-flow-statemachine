package ru.sberned.statemachine.state;

/**
 * Created by empatuk on 09/11/2016.
 */
public interface BeforeTransition<T> {
    boolean beforeTransition(T item);
}

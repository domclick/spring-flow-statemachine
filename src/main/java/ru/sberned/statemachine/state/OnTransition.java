package ru.sberned.statemachine.state;

import java.util.List;

/**
 * Created by empatuk on 31/10/2016.
 */
public interface OnTransition<T> {
    default void beforeTransition(List<T> items) {};

    default void afterTransition(List<T> items) {};
}

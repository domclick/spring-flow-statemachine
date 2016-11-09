package ru.sberned.statemachine.state;

import java.util.List;

/**
 * Created by empatuk on 31/10/2016.
 */
public interface OnTransition<T, E extends Enum<E>> {
    void moveToState(E state, List<T> items);
}

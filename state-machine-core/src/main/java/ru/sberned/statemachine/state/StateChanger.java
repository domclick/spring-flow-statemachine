package ru.sberned.statemachine.state;

/**
 * Created by jpatuk on 31/10/2016.
 */
public interface StateChanger<T, E extends Enum<E>> {
    void moveToState(E state, T item);
}

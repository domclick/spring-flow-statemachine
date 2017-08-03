package ru.sberned.statemachine.state;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 31/10/2016.
 */
public interface StateChanger<T, E> {
    void moveToState(E state, T item, Object... infos);
}

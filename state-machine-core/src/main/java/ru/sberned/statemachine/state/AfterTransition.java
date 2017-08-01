package ru.sberned.statemachine.state;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 09/11/2016.
 */
public interface AfterTransition<T> {
    void afterTransition(T item);
}

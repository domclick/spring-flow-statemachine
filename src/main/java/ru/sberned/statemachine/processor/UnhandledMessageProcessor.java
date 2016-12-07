package ru.sberned.statemachine.processor;

/**
 * Created by empatuk on 07/12/2016.
 */
public interface UnhandledMessageProcessor<T> {
    void process(T item);
}

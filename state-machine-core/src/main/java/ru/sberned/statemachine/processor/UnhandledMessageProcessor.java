package ru.sberned.statemachine.processor;

/**
 * Created by jpatuk on 07/12/2016.
 */
public interface UnhandledMessageProcessor<T> {
    enum IssueType {INVALID_TRANSITION, TIMEOUT, INTERRUPTED_EXCEPTION, EXECUTION_EXCEPTION}

    void process(T item, IssueType type, Exception ex);
}

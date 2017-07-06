package ru.sberned.statemachine.processor;

/**
 * Created by jpatuk on 07/12/2016.
 */
public interface UnhandledMessageProcessor<ID, STATE> {
    enum IssueType {INVALID_TRANSITION, TIMEOUT, INTERRUPTED_EXCEPTION, EXECUTION_EXCEPTION, ENTITY_NOT_FOUND}

    void process(ID id, STATE newState, IssueType type, Exception ex);
}

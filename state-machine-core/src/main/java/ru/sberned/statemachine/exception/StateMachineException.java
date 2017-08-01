package ru.sberned.statemachine.exception;

import ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 06/07/2017.
 */
public class StateMachineException extends RuntimeException {
    private IssueType issueType;

    public StateMachineException(IssueType issueType) {
        this.issueType = issueType;
    }

    public IssueType getIssueType() {
        return issueType;
    }
}

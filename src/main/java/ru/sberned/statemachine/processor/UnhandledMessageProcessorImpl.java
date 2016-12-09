package ru.sberned.statemachine.processor;

/**
 * Created by empatuk on 07/12/2016.
 */
public class UnhandledMessageProcessorImpl<T> implements UnhandledMessageProcessor<T> {

    @Override
    public void process(T item, IssueType type, Exception ex) {
        //default impl does nothing
    }
}

package ru.sberned.statemachine;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.sberned.statemachine.processor.UnableToProcessException;

/**
 * Created by jpatuk on 20/12/2016.
 */
@Component
@Transactional
public class StateMachine<ENTITY, STATE extends Enum<STATE>> {

    // public is here to make @Transactional work
    public void processItem(StateRepository<ENTITY, STATE> stateRepository, ENTITY item, STATE from, STATE to) {
        stateRepository.getBeforeAll().forEach(handler -> {
            if (!handler.beforeTransition(item)) {
                throw new UnableToProcessException();
            }
        });
        stateRepository.getBefore(from, to).forEach(handler -> {
            if (!handler.beforeTransition(item)) {
                throw new UnableToProcessException();
            }
        });
        stateRepository.getStateChanger().moveToState(to, item);
        stateRepository.getAfter(from, to).forEach(handler -> handler.afterTransition(item));
        stateRepository.getAfterAll().forEach(handler -> handler.afterTransition(item));
    }
}

package ru.sberned.statemachine.state;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 29/05/2017.
 */
public interface AfterAnyTransition<ENTITY, STATE extends Enum<STATE>> {
    void afterTransition(ENTITY item, STATE stateTo);
}

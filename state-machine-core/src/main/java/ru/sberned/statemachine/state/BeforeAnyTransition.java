package ru.sberned.statemachine.state;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 29/05/2017.
 */
public interface BeforeAnyTransition<ENTITY, STATE extends Enum<STATE>> {
    boolean beforeTransition(ENTITY item, STATE state);
}

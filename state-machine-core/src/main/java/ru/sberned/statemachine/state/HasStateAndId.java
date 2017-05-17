package ru.sberned.statemachine.state;

/**
 * Created by jpatuk on 07/12/2016.
 */
public interface HasStateAndId<ID, STATE> {
    ID getId();

    STATE getState();
}

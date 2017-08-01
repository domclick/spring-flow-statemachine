package ru.sberned.statemachine.state;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 07/12/2016.
 */
public interface HasStateAndId<ID, STATE> {
    ID getId();

    STATE getState();
}

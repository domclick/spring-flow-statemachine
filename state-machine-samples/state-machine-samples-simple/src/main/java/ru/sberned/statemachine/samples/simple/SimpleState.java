package ru.sberned.statemachine.samples.simple;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 25/04/2017.
 */
public enum SimpleState {
    STARTED, IN_PROGRESS, FINISHED, CANCELLED;

    public static SimpleState getByName(String name) {
        for (SimpleState state : SimpleState.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }
        return null;
    }
}

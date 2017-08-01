package ru.sberned.statemachine.util;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 21/11/2016.
 */
public enum CustomState {
    START, STATE1, STATE2, STATE3, STATE4, FINISH, CANCEL;

    public static CustomState getByName(String name) {
        for (CustomState state : CustomState.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }
        return null;
    }
}

package ru.sberned.statemachine.impl;

import ru.sberned.statemachine.state.GetEnumValue;

/**
 * Created by empatuk on 03/11/2016.
 */
public enum CustomState implements GetEnumValue {
    STATE1(Constants.STATE1), STATE2(Constants.STATE2), STATE3(Constants.STATE3), STATE4(Constants.STATE4), STATE5(Constants.STATE5);

    private final String value;

    CustomState(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static class Constants {
        public static final String STATE1 = "1";
        public static final String STATE2 = "2";
        public static final String STATE3 = "3";
        public static final String STATE4 = "4";
        public static final String STATE5 = "5";
    }
}

package ru.sberned.statemachine;

import ru.sberned.statemachine.state.StateChangedEvent;

import java.util.List;

/**
 * Created by empatuk on 18/11/2016.
 */
public class TestStateChangedEvent  {
    private StateChangedEvent<TestConfig.CustomState> stateChangedEvent;
    public TestStateChangedEvent(Object source, List<String> ids, TestConfig.CustomState newState) {
        stateChangedEvent = new StateChangedEvent<TestConfig.CustomState>(source, ids, newState);
    }

    public TestStateChangedEvent(Object source, String id, TestConfig.CustomState newState) {
        stateChangedEvent = new StateChangedEvent<TestConfig.CustomState>(source, id, newState);
    }

    public StateChangedEvent<TestConfig.CustomState> getStateChangedEvent() {
        return stateChangedEvent;
    }
}

package ru.sberned.statemachine.config;

import ru.sberned.statemachine.config.CustomState;
import ru.sberned.statemachine.state.StateChangedEvent;

import java.util.List;

/**
 * Created by empatuk on 18/11/2016.
 */
public class TestStateChangedEvent  {
    private StateChangedEvent<CustomState, String> stateChangedEvent;
    public TestStateChangedEvent(Object source, List<String> ids, CustomState newState) {
        stateChangedEvent = new StateChangedEvent<>(source, ids, newState);
    }

    public TestStateChangedEvent(Object source, String id, CustomState newState) {
        stateChangedEvent = new StateChangedEvent<>(source, id, newState);
    }

    public StateChangedEvent<CustomState, String> getStateChangedEvent() {
        return stateChangedEvent;
    }
}

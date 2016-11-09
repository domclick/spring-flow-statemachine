package ru.sberned.statemachine.state;

import org.springframework.context.ApplicationEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Created by empatuk on 01/11/2016.
 */
public class StateChangedEvent<E extends Enum<E>> extends ApplicationEvent {
    private List<String> ids;
    private E newState;

    public StateChangedEvent(Object source, List<String> ids, E newState) {
        super(source);
        this.ids = ids;
        this.newState = newState;
    }

    public StateChangedEvent(Object source, String id, E newState) {
        super(source);
        this.ids = Arrays.asList(id);
        this.newState = newState;
    }

    public List<String> getIds() {
        return ids;
    }

    public E getNewState() {
        return newState;
    }
}

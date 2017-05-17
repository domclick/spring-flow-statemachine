package ru.sberned.statemachine.state;

import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.List;

/**
 * Created by jpatuk on 01/11/2016.
 */
public class StateChangedEvent<E extends Enum<E>, ID> extends ApplicationEvent {
    private List<ID> ids;
    private E newState;

    public StateChangedEvent(Object source, List<ID> ids, E newState) {
        super(source);
        this.ids = ids;
        this.newState = newState;
    }

    public StateChangedEvent(Object source, ID id, E newState) {
        super(source);
        this.ids = Collections.singletonList(id);
        this.newState = newState;
    }

    public List<ID> getIds() {
        return ids;
    }

    public E getNewState() {
        return newState;
    }
}

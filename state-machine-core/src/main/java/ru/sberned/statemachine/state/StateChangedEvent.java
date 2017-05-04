package ru.sberned.statemachine.state;

import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.List;

/**
 * Created by jpatuk on 01/11/2016.
 */
public class StateChangedEvent<E extends Enum<E>, K> extends ApplicationEvent {
    private List<K> ids;
    private E newState;

    public StateChangedEvent(Object source, List<K> ids, E newState) {
        super(source);
        this.ids = ids;
        this.newState = newState;
    }

    public StateChangedEvent(Object source, K id, E newState) {
        super(source);
        this.ids = Collections.singletonList(id);
        this.newState = newState;
    }

    public List<K> getIds() {
        return ids;
    }

    public E getNewState() {
        return newState;
    }
}

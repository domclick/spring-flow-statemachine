package ru.sberned.statemachine.state;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by jpatuk on 01/11/2016.
 */
public class StateChangedEvent<E extends Enum<E>, ID> implements Serializable {
    private Collection<ID> ids;
    private E newState;

    private Object info;

    public StateChangedEvent(Collection<ID> ids, E newState) {
        this.ids = ids;
        this.newState = newState;
    }

    public StateChangedEvent(Collection<ID> ids, E newState, Object info) {
        this.ids = ids;
        this.newState = newState;
        this.info = info;
    }

    public StateChangedEvent(ID id, E newState) {
        this.ids = Collections.singletonList(id);
        this.newState = newState;
    }

    public StateChangedEvent(ID id, E newState, Object info) {
        this.ids = Collections.singletonList(id);
        this.newState = newState;
        this.info = info;
    }

    public Collection<ID> getIds() {
        return ids;
    }

    public void setIds(Collection<ID> ids) {
        this.ids = ids;
    }

    public E getNewState() {
        return newState;
    }

    public void setNewState(E newState) {
        this.newState = newState;
    }

    public Object getInfo() {
        return info;
    }

    private void setInfo(Object info) {
        this.info = info;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StateChangedEvent<?, ?> that = (StateChangedEvent<?, ?>) o;

        if (ids != null ? !ids.equals(that.ids) : that.ids != null) return false;
        if (newState != null ? !newState.equals(that.newState) : that.newState != null) return false;
        return info != null ? info.equals(that.info) : that.info == null;
    }

    @Override
    public int hashCode() {
        int result = ids != null ? ids.hashCode() : 0;
        result = 31 * result + (newState != null ? newState.hashCode() : 0);
        return result;
    }

}

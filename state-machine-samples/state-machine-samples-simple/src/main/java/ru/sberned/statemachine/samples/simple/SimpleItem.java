package ru.sberned.statemachine.samples.simple;

import ru.sberned.statemachine.state.HasId;

/**
 * Created by jpatuk on 04/05/2017.
 */
public class SimpleItem implements HasId<String> {
    private String id;
    private SimpleState state = SimpleState.STARTED;

    public SimpleItem(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SimpleItem && ((SimpleItem) obj).id != null && ((SimpleItem) obj).id.equals(this.id);
    }

    @Override
    public int hashCode() {
        if (id == null) return 0;
        return id.hashCode();
    }

    public SimpleState getState() {
        return state;
    }

    public void setState(SimpleState state) {
        this.state = state;
    }
}

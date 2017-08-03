package ru.sberned.samples.loading;

import ru.sberned.samples.loading.model.states.FirstState;
import ru.sberned.samples.loading.model.states.IAmSimpleState;
import ru.sberned.statemachine.state.HasStateAndId;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 04/05/2017.
 */
public class SimpleItem implements HasStateAndId<String, IAmSimpleState> {
    private String id;
    private IAmSimpleState state = new FirstState();

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

    @Override
    public IAmSimpleState getState() {
        return state;
    }

    public void setState(IAmSimpleState state) {
        this.state = state;
    }
}

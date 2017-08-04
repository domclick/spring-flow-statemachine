package ru.sberned.samples.loading;

import ru.sberned.samples.loading.model.states.FirstState;
import ru.sberned.samples.loading.model.states.IAmLoadableState;
import ru.sberned.statemachine.state.HasStateAndId;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 04/05/2017.
 */
public class LoadableItem implements HasStateAndId<String, IAmLoadableState> {
    private String id;
    private IAmLoadableState state = new FirstState();

    public LoadableItem(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LoadableItem && ((LoadableItem) obj).id != null && ((LoadableItem) obj).id.equals(this.id);
    }

    @Override
    public int hashCode() {
        if (id == null) return 0;
        return id.hashCode();
    }

    @Override
    public IAmLoadableState getState() {
        return state;
    }

    public void setState(IAmLoadableState state) {
        this.state = state;
    }
}

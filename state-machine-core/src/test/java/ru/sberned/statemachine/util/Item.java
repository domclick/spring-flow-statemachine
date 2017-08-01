package ru.sberned.statemachine.util;

import ru.sberned.statemachine.state.HasStateAndId;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 21/11/2016.
 */
public class Item implements HasStateAndId<String, CustomState> {
    public String id;
    public CustomState state;

    public Item(String id, CustomState state) {
        this.id = id;
        this.state = state;
    }

    @Override
    public CustomState getState() {
        return state;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Item && id.equals(((Item) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String getId() {
        return id;
    }
}

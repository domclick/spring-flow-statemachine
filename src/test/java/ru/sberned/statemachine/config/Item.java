package ru.sberned.statemachine.config;

import ru.sberned.statemachine.state.GetId;

/**
 * Created by empatuk on 21/11/2016.
 */
public class Item implements GetId<String> {
    public String id;
    public CustomState state;

    public Item(String id, CustomState state) {
        this.id = id;
        this.state = state;
    }

    public CustomState getState() {
        return state;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Item && id.equals(((Item) other).id);
    }

    @Override
    public int hashCode() {
        return  id.hashCode();
    }

    @Override
    public String getId() {
        return id;
    }
}

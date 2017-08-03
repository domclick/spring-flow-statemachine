package ru.sberned.statemachine.util;

import ru.sberned.statemachine.state.HasStateAndId;

public class ItemTwo implements HasStateAndId<String, StateTwo> {
    private StateTwo state;
    private String id;

    public ItemTwo(String id) {
        this.id = id;
        state = new StateTwo.StartStateTwo();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public StateTwo getState() {
        return state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemTwo)) return false;

        ItemTwo itemTwo = (ItemTwo) o;

        return id.equals(itemTwo.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ItemTwo{" + "id='" + id + '\'' + '}';
    }

    public void setState(StateTwo state) {
        this.state = state;
    }
}

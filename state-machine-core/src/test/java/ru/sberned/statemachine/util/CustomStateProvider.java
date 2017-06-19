package ru.sberned.statemachine.util;

import ru.sberned.statemachine.state.ItemWithStateProvider;

import java.util.List;

/**
 * Created by jpatuk on 17/06/2017.
 */
public class CustomStateProvider implements ItemWithStateProvider<Item, String> {
    private List<Item> items;

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @Override
    public Item getItemById(String id) {
        for (Item item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }
}

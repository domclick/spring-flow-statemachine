package ru.sberned.statemachine.samples.simple.store;

import org.springframework.stereotype.Component;
import ru.sberned.statemachine.samples.simple.SimpleItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jpatuk on 04/05/2017.
 */
@Component
public class ItemStore {
    private List<SimpleItem> items = new ArrayList<>();

    public boolean itemExists(String id) {
        return getItem(id) != null;
    }

    public SimpleItem getItem(String id) {
        for (SimpleItem item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public SimpleItem getItem(SimpleItem itemToFind) {
        for (SimpleItem item : items) {
            if (item.equals(itemToFind)) {
                return item;
            }
        }
        return null;
    }

    public void createNewItem(String id) {
        items.add(new SimpleItem(id));
        System.out.println("Created new item with id " + id);
    }

    public List<SimpleItem> getItemsById(List<String> ids) {
        return items.stream().filter(i -> ids.contains(i.getId())).collect(Collectors.toList());
    }
}

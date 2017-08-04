package ru.sberned.samples.loading.store;

import org.springframework.stereotype.Repository;
import ru.sberned.samples.loading.LoadableItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 04/05/2017.
 */
@Repository
public class ItemStore {
    private List<LoadableItem> items = new ArrayList<>();

    public boolean itemExists(String id) {
        return getItem(id) != null;
    }

    public LoadableItem getItem(String id) {
        for (LoadableItem item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public LoadableItem getItem(LoadableItem itemToFind) {
        for (LoadableItem item : items) {
            if (item.equals(itemToFind)) {
                return item;
            }
        }
        return null;
    }

    public void createNewItem(String id) {
        items.add(new LoadableItem(id));
        System.out.println("Created new item with id " + id);
    }
}

package ru.sberned.statemachine.impl;

import org.springframework.stereotype.Component;
import ru.sberned.statemachine.state.StateProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by empatuk on 03/11/2016.
 */
@Component
public class CustomStateProvider implements StateProvider<CustomItem, CustomState> {

    @Override
    public Map<CustomItem, CustomState> getItemsState(List<String> ids) {
        Map<CustomItem, CustomState> res = new HashMap<>();
        res.put(new CustomItem("1", 1), CustomState.STATE1);
        return res;
    }

    @Override
    public void updateState(CustomState newState, List<CustomItem> items) {

    }
}

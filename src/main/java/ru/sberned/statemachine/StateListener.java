package ru.sberned.statemachine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.sberned.statemachine.state.StateChangedEvent;
import ru.sberned.statemachine.state.StateProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by empatuk on 09/11/2016.
 */
@Component
public class StateListener<T, E extends Enum<E>> {
    @Autowired
    private StateProvider<T, E> stateProvider;
    private StateHolder<T, E> stateHolder;

    public void setStateHolder(StateHolder<T, E> stateHolder) {
        this.stateHolder = stateHolder;
    }

    @Async
    @EventListener
    public synchronized void handleStateChanged(StateChangedEvent<E> event) {
        Assert.notNull(stateHolder);
        Map<E, List<T>> itemsMap = new HashMap<>();
        Map<T, E> sourceMap = stateProvider.getItemsState(event.getIds());
        for (Map.Entry<T, E> entry : sourceMap.entrySet()) {
            itemsMap.putIfAbsent(entry.getValue(), new ArrayList<>());
            itemsMap.get(entry.getValue()).add(entry.getKey());
        }

        itemsMap.forEach((stateFrom, items) -> {
            if (stateHolder.isValidTransition(stateFrom, event.getNewState())) {
                processItems(items, stateFrom, event.getNewState());
            }
        });
    }

    @Transactional
    private void processItems(List<T> items, E from, E to) {
        stateHolder.getBefore(from, to).forEach(handler -> handler.beforeTransition(items));
        stateHolder.getTransition().moveToState(to, items);
        stateHolder.getAfter(from, to).forEach(handler -> handler.afterTransition(items));
    }
}

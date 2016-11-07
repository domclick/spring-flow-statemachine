package ru.sberned.statemachine.state;

import java.util.List;
import java.util.Map;

/**
 * Created by empatuk on 01/11/2016.
 */
public interface StateProvider<T, E extends Enum<E>> {
    Map<T, E> getItemsState(List<String> ids);

    void updateState(E newState, List<T> items);
}

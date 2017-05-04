package ru.sberned.statemachine.state;

import java.util.List;
import java.util.Map;

/**
 * Created by jpatuk on 01/11/2016.
 */
public interface StateProvider<T, E extends Enum<E>, K> {
    Map<T, E> getItemsState(List<K> ids);
}

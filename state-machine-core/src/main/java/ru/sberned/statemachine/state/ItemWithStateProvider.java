package ru.sberned.statemachine.state;

import java.util.Collection;
import java.util.List;

/**
 * Created by jpatuk on 01/11/2016.
 */
public interface ItemWithStateProvider<ENTITY, ID> {
    Collection<ENTITY> getItemsByIds(List<ID> ids);
}

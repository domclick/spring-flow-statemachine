package ru.sberned.statemachine.state;

/**
 * Created by jpatuk on 01/11/2016.
 */
public interface ItemWithStateProvider<ENTITY, ID> {
    ENTITY getItemById(ID id);
}

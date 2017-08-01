package ru.sberned.statemachine.state;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 01/11/2016.
 */
public interface ItemWithStateProvider<ENTITY, ID> {
    ENTITY getItemById(ID id);
}

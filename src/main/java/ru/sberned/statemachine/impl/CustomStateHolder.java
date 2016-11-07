package ru.sberned.statemachine.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.sberned.statemachine.state.StateHolder;
import ru.sberned.statemachine.state.StateProvider;

import java.util.EnumSet;

/**
 * Created by empatuk on 03/11/2016.
 */
@Component
public class CustomStateHolder extends StateHolder<CustomItem, CustomState> {

    @Autowired
    protected CustomStateHolder(StateProvider<CustomItem, CustomState> stateProvider) {
        super(stateProvider, CustomState.class, EnumSet.allOf(CustomState.class));
    }
}

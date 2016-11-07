package ru.sberned.statemachine.impl;

import org.springframework.stereotype.Component;
import ru.sberned.statemachine.state.ChangeState;

/**
 * Created by empatuk on 03/11/2016.
 */
@Component
@ChangeState(enumClass = CustomState.class, from = {CustomState.Constants.STATE1}, to = CustomState.Constants.STATE2)
public class State1ToState2Handler {
}

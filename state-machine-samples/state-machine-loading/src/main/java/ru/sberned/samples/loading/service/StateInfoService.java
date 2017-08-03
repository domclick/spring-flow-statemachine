package ru.sberned.samples.loading.service;

import org.springframework.stereotype.Service;
import ru.sberned.samples.loading.SimpleItem;
import ru.sberned.samples.loading.model.states.FirstState;
import ru.sberned.samples.loading.model.states.IAmSimpleState;
import ru.sberned.samples.loading.model.states.SecondState;
import ru.sberned.samples.loading.model.states.ThirdState;
import ru.sberned.statemachine.state.AfterTransition;
import ru.sberned.statemachine.state.BeforeTransition;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.lang.System.out;
import static java.util.Collections.EMPTY_SET;
import static java.util.Collections.singletonList;

@Service
public class StateInfoService {

    public Set<IAmSimpleState> obtainAvailableStates() {
        return new HashSet<>(Arrays.asList(new FirstState(), new SecondState(), new ThirdState()));
    }

    public Iterable<IAmSimpleState> loadAvailableToStates(IAmSimpleState availableState) {
        if (availableState instanceof FirstState)
            return Arrays.asList(new SecondState(), new ThirdState());
        if (availableState instanceof SecondState)
            return Collections.singleton(new ThirdState());
        return Collections.emptySet();
    }

    public BeforeTransition<SimpleItem>[] getBeforeHandlers(IAmSimpleState fromState, IAmSimpleState toState) {
        BeforeTransition<SimpleItem> beforeTransition = item -> {
            out.println("It's loaded BEFORE action! Moving " + item.getId() + " from " + fromState.getName() + " to " + toState.getName() + "");
            return true;
        };
        return singletonList(beforeTransition).toArray(new BeforeTransition[0]);
    }

    public AfterTransition<SimpleItem>[] getAfterHandlers(IAmSimpleState fromState, IAmSimpleState toState) {
        AfterTransition<SimpleItem> afterTransition = item -> out.println("It's loaded AFTER action! Moving " + item.getId() + " from " + fromState.getName() + " to " + toState.getName() + "");
        return singletonList(afterTransition).toArray(new AfterTransition[0]);
    }
}

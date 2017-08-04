package ru.sberned.samples.loading.service;

import org.springframework.stereotype.Service;
import ru.sberned.samples.loading.LoadableItem;
import ru.sberned.samples.loading.model.states.FirstState;
import ru.sberned.samples.loading.model.states.IAmLoadableState;
import ru.sberned.samples.loading.model.states.SecondState;
import ru.sberned.samples.loading.model.states.ThirdState;
import ru.sberned.statemachine.state.AfterTransition;
import ru.sberned.statemachine.state.BeforeTransition;

import java.util.*;

import static java.lang.System.out;
import static java.util.Collections.singletonList;

@Service
public class StateInfoService {


    private List<IAmLoadableState> availableStates = new ArrayList<>(Arrays.asList(new FirstState(), new SecondState(), new ThirdState()));

    public Set<IAmLoadableState> obtainAvailableStates() {
        return new HashSet<>(availableStates);
    }

    public Iterable<IAmLoadableState> loadAvailableToStates(IAmLoadableState availableState) {
        if (availableState instanceof FirstState) {
            HashSet<IAmLoadableState> iAmLoadableStates = new HashSet<>(Arrays.asList(new SecondState(), new ThirdState()));
            iAmLoadableStates.retainAll(availableStates);
            return iAmLoadableStates;
        }
        if (availableState instanceof SecondState) {
            HashSet<IAmLoadableState> result = new HashSet<>(Collections.singleton(new ThirdState()));
            result.retainAll(availableStates);
            return result;
        }
        return Collections.emptySet();
    }

    public BeforeTransition<LoadableItem>[] getBeforeHandlers(IAmLoadableState fromState, IAmLoadableState toState) {
        BeforeTransition<LoadableItem> beforeTransition = item -> {
            out.println("It's loaded BEFORE action! Moving " + item.getId() + " from " + fromState.getName() + " to " + toState.getName() + "");
            return true;
        };
        return singletonList(beforeTransition).toArray(new BeforeTransition[0]);
    }

    public AfterTransition<LoadableItem>[] getAfterHandlers(IAmLoadableState fromState, IAmLoadableState toState) {
        AfterTransition<LoadableItem> afterTransition = item -> out.println("It's loaded AFTER action! Moving " + item.getId() + " from " + fromState.getName() + " to " + toState.getName() + "");
        return singletonList(afterTransition).toArray(new AfterTransition[0]);
    }

    public void removeState(IAmLoadableState loadableState) {
        availableStates.remove(loadableState);
        System.out.println("Removed state " + loadableState.getName() + " from available states and transitions");
    }
}

package ru.sberned.statemachine;

import ru.sberned.statemachine.state.AfterTransition;
import ru.sberned.statemachine.state.BeforeTransition;
import ru.sberned.statemachine.state.OnTransition;

import java.util.*;

/**
 * Created by empatuk on 09/11/2016.
 */
public class StateHolder<T, E extends Enum<E>> {
    private Map<E, Map<E, Processors>> stateMap = new HashMap<>();
    private OnTransition<T, E> transition;
    private Set<E> availableStates;

    boolean isValidTransition(E from, E to) {
        return stateMap.get(to) != null && stateMap.get(to).get(from) != null;
    }

    private void setStateChanger(OnTransition<T, E> transition) {
        this.transition = transition;
    }

    private void setAvailableStates(Set<E> availableStates) {
        this.availableStates = availableStates;
    }

    OnTransition<T, E> getTransition() {
        return transition;
    }

    List<BeforeTransition<T>> getBefore(E from, E to) {
        if (isValidTransition(from, to)) {
            return stateMap.get(to).get(from).getBeforeHandlers();
        }
        return new ArrayList<>();
    }

    List<AfterTransition<T>> getAfter(E from, E to) {
        if (isValidTransition(from, to)) {
            return stateMap.get(to).get(from).getAfterHandlers();
        }
        return new ArrayList<>();
    }

    private class Processors {
        private List<BeforeTransition<T>> beforeHandlers = new ArrayList<>();
        private List<AfterTransition<T>> afterHandlers = new ArrayList<>();

        private List<BeforeTransition<T>> getBeforeHandlers() {
            return beforeHandlers;
        }

        private List<AfterTransition<T>> getAfterHandlers() {
            return afterHandlers;
        }
    }


    static class StateHolderBuilder<T, E extends Enum<E>> {
        private StateHolder<T, E> stateHolder;
        public StateHolderBuilder() {
            stateHolder = new StateHolder<>();
        }

        public StateHolderBuilder setStateChanger(OnTransition<T, E> transition) {
            stateHolder.setStateChanger(transition);
            return this;
        }

        public StateHolderBuilder setAvailableStates(Set<E> availableStates) {
            stateHolder.setAvailableStates(availableStates);
            return this;
        }

        public From<T,E> defineTransitions() {
            return stateHolder.new StateTransition(stateHolder);
        }
    }

    public interface From<T, E extends Enum<E>> {
        To<T, E> from(E... states);
    }

    public interface To<T, E extends Enum<E>> {
        CompleteTransition<T, E> to(E... states);
    }

    public interface CompleteTransition<T, E extends Enum<E>> {
        CompleteTransition<T, E> before(BeforeTransition<T>... handlers);

        CompleteTransition<T, E> after(AfterTransition<T>... handlers);

        From<T, E> and();

        StateHolder<T, E> build();
    }

    public class StateTransition implements To<T, E>, From<T, E>, CompleteTransition<T, E> {
        private final StateHolder<T, E> stateHolder;
        private Set<E> from = new HashSet<>(), to = new HashSet<>();
        private List<BeforeTransition<T>> beforeTransitions = new ArrayList<>();
        private List<AfterTransition<T>> afterTransitions = new ArrayList<>();

        public StateTransition(StateHolder<T, E> stateHolder) {
            this.stateHolder = stateHolder;
        }

        private void checkAndFillStates(Set<E> whereToFill, E... states) {
            if (states == null || states.length == 0) {
                throw new IllegalArgumentException("No states supplied to from!");
            }

            for (E state : states) {
                if (!stateHolder.availableStates.contains(state)) {
                    throw new IllegalArgumentException("State " + state + " is not within available states");
                }
            }
            whereToFill.addAll(Arrays.asList(states));
        }

        @Override
        public To<T, E> from(E... states) {
            checkAndFillStates(from, states);
            return this;
        }

        @Override
        public CompleteTransition<T, E> to(E... states) {
            checkAndFillStates(to, states);
            return this;
        }

        @Override
        public CompleteTransition<T, E> before(BeforeTransition<T>... handlers) {
            beforeTransitions.addAll(Arrays.asList(handlers));
            return this;
        }

        @Override
        public CompleteTransition<T, E> after(AfterTransition<T>... handlers) {
            afterTransitions.addAll(Arrays.asList(handlers));
            return this;
        }

        private void finalizeStep() {
            Map<E, Map<E, StateHolder<T, E>.Processors>> states = stateHolder.stateMap;

            for (E toState : to) {
                Map<E, StateHolder<T, E>.Processors> toMap = states.get(toState);
                if (toMap == null) {
                    toMap = new HashMap<>();
                    states.put(toState, toMap);
                }

                for (E fromState : from) {
                    StateHolder.Processors processors = toMap.get(fromState);
                    if (processors == null) {
                        processors = stateHolder.new Processors();
                        toMap.put(fromState, processors);
                    }
                    processors.getBeforeHandlers().addAll(beforeTransitions);
                    processors.getAfterHandlers().addAll(afterTransitions);
                }
            }
        }

        @Override
        public From<T, E> and() {
            finalizeStep();
            return stateHolder.new StateTransition(stateHolder);
        }

        @Override
        public StateHolder<T, E> build() {
            finalizeStep();
            return stateHolder;
        }
    }
}

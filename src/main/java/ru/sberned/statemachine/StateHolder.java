package ru.sberned.statemachine;

import ru.sberned.statemachine.state.AfterTransition;
import ru.sberned.statemachine.state.BeforeTransition;
import ru.sberned.statemachine.state.OnTransition;

import java.util.*;

/**
 * Created by empatuk on 09/11/2016.
 */
public class StateHolder<T, E extends Enum<E>> {
    private Map<E, Map<E, Processors<T>>> stateMap = new HashMap<>();
    private OnTransition<T, E> transition;
    private EnumSet<E> availableStates;

    public boolean isValidTransition(E from, E to) {
        return stateMap.get(to) != null && stateMap.get(to).get(from) != null;
    }

    private void setStateChanger(OnTransition<T, E> transition) {
        this.transition = transition;
    }

    private void setAvailableStates(EnumSet<E> availableStates) {
        this.availableStates = availableStates;
    }

    public OnTransition<T, E> getTransition() {
        return transition;
    }

    public List<BeforeTransition<T>> getBefore(E from, E to) {
        if (isValidTransition(from, to)) {
            stateMap.get(to).get(from).getBeforeHandlers();
        }
        return new ArrayList<>();
    }

    public List<AfterTransition<T>> getAfter(E from, E to) {
        if (isValidTransition(from, to)) {
            stateMap.get(to).get(from).getAfterHandlers();
        }
        return new ArrayList<>();
    }

    private static class Processors<T> {
        private List<BeforeTransition<T>> beforeHandlers = new ArrayList<>();
        private List<AfterTransition<T>> afterHandlers = new ArrayList<>();

        private List<BeforeTransition<T>> getBeforeHandlers() {
            return beforeHandlers;
        }

        private List<AfterTransition<T>> getAfterHandlers() {
            return afterHandlers;
        }
    }


    public static class StateHolderBuilder<T, E extends Enum<E>> {
        private StateHolder<T, E> stateHolder;
        public StateHolderBuilder() {
            stateHolder = new StateHolder<T, E>();
        }

        public StateHolderBuilder setStateChanger(OnTransition<T, E> transition) {
            stateHolder.setStateChanger(transition);
            return this;
        }

        public StateHolderBuilder setAvailableStates(EnumSet<E> availableStates) {
            stateHolder.setAvailableStates(availableStates);
            return this;
        }

        public From<T,E> defineTransitions() {
            StateTransition<T, E> transition = new StateTransition<T, E>(stateHolder);
            return transition;
        }
    }

    public interface From<T, E extends Enum<E>> {
        To<T, E> from(E... states);
    }

    public interface To<T, E extends Enum<E>> {
        CompleteTransition<T, E> to(E... states);
    }

    public interface CompleteTransition<T, E extends Enum<E>> {
        void before(BeforeTransition<T>... handlers);

        void after(AfterTransition<T>... handlers);

        From<T, E> and();

        StateHolder<T, E> complete();
    }

    public static class StateTransition<T, E extends Enum<E>> implements To<T, E>, From<T, E>, CompleteTransition<T, E> {
        private final StateHolder<T, E> stateHolder;
        private Set<E> from, to;
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
        public void before(BeforeTransition<T>... handlers) {
            beforeTransitions.addAll(Arrays.asList(handlers));
        }

        @Override
        public void after(AfterTransition<T>... handlers) {
            afterTransitions.addAll(Arrays.asList(handlers));
        }

        private void finalizeStep() {
            Map<E, Map<E, Processors<T>>> states = stateHolder.stateMap;

            for (E toState : to) {
                Map<E, Processors<T>> toMap = states.get(toState);
                if (toMap == null) {
                    toMap = new HashMap<>();
                    states.put(toState, toMap);
                }

                for (E fromState : from) {
                    Processors<T> processors = toMap.get(fromState);
                    if (processors == null) {
                        processors = new Processors<T>();
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
            return new StateTransition<T, E>(stateHolder);
        }

        @Override
        public StateHolder<T, E> complete() {
            finalizeStep();
            return stateHolder;
        }
    }
}

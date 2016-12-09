package ru.sberned.statemachine;

import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.state.AfterTransition;
import ru.sberned.statemachine.state.BeforeTransition;
import ru.sberned.statemachine.state.OnTransition;

import java.util.*;

/**
 * Created by empatuk on 09/11/2016.
 */
public class StateMachine<ENTITY, STATE extends Enum<STATE>> {
    private Map<STATE, Map<STATE, Processors>> stateMap = new HashMap<>();
    private OnTransition<ENTITY, STATE> transition;
    private Set<STATE> availableStates;
    private List<BeforeTransition<ENTITY>> beforeAllHandlers = new ArrayList<>();
    private List<AfterTransition<ENTITY>> afterAllHandlers = new ArrayList<>();
    private UnhandledMessageProcessor<ENTITY> unhandledMessageProcessor;

    boolean isValidTransition(STATE from, STATE to) {
        return stateMap.get(to) != null && stateMap.get(to).get(from) != null;
    }

    private void setStateChanger(OnTransition<ENTITY, STATE> transition) {
        this.transition = transition;
    }

    private void setAvailableStates(Set<STATE> availableStates) {
        this.availableStates = availableStates;
    }

    private void setAnyBefore(List<BeforeTransition<ENTITY>> anyBefore) {
        beforeAllHandlers.addAll(anyBefore);
    }

    private void setAnyAfter(List<AfterTransition<ENTITY>> anyAfter) {
        afterAllHandlers.addAll(anyAfter);
    }

    private void setUnhandledMessageProcessor(UnhandledMessageProcessor<ENTITY> unhandledMessageProcessor) {
        this.unhandledMessageProcessor = unhandledMessageProcessor;
    }

    OnTransition<ENTITY, STATE> getTransition() {
        return transition;
    }

    List<BeforeTransition<ENTITY>> getBefore(STATE from, STATE to) {
        if (isValidTransition(from, to)) {
            return stateMap.get(to).get(from).getBeforeHandlers();
        }
        return new ArrayList<>();
    }

    List<AfterTransition<ENTITY>> getAfter(STATE from, STATE to) {
        if (isValidTransition(from, to)) {
            return stateMap.get(to).get(from).getAfterHandlers();
        }
        return new ArrayList<>();
    }

    List<BeforeTransition<ENTITY>> getBeforeAll() {
        return beforeAllHandlers;
    }

    List<AfterTransition<ENTITY>> getAfterAll() {
        return afterAllHandlers;
    }

    UnhandledMessageProcessor<ENTITY> getUnhandledMessageProcessor() {
        return unhandledMessageProcessor;
    }

    private class Processors {
        private List<BeforeTransition<ENTITY>> beforeHandlers = new ArrayList<>();
        private List<AfterTransition<ENTITY>> afterHandlers = new ArrayList<>();

        private List<BeforeTransition<ENTITY>> getBeforeHandlers() {
            return beforeHandlers;
        }

        private List<AfterTransition<ENTITY>> getAfterHandlers() {
            return afterHandlers;
        }
    }


    static class StateHolderBuilder<ENTITY, STATE extends Enum<STATE>> {
        private StateMachine<ENTITY, STATE> stateHolder;

        public StateHolderBuilder() {
            stateHolder = new StateMachine<>();
        }

        public StateHolderBuilder<ENTITY, STATE> setStateChanger(OnTransition<ENTITY, STATE> transition) {
            stateHolder.setStateChanger(transition);
            return this;
        }

        public StateHolderBuilder<ENTITY, STATE> setAnyBefore(BeforeTransition<ENTITY>... handlers) {
            stateHolder.setAnyBefore(Arrays.asList(handlers));
            return this;
        }


        public StateHolderBuilder<ENTITY, STATE> setAnyAfter(AfterTransition<ENTITY>... handlers) {
            stateHolder.setAnyAfter(Arrays.asList(handlers));
            return this;
        }

        public StateHolderBuilder<ENTITY, STATE> setAvailableStates(Set<STATE> availableStates) {
            stateHolder.setAvailableStates(availableStates);
            return this;
        }

        public StateHolderBuilder<ENTITY, STATE> setUnhandledMessageProcessor(UnhandledMessageProcessor<ENTITY> unhandledMessageProcessor) {
            stateHolder.setUnhandledMessageProcessor(unhandledMessageProcessor);
            return this;
        }

        public From<ENTITY, STATE> defineTransitions() {
            return stateHolder.new StateTransition(stateHolder);
        }
    }

    public interface From<ENTITY, STATE extends Enum<STATE>> {
        To<ENTITY, STATE> from(STATE... states);
    }

    public interface To<ENTITY, STATE extends Enum<STATE>> {
        CompleteTransition<ENTITY, STATE> to(STATE... states);
    }

    public interface CompleteTransition<ENTITY, STATE extends Enum<STATE>> {
        CompleteTransition<ENTITY, STATE> before(BeforeTransition<ENTITY>... handlers);

        CompleteTransition<ENTITY, STATE> after(AfterTransition<ENTITY>... handlers);

        From<ENTITY, STATE> and();

        StateMachine<ENTITY, STATE> build();
    }

    public class StateTransition implements To<ENTITY, STATE>, From<ENTITY, STATE>, CompleteTransition<ENTITY, STATE> {
        private final StateMachine<ENTITY, STATE> stateHolder;
        private Set<STATE> from = new HashSet<>(), to = new HashSet<>();
        private List<BeforeTransition<ENTITY>> beforeTransitions = new ArrayList<>();
        private List<AfterTransition<ENTITY>> afterTransitions = new ArrayList<>();

        public StateTransition(StateMachine<ENTITY, STATE> stateHolder) {
            this.stateHolder = stateHolder;
        }

        private void checkAndFillStates(Set<STATE> whereToFill, STATE... states) {
            if (states == null || states.length == 0) {
                throw new IllegalArgumentException("No states supplied to from!");
            }

            for (STATE state : states) {
                if (!stateHolder.availableStates.contains(state)) {
                    throw new IllegalArgumentException("State " + state + " is not within available states");
                }
            }
            whereToFill.addAll(Arrays.asList(states));
        }

        @Override
        public To<ENTITY, STATE> from(STATE... states) {
            checkAndFillStates(from, states);
            return this;
        }

        @Override
        public CompleteTransition<ENTITY, STATE> to(STATE... states) {
            checkAndFillStates(to, states);
            return this;
        }

        @Override
        public CompleteTransition<ENTITY, STATE> before(BeforeTransition<ENTITY>... handlers) {
            beforeTransitions.addAll(Arrays.asList(handlers));
            return this;
        }

        @Override
        public CompleteTransition<ENTITY, STATE> after(AfterTransition<ENTITY>... handlers) {
            afterTransitions.addAll(Arrays.asList(handlers));
            return this;
        }

        private void finalizeStep() {
            Map<STATE, Map<STATE, StateMachine<ENTITY, STATE>.Processors>> states = stateHolder.stateMap;

            for (STATE toState : to) {
                Map<STATE, StateMachine<ENTITY, STATE>.Processors> toMap = states.get(toState);
                if (toMap == null) {
                    toMap = new HashMap<>();
                    states.put(toState, toMap);
                }

                for (STATE fromState : from) {
                    toMap.putIfAbsent(fromState, stateHolder.new Processors());
                    StateMachine<ENTITY, STATE>.Processors processors = toMap.get(fromState);
                    processors.getBeforeHandlers().addAll(beforeTransitions);
                    processors.getAfterHandlers().addAll(afterTransitions);
                }
            }
        }

        @Override
        public From<ENTITY, STATE> and() {
            finalizeStep();
            return stateHolder.new StateTransition(stateHolder);
        }

        @Override
        public StateMachine<ENTITY, STATE> build() {
            finalizeStep();
            return stateHolder;
        }
    }
}

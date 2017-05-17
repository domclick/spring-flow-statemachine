package ru.sberned.statemachine;

import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.state.AfterTransition;
import ru.sberned.statemachine.state.BeforeTransition;
import ru.sberned.statemachine.state.StateChanger;

import java.util.*;

/**
 * Created by jpatuk on 22/03/2017.
 */
public class StateRepository<ENTITY, STATE extends Enum<STATE>> {
    private Map<STATE, Map<STATE, Processors>> stateMap = new HashMap<>();
    private StateChanger<ENTITY, STATE> stateChanger;
    private Set<STATE> availableStates;
    private List<BeforeTransition<ENTITY>> beforeAllHandlers = new ArrayList<>();
    private List<AfterTransition<ENTITY>> afterAllHandlers = new ArrayList<>();
    private UnhandledMessageProcessor<ENTITY> unhandledMessageProcessor;

    boolean isValidTransition(STATE from, STATE to) {
        return stateMap.get(to) != null && stateMap.get(to).get(from) != null;
    }

    private void setStateChanger(StateChanger<ENTITY, STATE> transition) {
        this.stateChanger = transition;
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

    StateChanger<ENTITY, STATE> getStateChanger() {
        return stateChanger;
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


    public static class StateRepositoryBuilder<ENTITY, STATE extends Enum<STATE>> {
        private StateRepository<ENTITY, STATE> stateRepository;

        public StateRepositoryBuilder() {
            stateRepository = new StateRepository<>();
        }

        public StateRepositoryBuilder<ENTITY, STATE> setStateChanger(StateChanger<ENTITY, STATE> stateChanger) {
            stateRepository.setStateChanger(stateChanger);
            return this;
        }

        @SafeVarargs
        public final StateRepositoryBuilder<ENTITY, STATE> setAnyBefore(BeforeTransition<ENTITY>... handlers) {
            stateRepository.setAnyBefore(Arrays.asList(handlers));
            return this;
        }


        @SafeVarargs
        public final StateRepositoryBuilder<ENTITY, STATE> setAnyAfter(AfterTransition<ENTITY>... handlers) {
            stateRepository.setAnyAfter(Arrays.asList(handlers));
            return this;
        }

        public StateRepositoryBuilder<ENTITY, STATE> setAvailableStates(Set<STATE> availableStates) {
            stateRepository.setAvailableStates(availableStates);
            return this;
        }

        public StateRepositoryBuilder<ENTITY, STATE> setUnhandledMessageProcessor(UnhandledMessageProcessor<ENTITY> unhandledMessageProcessor) {
            stateRepository.setUnhandledMessageProcessor(unhandledMessageProcessor);
            return this;
        }

        public From<ENTITY, STATE> defineTransitions() {
            return stateRepository.new StateTransition(stateRepository);
        }
    }

    @SuppressWarnings("unchecked")
    public interface From<ENTITY, STATE extends Enum<STATE>> {
        To<ENTITY, STATE> from(STATE... states);
    }

    @SuppressWarnings("unchecked")
    public interface To<ENTITY, STATE extends Enum<STATE>> {
        CompleteTransition<ENTITY, STATE> to(STATE... states);
    }

    @SuppressWarnings("unchecked")
    public interface CompleteTransition<ENTITY, STATE extends Enum<STATE>> {
        CompleteTransition<ENTITY, STATE> before(BeforeTransition<ENTITY>... handlers);

        CompleteTransition<ENTITY, STATE> after(AfterTransition<ENTITY>... handlers);

        From<ENTITY, STATE> and();

        StateRepository<ENTITY, STATE> build();
    }

    public class StateTransition implements To<ENTITY, STATE>, From<ENTITY, STATE>, CompleteTransition<ENTITY, STATE> {
        private final StateRepository<ENTITY, STATE> stateRepository;
        private Set<STATE> from = new HashSet<>(), to = new HashSet<>();
        private List<BeforeTransition<ENTITY>> beforeTransitions = new ArrayList<>();
        private List<AfterTransition<ENTITY>> afterTransitions = new ArrayList<>();

        public StateTransition(StateRepository<ENTITY, STATE> stateRepository) {
            this.stateRepository = stateRepository;
        }

        @SafeVarargs
        private final void checkAndFillStates(Set<STATE> whereToFill, STATE... states) {
            if (states == null || states.length == 0) {
                throw new IllegalArgumentException("No states supplied to from!");
            }

            for (STATE state : states) {
                if (!stateRepository.availableStates.contains(state)) {
                    throw new IllegalArgumentException("State " + state + " is not within available states");
                }
            }
            whereToFill.addAll(Arrays.asList(states));
        }

        @SafeVarargs
        @Override
        public final To<ENTITY, STATE> from(STATE... states) {
            checkAndFillStates(from, states);
            return this;
        }

        @SafeVarargs
        @Override
        public final CompleteTransition<ENTITY, STATE> to(STATE... states) {
            checkAndFillStates(to, states);
            return this;
        }

        @SafeVarargs
        @Override
        public final CompleteTransition<ENTITY, STATE> before(BeforeTransition<ENTITY>... handlers) {
            beforeTransitions.addAll(Arrays.asList(handlers));
            return this;
        }

        @SafeVarargs
        @Override
        public final CompleteTransition<ENTITY, STATE> after(AfterTransition<ENTITY>... handlers) {
            afterTransitions.addAll(Arrays.asList(handlers));
            return this;
        }

        private void finalizeStep() {
            Map<STATE, Map<STATE, StateRepository<ENTITY, STATE>.Processors>> states = stateRepository.stateMap;

            for (STATE toState : to) {
                Map<STATE, StateRepository<ENTITY, STATE>.Processors> toMap = states.computeIfAbsent(toState, k -> new HashMap<>());

                for (STATE fromState : from) {
                    toMap.putIfAbsent(fromState, stateRepository.new Processors());
                    StateRepository<ENTITY, STATE>.Processors processors = toMap.get(fromState);
                    processors.getBeforeHandlers().addAll(beforeTransitions);
                    processors.getAfterHandlers().addAll(afterTransitions);
                }
            }
        }

        @Override
        public From<ENTITY, STATE> and() {
            finalizeStep();
            return stateRepository.new StateTransition(stateRepository);
        }

        @Override
        public StateRepository<ENTITY, STATE> build() {
            finalizeStep();
            return stateRepository;
        }
    }
}

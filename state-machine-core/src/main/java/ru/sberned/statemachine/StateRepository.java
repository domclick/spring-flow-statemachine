package ru.sberned.statemachine;

import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.state.*;

import java.util.*;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 22/03/2017.
 */
public class StateRepository<ENTITY extends HasStateAndId<ID, STATE>, STATE, ID> {
    private Map<STATE, Map<STATE, Processors>> stateMap = new HashMap<>();
    private Set<STATE> availableStates;
    private List<BeforeAnyTransition<ENTITY, STATE>> beforeAllHandlers = new ArrayList<>();
    private List<AfterAnyTransition<ENTITY, STATE>> afterAllHandlers = new ArrayList<>();
    private UnhandledMessageProcessor<ID, STATE> unhandledMessageProcessor;

    boolean isValidTransition(STATE from, STATE to) {
        return stateMap.get(to) != null && stateMap.get(to).get(from) != null;
    }

    private void setAvailableStates(Set<STATE> availableStates) {
        this.availableStates = availableStates;
    }

    private void setAnyBefore(List<BeforeAnyTransition<ENTITY, STATE>> anyBefore) {
        beforeAllHandlers.addAll(anyBefore);
    }

    private void setAnyAfter(List<AfterAnyTransition<ENTITY, STATE>> anyAfter) {
        afterAllHandlers.addAll(anyAfter);
    }

    List<BeforeTransition<ENTITY>> getBefore(STATE from, STATE to) {
        return stateMap.get(to).get(from).getBeforeHandlers();
    }

    List<AfterTransition<ENTITY>> getAfter(STATE from, STATE to) {
        return stateMap.get(to).get(from).getAfterHandlers();
    }

    List<BeforeAnyTransition<ENTITY, STATE>> getBeforeAll() {
        return beforeAllHandlers;
    }

    List<AfterAnyTransition<ENTITY, STATE>> getAfterAll() {
        return afterAllHandlers;
    }

    UnhandledMessageProcessor<ID, STATE> getUnhandledMessageProcessor() {
        return unhandledMessageProcessor;
    }

    private void setUnhandledMessageProcessor(UnhandledMessageProcessor<ID, STATE> unhandledMessageProcessor) {
        this.unhandledMessageProcessor = unhandledMessageProcessor;
    }

    @SuppressWarnings("unchecked")
    public interface From<ENTITY extends HasStateAndId<ID, STATE>, STATE, ID> {
        To<ENTITY, STATE, ID> from(STATE... states);
    }


    @SuppressWarnings("unchecked")
    public interface To<ENTITY extends HasStateAndId<ID, STATE>, STATE, ID> {
        CompleteTransition<ENTITY, STATE, ID> to(STATE... states);
    }

    @SuppressWarnings("unchecked")
    public interface CompleteTransition<ENTITY extends HasStateAndId<ID, STATE>, STATE, ID> {
        CompleteTransition<ENTITY, STATE, ID> before(BeforeTransition<ENTITY>... handlers);

        CompleteTransition<ENTITY, STATE, ID> after(AfterTransition<ENTITY>... handlers);

        From<ENTITY, STATE, ID> and();

        StateRepository<ENTITY, STATE, ID> build();
    }

    public static class StateRepositoryBuilder<ENTITY extends HasStateAndId<ID, STATE>, STATE, ID> {
        private StateRepository<ENTITY, STATE, ID> stateRepository;

        private StateRepositoryBuilder() {
            stateRepository = new StateRepository<>();
        }

        public static <ENTITY extends HasStateAndId<ID, STATE>, STATE, ID> StateRepositoryBuilder<ENTITY, STATE, ID> configure() {
            return new StateRepositoryBuilder<>();
        }


        @SafeVarargs
        public final StateRepositoryBuilder<ENTITY, STATE, ID> setAnyBefore(BeforeAnyTransition<ENTITY, STATE>... handlers) {
            stateRepository.setAnyBefore(Arrays.asList(handlers));
            return this;
        }


        @SafeVarargs
        public final StateRepositoryBuilder<ENTITY, STATE, ID> setAnyAfter(AfterAnyTransition<ENTITY, STATE>... handlers) {
            stateRepository.setAnyAfter(Arrays.asList(handlers));
            return this;
        }

        public StateRepositoryBuilder<ENTITY, STATE, ID> setAvailableStates(Set<STATE> availableStates) {
            stateRepository.setAvailableStates(availableStates);
            return this;
        }

        public StateRepositoryBuilder<ENTITY, STATE, ID> setUnhandledMessageProcessor(UnhandledMessageProcessor<ID, STATE> unhandledMessageProcessor) {
            stateRepository.setUnhandledMessageProcessor(unhandledMessageProcessor);
            return this;
        }

        public From<ENTITY, STATE, ID> defineTransitions() {
            return stateRepository.new StateTransition(stateRepository);
        }
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

    public class StateTransition implements To<ENTITY, STATE, ID>, From<ENTITY, STATE, ID>, CompleteTransition<ENTITY, STATE, ID> {
        private final StateRepository<ENTITY, STATE, ID> stateRepository;
        private Set<STATE> from = new HashSet<>(), to = new HashSet<>();
        private List<BeforeTransition<ENTITY>> beforeTransitions = new ArrayList<>();
        private List<AfterTransition<ENTITY>> afterTransitions = new ArrayList<>();

        public StateTransition(StateRepository<ENTITY, STATE, ID> stateRepository) {
            this.stateRepository = stateRepository;
        }

        @SafeVarargs
        private final void checkAndFillStates(Set<STATE> whereToFill, STATE... states) {
            if (states == null || states.length == 0) {
                throw new IllegalArgumentException("No states supplied!");
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
        public final To<ENTITY, STATE, ID> from(STATE... states) {
            checkAndFillStates(from, states);
            return this;
        }

        @SafeVarargs
        @Override
        public final CompleteTransition<ENTITY, STATE, ID> to(STATE... states) {
            checkAndFillStates(to, states);
            return this;
        }

        @SafeVarargs
        @Override
        public final CompleteTransition<ENTITY, STATE, ID> before(BeforeTransition<ENTITY>... handlers) {
            beforeTransitions.addAll(Arrays.asList(handlers));
            return this;
        }

        @SafeVarargs
        @Override
        public final CompleteTransition<ENTITY, STATE, ID> after(AfterTransition<ENTITY>... handlers) {
            afterTransitions.addAll(Arrays.asList(handlers));
            return this;
        }

        private void finalizeStep() {
            Map<STATE, Map<STATE, StateRepository<ENTITY, STATE, ID>.Processors>> states = stateRepository.stateMap;

            for (STATE toState : to) {
                Map<STATE, StateRepository<ENTITY, STATE, ID>.Processors> toMap = states.computeIfAbsent(toState, k -> new HashMap<>());

                for (STATE fromState : from) {
                    toMap.putIfAbsent(fromState, stateRepository.new Processors());
                    StateRepository<ENTITY, STATE, ID>.Processors processors = toMap.get(fromState);
                    processors.getBeforeHandlers().addAll(beforeTransitions);
                    processors.getAfterHandlers().addAll(afterTransitions);
                }
            }
        }

        @Override
        public From<ENTITY, STATE, ID> and() {
            finalizeStep();
            return stateRepository.new StateTransition(stateRepository);
        }

        @Override
        public StateRepository<ENTITY, STATE, ID> build() {
            finalizeStep();
            return stateRepository;
        }
    }
}

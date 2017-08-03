package ru.sberned.samples.loading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.samples.loading.model.states.IAmSimpleState;
import ru.sberned.samples.loading.service.StateInfoService;
import ru.sberned.samples.loading.store.ItemStore;
import ru.sberned.statemachine.StateMachine;
import ru.sberned.statemachine.StateRepository;
import ru.sberned.statemachine.StateRepository.StateRepositoryBuilder;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.state.BeforeAnyTransition;
import ru.sberned.statemachine.state.ItemWithStateProvider;
import ru.sberned.statemachine.state.StateChanger;

import java.util.Set;


/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 25/04/2017.
 */
@SuppressWarnings("unchecked")
@Configuration
public class SimpleConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleConfig.class);

    @Autowired
    private LockProvider lockProvider;

    @Autowired
    private StateInfoService stateInfoService;

    @Bean
    public StateMachine<SimpleItem, IAmSimpleState, String> stateMachine() {
        Set<IAmSimpleState> availableStates = stateInfoService.obtainAvailableStates();
        StateRepository.From<SimpleItem, IAmSimpleState, String> builderFrom = StateRepositoryBuilder.<SimpleItem, IAmSimpleState, String>configure()
                .setAvailableStates(availableStates)
                .setUnhandledMessageProcessor((item, state, type, ex) -> LOGGER.error("Got unhandled item with id {}, issue is {}", item, type))
                .setAnyBefore((BeforeAnyTransition<SimpleItem, IAmSimpleState>) (item, state) -> {
                    LOGGER.info("Started working on item with id {}", item.getId());
                    return true;
                })
                .defineTransitions();
        StateRepository.CompleteTransition<SimpleItem, IAmSimpleState, String> completeTransition = null;
        for (IAmSimpleState fromState : availableStates) {
            Iterable<IAmSimpleState> toStates = stateInfoService.loadAvailableToStates(fromState);
            for (IAmSimpleState toState : toStates) {
                if (completeTransition != null) builderFrom = completeTransition.and();
                completeTransition = builderFrom
                        .from(fromState)
                        .to(toState)
                        .before(stateInfoService.getBeforeHandlers(fromState, toState))
                        .after(stateInfoService.getAfterHandlers(fromState, toState));
            }
        }
        StateRepository<SimpleItem, IAmSimpleState, String> repository = completeTransition.build();

        StateMachine<SimpleItem, IAmSimpleState, String> stateMachine = new StateMachine<>(stateProvider(), stateChanger(), lockProvider);
        stateMachine.setStateRepository(repository);
        return stateMachine;
    }

    @Bean
    public ItemWithStateProvider<SimpleItem, String> stateProvider() {
        return new ListStateProvider();
    }

    @Bean
    public StateChanger<SimpleItem, IAmSimpleState> stateChanger() {
        return new StateHandler();
    }

    public static class ListStateProvider implements ItemWithStateProvider<SimpleItem, String> {
        @Autowired
        private ItemStore store;

        @Override
        public SimpleItem getItemById(String id) {
            return store.getItem(id);
        }
    }

    public static class StateHandler implements StateChanger<SimpleItem, IAmSimpleState> {
        @Autowired
        private ItemStore store;

        @Override
        public void moveToState(IAmSimpleState state, SimpleItem item, Object... infos) {
            SimpleItem itemFound = store.getItem(item);
            if (itemFound != null) {
                itemFound.setState(state);
            } else {
                throw new RuntimeException("Item not found");
            }
        }
    }
}

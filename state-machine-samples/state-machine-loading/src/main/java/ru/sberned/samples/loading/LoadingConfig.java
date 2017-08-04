package ru.sberned.samples.loading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.samples.loading.model.states.IAmLoadableState;
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
public class LoadingConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadingConfig.class);

    @Autowired
    private LockProvider lockProvider;

    @Autowired
    private StateInfoService stateInfoService;

    @Bean
    public StateMachine<LoadableItem, IAmLoadableState, String> stateMachine() {
        StateRepository<LoadableItem, IAmLoadableState, String> repository = createRepository();

        StateMachine<LoadableItem, IAmLoadableState, String> stateMachine = new StateMachine<>(stateProvider(), stateChanger(), lockProvider);
        stateMachine.setStateRepository(repository);
        return stateMachine;
    }

    private StateRepository<LoadableItem, IAmLoadableState, String> createRepository() {
        Set<IAmLoadableState> availableStates = stateInfoService.obtainAvailableStates();
        StateRepository.From<LoadableItem, IAmLoadableState, String> builderFrom = StateRepositoryBuilder.<LoadableItem, IAmLoadableState, String>configure()
                .setAvailableStates(availableStates)
                .setUnhandledMessageProcessor((item, state, type, ex) -> LOGGER.error("Got unhandled item with id {}, issue is {}", item, type))
                .setAnyBefore((BeforeAnyTransition<LoadableItem, IAmLoadableState>) (item, state) -> {
                    LOGGER.info("Started working on item with id {}", item.getId());
                    return true;
                })
                .defineTransitions();
        StateRepository.CompleteTransition<LoadableItem, IAmLoadableState, String> completeTransition = null;
        for (IAmLoadableState fromState : availableStates) {
            Iterable<IAmLoadableState> toStates = stateInfoService.loadAvailableToStates(fromState);
            for (IAmLoadableState toState : toStates) {
                if (completeTransition != null) builderFrom = completeTransition.and();
                completeTransition = builderFrom
                        .from(fromState)
                        .to(toState)
                        .before(stateInfoService.getBeforeHandlers(fromState, toState))
                        .after(stateInfoService.getAfterHandlers(fromState, toState));
            }
        }
        return completeTransition.build();
    }

    public void reinitStateMachine(){
        stateMachine().setStateRepository(createRepository());
    }

    @Bean
    public ItemWithStateProvider<LoadableItem, String> stateProvider() {
        return new ListStateProvider();
    }

    @Bean
    public StateChanger<LoadableItem, IAmLoadableState> stateChanger() {
        return new StateHandler();
    }

    public static class ListStateProvider implements ItemWithStateProvider<LoadableItem, String> {
        @Autowired
        private ItemStore store;

        @Override
        public LoadableItem getItemById(String id) {
            return store.getItem(id);
        }
    }

    public static class StateHandler implements StateChanger<LoadableItem, IAmLoadableState> {
        @Autowired
        private ItemStore store;

        @Override
        public void moveToState(IAmLoadableState state, LoadableItem item, Object... infos) {
            LoadableItem itemFound = store.getItem(item);
            if (itemFound != null) {
                itemFound.setState(state);
            } else {
                throw new RuntimeException("Item not found");
            }
        }
    }
}

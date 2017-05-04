package ru.sberned.statemachine.samples.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.StateListener;
import ru.sberned.statemachine.StateMachine;
import ru.sberned.statemachine.samples.simple.store.ItemStore;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.state.StateProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by jpatuk on 25/04/2017.
 */
@Configuration
public class SimpleConfig {

    @Bean
    public StateListener<SimpleItem, SimpleState, String> stateListener() {
        return new StateListener<>();
    }

    @Bean
    public StateMachine<SimpleItem, SimpleState> stateMachine() {
        return new StateMachine<>();
    }

    @Bean
    public StateProvider<SimpleItem, SimpleState, String> stateProvider() {
        return new MapStateProvider();
    }

    @Bean
    public StateChanger<SimpleItem, SimpleState> stateChanger() {
        return new StateHandler();
    }

    public static class MapStateProvider implements StateProvider<SimpleItem, SimpleState, String> {
        @Autowired
        private ItemStore store;

        @Override
        public Map<SimpleItem, SimpleState> getItemsState(List<String> ids) {
            return store.getItemsById(ids).stream()
                    .collect(Collectors.toMap(Function.identity(), SimpleItem::getState));
        }
    }

    public static class StateHandler implements StateChanger<SimpleItem, SimpleState> {
        @Autowired
        private ItemStore store;

        @Override
        public void moveToState(SimpleState state, SimpleItem item) {
            SimpleItem itemFound = store.getItem(item);
            if (itemFound != null) {
                itemFound.setState(state);
            } else {
                throw new RuntimeException("Item not found");
            }
        }
    }
}

package ru.sberned.statemachine.samples.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.StateMachine;
import ru.sberned.statemachine.samples.simple.store.ItemStore;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.state.ItemWithStateProvider;

/**
 * Created by jpatuk on 25/04/2017.
 */
@Configuration
public class SimpleConfig {

    @Bean
    public StateMachine<SimpleItem, SimpleState, String> stateListener() {
        return new StateMachine<>();
    }

    @Bean
    public ItemWithStateProvider<SimpleItem, String> stateProvider() {
        return new ListStateProvider();
    }

    @Bean
    public StateChanger<SimpleItem, SimpleState> stateChanger() {
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

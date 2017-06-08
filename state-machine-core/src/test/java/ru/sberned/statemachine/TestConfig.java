package ru.sberned.statemachine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.Item;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.lock.MapLockProvider;
import ru.sberned.statemachine.state.ItemWithStateProvider;

import java.util.List;

/**
 * Created by jpatuk on 10/11/2016.
 */
@Configuration
public class TestConfig {
    @Bean
    public ItemWithStateProvider<Item, String> stateProvider() {
        return new CustomStateProvider();
    }

    @Bean
    public LockProvider stateLock() {
        return new MapLockProvider();
    }

    @Bean
    public StateMachine<Item, CustomState, String> stateListener() {
        return new StateMachine<>();
    }

    @Bean
    public StateChanger<Item, CustomState> stateChanger() {
        return new TestOnTransition();
    }

    public static class CustomStateProvider implements ItemWithStateProvider<Item, String> {
        private List<Item> items;

        void setItems(List<Item> items) {
            this.items = items;
        }

        @Override
        public Item getItemById(String id) {
            for (Item item : items) {
                if (item.getId().equals(id)) {
                    return item;
                }
            }
            return null;
        }
    }

    private class TestOnTransition implements StateChanger<Item, CustomState> {

        @Override
        public void moveToState(CustomState state, Item item) {
            System.out.println("state is " + state + ", item id is " + item.getId() + ", item state is " + item.getState());
            item.state = state;
        }
    }
}

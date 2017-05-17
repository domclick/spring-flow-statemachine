package ru.sberned.statemachine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.Item;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.lock.MapLockProvider;
import ru.sberned.statemachine.state.ItemWithStateProvider;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    public static class CustomStateProvider implements ItemWithStateProvider<Item, String> {
        private List<Item> items;

        void setItems(List<Item> items) {
            this.items = items;
        }

        @Override
        public Collection<Item> getItemsByIds(List<String> ids) {
            return items.stream().filter(item -> ids.contains(item.getId())).collect(Collectors.toList());
        }
    }
}

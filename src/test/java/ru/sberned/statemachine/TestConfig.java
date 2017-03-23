package ru.sberned.statemachine;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.config.CustomState;
import ru.sberned.statemachine.config.Item;
import ru.sberned.statemachine.lock.MapLockProvider;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.state.StateProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by empatuk on 10/11/2016.
 */
@Configuration
@EnableAutoConfiguration(exclude = StateMachineConfig.class)
public class TestConfig {


    @Bean
    public StateProvider<Item, CustomState, String> stateProvider() {
        return new CustomStateProvider();
    }

    @Bean
    public LockProvider<String> stateLock() {
        return new MapLockProvider<>();
    }

    @Bean
    public StateListener<Item, CustomState, String> stateListener() {
        return new StateListener<>();
    }

    public static class CustomStateProvider implements StateProvider<Item, CustomState, String> {
        private List<Item> items;

        @Override
        public Map<Item, CustomState> getItemsState(List<String> ids) {
            return items.stream()
                    .filter(item -> ids.contains(item.id))
                    .collect(Collectors.toMap(Function.identity(), Item::getState));
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }
    }
}

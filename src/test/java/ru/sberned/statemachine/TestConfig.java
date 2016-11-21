package ru.sberned.statemachine;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import ru.sberned.statemachine.state.StateProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by empatuk on 10/11/2016.
 */
@Configuration
@EnableAutoConfiguration
public class TestConfig {
    @Bean
    public StateProvider<Item, CustomState> stateProvider() {
        return new CustomStateProvider();
    }

    @Bean
    public StateListener<Item, CustomState> stateListener() {
        return new StateListener<Item, CustomState>() {
            @EventListener
            public void handleTestStateChanged(TestStateChangedEvent event) {
                handleStateChanged(event.getStateChangedEvent());
            }
        };
    }

    public static class CustomStateProvider implements StateProvider<Item, CustomState> {
        private List<Item> items;

        @Override
        public Map<Item, CustomState> getItemsState(List<String> ids) {
            return items.stream()
                    .filter(item -> ids.contains(item.id))
                    .collect(Collectors.toMap(Item::getState, Function.identity()));
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }
    }
}

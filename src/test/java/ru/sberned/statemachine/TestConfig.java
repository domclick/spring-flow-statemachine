package ru.sberned.statemachine;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import ru.sberned.statemachine.config.CustomState;
import ru.sberned.statemachine.config.Item;
import ru.sberned.statemachine.config.TestStateChangedEvent;
import ru.sberned.statemachine.lock.MapStateLock;
import ru.sberned.statemachine.lock.StateLock;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.processor.UnhandledMessageProcessorImpl;
import ru.sberned.statemachine.state.StateProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by empatuk on 10/11/2016.
 */
@Configuration
@EnableAutoConfiguration(exclude = StateConfig.class)
public class TestConfig {
    @Bean
    public StateProvider<Item, CustomState, String> stateProvider() {
        return new CustomStateProvider();
    }

    @Bean
    public StateLock<String> stateLock() {
        return new MapStateLock<>();
    }

    @Bean
    public UnhandledMessageProcessor<Item> unhandledMessageProcessor() {
        return new UnhandledMessageProcessorImpl<>();
    }

    @Bean
    public StateListener<Item, CustomState, String> stateListener() {
        return new StateListener<Item, CustomState, String>() {
            @EventListener
            public void handleTestStateChanged(TestStateChangedEvent event) {
                handleStateChanged(event.getStateChangedEvent());
            }
        };
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

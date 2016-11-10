package ru.sberned.statemachine;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.state.StateProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by empatuk on 10/11/2016.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan("ru.sberned.statemachine")
public class TestConfig {
    @Bean
    public StateProvider<Item, CustomState> stateProvider() {
        return new CustomStateProvider();
    }

    public static class CustomStateProvider implements StateProvider<Item, CustomState> {
        private Map<Item, CustomState> items;

        @Override
        public Map<Item, CustomState> getItemsState(List<String> ids) {
            return items.entrySet().stream().filter(entry -> ids.contains(entry.getKey().id))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        public void setItems(Map<Item, CustomState> items) {
            this.items = items;
        }
    }

    public static class Item {
        String id;

        public Item(String id) {
            this.id = id;
        }
    }

    public enum CustomState {START, STATE1, STATE2, STATE3, STATE4, FINISH, CANCEL}
}

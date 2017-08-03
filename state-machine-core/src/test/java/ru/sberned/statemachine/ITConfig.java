package ru.sberned.statemachine;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.lock.MapLockProvider;
import ru.sberned.statemachine.state.ItemWithStateProvider;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.DBStateProvider;
import ru.sberned.statemachine.util.Item;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 25/04/2017.
 */
@Configuration
@EnableAutoConfiguration
public class ITConfig {
    @Bean
    public ItemWithStateProvider<Item, String> stateProvider() {
        return new DBStateProvider();
    }

    @Bean
    public StateChanger<Item, CustomState> stateChanger() {
        return new DBStateProvider();
    }

    @Bean
    public LockProvider stateLock() {
        return new MapLockProvider();
    }

    @Bean
    public StateMachine<Item, CustomState, String> stateMachine() {
        return new StateMachine<>(stateProvider(), stateChanger(), stateLock());
    }
}

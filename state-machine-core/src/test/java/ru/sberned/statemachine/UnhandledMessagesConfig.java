package ru.sberned.statemachine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.lock.MapLockProvider;
import ru.sberned.statemachine.state.ItemWithStateProvider;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.CustomStateProvider;
import ru.sberned.statemachine.util.Item;

/**
 * Created by jpatuk on 17/06/2017.
 */
@Configuration
public class UnhandledMessagesConfig {
    @Bean
    public ItemWithStateProvider<Item, String> stateProvider() {
        return new CustomStateProvider();
    }

    @Bean
    public LockProvider stateLock() {
        return new MapLockProvider();
    }

    @Bean
    public StateMachine<Item, CustomState, String> stateMachineWithTimeout() {
        return new StateMachine<>(stateProvider(), timeoutStateChanger(), stateLock());
    }

    @Bean
    public StateChanger<Item, CustomState> timeoutStateChanger() {
        return new TimeoutOnTransition();
    }

    private class TimeoutOnTransition implements StateChanger<Item, CustomState> {

        @Override
        public void moveToState(CustomState state, Item item, Object... infos) {
            try {
                Thread.sleep(2000);
                item.state = state;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

package ru.sberned.statemachine;

import org.junit.Test;
import ru.sberned.statemachine.lock.MapLockProvider;
import ru.sberned.statemachine.state.ItemWithStateProvider;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.util.ItemTwo;
import ru.sberned.statemachine.util.StateTwo;
import ru.sberned.statemachine.util.StateTwo.FinishStateTwo;
import ru.sberned.statemachine.util.StateTwo.MiddleStateTwo;
import ru.sberned.statemachine.util.StateTwo.StartStateTwo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;

public class TestWithNonEnumStates {
    @Test
    public void testStateUpdatedWhenNotEnum() throws InterruptedException, ExecutionException {
        StateRepository<ItemTwo, StateTwo, String> repo = StateRepository.StateRepositoryBuilder.<ItemTwo, StateTwo, String>configure()
                .setAvailableStates(new HashSet<>(Arrays.asList(new StartStateTwo(), new MiddleStateTwo(), new FinishStateTwo())))
                .defineTransitions()
                    .from(new StartStateTwo())
                    .to(new MiddleStateTwo(), new FinishStateTwo())
                .and()
                    .from(new MiddleStateTwo())
                    .to(new FinishStateTwo())
                .build();
        ItemWithStateProvider<ItemTwo, String> provider = new ItemWithStateProvider<ItemTwo, String>() {
            Map<String, ItemTwo> m = new HashMap<>();

            @Override
            public ItemTwo getItemById(String s) {
                return m.computeIfAbsent(s, ItemTwo::new);
            }
        };
        StateChanger<ItemTwo, StateTwo> changer = (state, item, infos) -> item.setState(state);
        StateMachine<ItemTwo, StateTwo, String> stateMachine = new StateMachine<>(provider, changer, new MapLockProvider());
        stateMachine.setStateRepository(repo);
        ItemTwo item1 = provider.getItemById("1");
        stateMachine.changeState(singleton("1"), new MiddleStateTwo(), null).get("1").get();
        stateMachine.changeState(singleton("1"), new FinishStateTwo(), null).get("1").get();
        assertEquals(new FinishStateTwo(), item1.getState());
    }

}

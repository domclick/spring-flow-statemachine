package ru.sberned.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import ru.sberned.statemachine.state.AfterTransition;
import ru.sberned.statemachine.state.StateChangedEvent;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.DBStateProvider;
import ru.sberned.statemachine.util.Item;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static ru.sberned.statemachine.util.CustomState.FINISH;
import static ru.sberned.statemachine.util.CustomState.START;
import static ru.sberned.statemachine.util.CustomState.STATE1;

/**
 * Created by jpatuk on 25/04/2017.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = ITConfig.class
)
public class StateMachineIT {
    @Autowired
    private StateMachine<Item, CustomState, String> stateListener;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private DBStateProvider stateProvider;

    @Before
    public void before() {
        stateProvider.cleanItems();
        stateProvider.insertItems(Arrays.asList(new Item("1", CustomState.START),
                new Item("2", CustomState.START),
                new Item("3", CustomState.START),
                new Item("4", CustomState.START),
                new Item("5", CustomState.START),
                new Item("6", CustomState.START),
                new Item("7", CustomState.START)));
    }

    @Test
    public void testStateUpdated() {
        StateRepository.StateRepositoryBuilder<Item, CustomState> builder = new StateRepository.StateRepositoryBuilder<>();
        StateRepository<Item, CustomState> repository = builder
                .setStateChanger(stateProvider)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(STATE1)
                .and()
                .from(STATE1)
                .to(CustomState.FINISH)
                .build();

        stateListener.setStateRepository(repository);
        List<String> items = Arrays.asList("1", "2");
        publisher.publishEvent(new StateChangedEvent<>(this, items, STATE1));

        verifyState(items, STATE1);

        publisher.publishEvent(new StateChangedEvent<>(this, items, FINISH));

        verifyState(items, FINISH);
    }

    @Test
    public void testRollback() {
        final String failedId = "2";
        StateRepository.StateRepositoryBuilder<Item, CustomState> builder = new StateRepository.StateRepositoryBuilder<>();
        StateRepository<Item, CustomState> repository = builder
                .setStateChanger(stateProvider)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .setAnyAfter((AfterTransition<Item>) item -> {
                    if (item.getId().equals(failedId)) {
                        throw new RuntimeException("just to check");
                    }
                })
                .defineTransitions()
                .from(CustomState.START)
                .to(STATE1)
                .build();

        stateListener.setStateRepository(repository);
        List<String> items = Arrays.asList("1", "2", "3", "4", "5", "6", "7");
        publisher.publishEvent(new StateChangedEvent<>(this, items, STATE1));

        verifyState(Arrays.asList("1", "3", "4", "5", "6", "7"), STATE1);
        verifyState(Collections.singletonList("2"), START);
    }

    private void verifyState(List<String> ids, CustomState expectedState) {
        Collection<Item> itemsList = stateProvider.getItemsByIds(ids);
        itemsList.forEach(item ->
                assertEquals(expectedState, item.getState())
        );
    }
}

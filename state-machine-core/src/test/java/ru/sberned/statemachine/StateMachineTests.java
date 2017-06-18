package ru.sberned.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import ru.sberned.statemachine.StateRepository.StateRepositoryBuilder;
import ru.sberned.statemachine.state.*;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.CustomStateProvider;
import ru.sberned.statemachine.util.Item;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor;

import java.util.*;

import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = TestConfig.class
)
public class StateMachineTests {
    @Autowired
    private StateMachine<Item, CustomState, String> stateMachine;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private CustomStateProvider stateProvider;
    @SpyBean
    private StateChanger<Item, CustomState> onTransition;

    private BeforeTransition<Item> beforeTransition1 = mock(BeforeTransition.class);
    private BeforeTransition<Item> beforeTransition2 = mock(BeforeTransition.class);
    private AfterTransition<Item> afterTransition1 = mock(AfterTransition.class);
    private AfterTransition<Item> afterTransition2 = mock(AfterTransition.class);

    private StateRepository<Item, CustomState, String> getDefaultTransition(UnhandledMessageProcessor<String> unhandled) {
        return StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .setUnhandledMessageProcessor(unhandled)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .build();
    }

    @Before
    public void before() {
        List<Item> itemsList = new ArrayList<>();
        itemsList.add(new Item("1", CustomState.START));
        itemsList.add(new Item("2", CustomState.STATE1));
        itemsList.add(new Item("3", CustomState.STATE2));
        itemsList.add(new Item("4", CustomState.START));
        itemsList.add(new Item("5", CustomState.STATE3));
        itemsList.add(new Item("6", CustomState.START));
        stateProvider.setItems(itemsList);
    }

    @Test
    public void testCorrectStatesNoHandlers() {
        StateRepository<Item, CustomState, String> stateHolder = getDefaultTransition(null);

        stateMachine.setStateRepository(stateHolder);
        publisher.publishEvent(new StateChangedEvent("1", CustomState.STATE1));
        verify(onTransition, timeout(500).times(1)).moveToState(CustomState.STATE1, new Item("1", CustomState.START));
    }

    @Test
    public void testCorrectStatesWithHandlersInOrder() throws Exception {
        StateRepository<Item, CustomState, String> stateHolder = StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .before(beforeTransition1, beforeTransition2)
                .after(afterTransition1, afterTransition2)
                .build();

        stateMachine.setStateRepository(stateHolder);

        Item item = new Item("1", CustomState.START);
        when(beforeTransition1.beforeTransition(item)).thenReturn(true);
        when(beforeTransition2.beforeTransition(item)).thenReturn(true);

        stateMachine.handleMessage("1", CustomState.STATE1, null);

        InOrder inOrder = inOrder(beforeTransition1, beforeTransition2, onTransition, afterTransition1, afterTransition2);
        inOrder.verify(beforeTransition1, times(1)).beforeTransition(item);
        inOrder.verify(beforeTransition2, times(1)).beforeTransition(item);
        inOrder.verify(onTransition, times(1)).moveToState(CustomState.STATE1, item);
        inOrder.verify(afterTransition1, times(1)).afterTransition(item);
        inOrder.verify(afterTransition2, times(1)).afterTransition(item);
    }

    @Test
    public void testConflictingEventsLeadToOnlyOneStateChange() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .and()
                .from(CustomState.START)
                .to(CustomState.STATE2)
                .build();

        stateMachine.setStateRepository(stateHolder);

        publisher.publishEvent(new StateChangedEvent(Arrays.asList("1", "4"), CustomState.STATE1));
        publisher.publishEvent(new StateChangedEvent(Arrays.asList("1", "4"), CustomState.STATE2));
        publisher.publishEvent(new StateChangedEvent("6", CustomState.STATE1));
        publisher.publishEvent(new StateChangedEvent("6", CustomState.STATE2));

        verify(onTransition, timeout(500).times(1)).moveToState(any(CustomState.class), eq(new Item("1", CustomState.START)));
        verify(onTransition, timeout(500).times(1)).moveToState(any(CustomState.class), eq(new Item("4", CustomState.START)));
        verify(onTransition, timeout(500).times(1)).moveToState(any(CustomState.class), eq(new Item("6", CustomState.START)));
    }

    @Test
    public void testNoTransition() {
        StateRepository<Item, CustomState, String> stateHolder = getDefaultTransition(null);

        stateMachine.setStateRepository(stateHolder);
        publisher.publishEvent(new StateChangedEvent("2", CustomState.STATE1));
        verify(onTransition, timeout(500).times(0)).moveToState(CustomState.STATE1, new Item("2", CustomState.STATE1));
    }

    @Test
    public void testAnyHandlers() throws Exception {
        BeforeAnyTransition<Item, CustomState> beforeAny = mock(BeforeAnyTransition.class);
        AfterAnyTransition<Item, CustomState> afterAny = mock(AfterAnyTransition.class);
        StateRepository<Item, CustomState, String> stateHolder = StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .setAnyBefore(beforeAny)
                .setAnyAfter(afterAny)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .before(beforeTransition1)
                .after(afterTransition1)
                .build();

        stateMachine.setStateRepository(stateHolder);
        Item item = new Item("1", CustomState.START);
        when(beforeTransition1.beforeTransition(item)).thenReturn(true);
        when(beforeAny.beforeTransition(item, CustomState.STATE1)).thenReturn(true);

        stateMachine.handleMessage("1", CustomState.STATE1, null);

        InOrder inOrder = inOrder(beforeAny, beforeTransition1, onTransition, afterAny, afterTransition1);
        inOrder.verify(beforeAny, times(1)).beforeTransition(item, CustomState.STATE1);
        inOrder.verify(beforeTransition1, times(1)).beforeTransition(item);
        inOrder.verify(onTransition, times(1)).moveToState(CustomState.STATE1, item);
        inOrder.verify(afterTransition1, times(1)).afterTransition(item);
        inOrder.verify(afterAny, times(1)).afterTransition(item, CustomState.STATE1);
    }
}

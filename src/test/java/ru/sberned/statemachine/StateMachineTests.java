package ru.sberned.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import ru.sberned.statemachine.config.CustomState;
import ru.sberned.statemachine.config.Item;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.state.AfterTransition;
import ru.sberned.statemachine.state.BeforeTransition;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.state.StateChangedEvent;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType.*;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = TestConfig.class
)
public class StateMachineTests {
    @Autowired
    private StateListener<Item, CustomState, String> stateListener;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private TestConfig.CustomStateProvider stateProvider;

    private TestOnTransition onTransition = mock(TestOnTransition.class);
    private BeforeTransition<Item> beforeTransition1 = mock(BeforeTransition.class);
    private BeforeTransition<Item> beforeTransition2 = mock(BeforeTransition.class);
    private AfterTransition<Item> afterTransition1 = mock(AfterTransition.class);
    private AfterTransition<Item> afterTransition2 = mock(AfterTransition.class);
    private UnhandledMessageProcessor<Item> processor = mock(UnhandledMessageProcessor.class);

    private class TestOnTransition implements StateChanger<Item, CustomState> {

        @Override
        public void moveToState(CustomState state, Item item) {
            item.state = state;
        }
    }

    private class TimeoutOnTransition implements StateChanger<Item, CustomState> {

        @Override
        public void moveToState(CustomState state, Item item) {
            try {
                Thread.sleep(2000);
                item.state = state;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private StateRepository<Item, CustomState> getDefaultTransition(UnhandledMessageProcessor<Item> unhandled) {
        StateRepository.StateRepositoryBuilder<Item, CustomState> builder = new StateRepository.StateRepositoryBuilder<>();
        return builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
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
        StateRepository<Item, CustomState> stateHolder = getDefaultTransition(null);

        stateListener.setStateRepository(stateHolder);
        publisher.publishEvent(new StateChangedEvent(this, "1", CustomState.STATE1));
        verify(onTransition, timeout(500).times(1)).moveToState(CustomState.STATE1, new Item("1", CustomState.START));
    }

    @Test
    public void testCorrectStatesWithHandlersInOrder() {
        StateRepository.StateRepositoryBuilder<Item, CustomState> builder = new StateRepository.StateRepositoryBuilder<>();
        StateRepository<Item, CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .before(beforeTransition1, beforeTransition2)
                .after(afterTransition1, afterTransition2)
                .build();

        stateListener.setStateRepository(stateHolder);

        Item item = new Item("1", CustomState.START);
        stateListener.handleMessage(item, CustomState.START, CustomState.STATE1);

        InOrder inOrder = inOrder(beforeTransition1, beforeTransition2, onTransition, afterTransition1, afterTransition2);
        inOrder.verify(beforeTransition1, times(1)).beforeTransition(item);
        inOrder.verify(beforeTransition2, times(1)).beforeTransition(item);
        inOrder.verify(onTransition, times(1)).moveToState(CustomState.STATE1, item);
        inOrder.verify(afterTransition1, times(1)).afterTransition(item);
        inOrder.verify(afterTransition2, times(1)).afterTransition(item);
    }

    @Test
    public void testConflictingEventsLeadToOnlyOneStateChange() throws InterruptedException {
        Mockito.doCallRealMethod().when(onTransition).moveToState(any(CustomState.class), any(Item.class));
        StateRepository.StateRepositoryBuilder<Item, CustomState> builder = new StateRepository.StateRepositoryBuilder<>();
        StateRepository<Item, CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .and()
                .from(CustomState.START)
                .to(CustomState.STATE2)
                .build();

        stateListener.setStateRepository(stateHolder);

        publisher.publishEvent(new StateChangedEvent(this, Arrays.asList("1", "4"), CustomState.STATE1));
        publisher.publishEvent(new StateChangedEvent(this, Arrays.asList("1", "4"), CustomState.STATE2));
        publisher.publishEvent(new StateChangedEvent(this, "6", CustomState.STATE1));
        publisher.publishEvent(new StateChangedEvent(this, "6", CustomState.STATE2));

        verify(onTransition, timeout(500).times(1)).moveToState(any(CustomState.class), eq(new Item("1", CustomState.START)));
        verify(onTransition, timeout(500).times(1)).moveToState(any(CustomState.class), eq(new Item("4", CustomState.START)));
        verify(onTransition, timeout(500).times(1)).moveToState(any(CustomState.class), eq(new Item("6", CustomState.START)));
        Map<Item, CustomState> modifiedItems = stateProvider.getItemsState(Arrays.asList("1", "4"));
        assertEquals(modifiedItems.size(), 2);
        Item[] items = modifiedItems.keySet().toArray(new Item[2]);
        assertEquals(items[0].getState(), items[1].getState());
    }

    @Test
    public void testNoTransition() {
        StateRepository<Item, CustomState> stateHolder = getDefaultTransition(null);

        stateListener.setStateRepository(stateHolder);
        publisher.publishEvent(new StateChangedEvent(this, "2", CustomState.STATE1));
        verify(onTransition, timeout(500).times(0)).moveToState(CustomState.STATE1, new Item("2", CustomState.STATE1));
    }

    @Test
    public void testAnyHandlers() {
        StateRepository.StateRepositoryBuilder<Item, CustomState> builder = new StateRepository.StateRepositoryBuilder<>();
        StateRepository<Item, CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .setAnyBefore(beforeTransition2)
                .setAnyAfter(afterTransition2)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .before(beforeTransition1)
                .after(afterTransition1)
                .build();

        stateListener.setStateRepository(stateHolder);
        Item item = new Item("1", CustomState.START);
        stateListener.handleMessage(item, CustomState.START, CustomState.STATE1);

        InOrder inOrder = inOrder(beforeTransition2, beforeTransition1, onTransition, afterTransition2, afterTransition1);
        inOrder.verify(beforeTransition2, times(1)).beforeTransition(item);
        inOrder.verify(beforeTransition1, times(1)).beforeTransition(item);
        inOrder.verify(onTransition, times(1)).moveToState(CustomState.STATE1, item);
        inOrder.verify(afterTransition1, times(1)).afterTransition(item);
        inOrder.verify(afterTransition2, times(1)).afterTransition(item);
    }

    @Test
    public void testUnhandledMessageProcessorTimeout() throws InterruptedException {
        StateRepository.StateRepositoryBuilder<Item, CustomState> builder = new StateRepository.StateRepositoryBuilder<>();
        StateRepository<Item, CustomState> stateHolder = builder
                .setStateChanger(new TimeoutOnTransition())
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .setUnhandledMessageProcessor(processor)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .and()
                .from(CustomState.STATE1)
                .to(CustomState.STATE2)
                .build();

        stateListener.setStateRepository(stateHolder);

        publisher.publishEvent(new StateChangedEvent(this, "1", CustomState.STATE1));
        Thread.sleep(100);
        publisher.publishEvent(new StateChangedEvent(this, "1", CustomState.STATE2));

        verify(processor, timeout(3000).times(1)).process(new Item("1", CustomState.START), TIMEOUT, null);
    }

    @Test
    public void testUnhandledMessageProcessorInvalidState() throws InterruptedException {
        StateRepository<Item, CustomState> stateHolder = getDefaultTransition(processor);

        stateListener.setStateRepository(stateHolder);
        publisher.publishEvent(new StateChangedEvent(this, "1", CustomState.STATE2));

        verify(processor, timeout(1500).times(1)).process(new Item("1", CustomState.START), INVALID_TRANSITION, null);
    }

    @Test
    public void testUnhandledMessageProcessorExecutionException() throws InterruptedException {
        StateRepository<Item, CustomState> stateHolder = getDefaultTransition(processor);
        stateListener.setStateRepository(stateHolder);

        RuntimeException ex = new RuntimeException();
        doThrow(ex).when(onTransition).moveToState(CustomState.STATE1, new Item("1", CustomState.START));

        publisher.publishEvent(new StateChangedEvent(this, "1", CustomState.STATE1));

        verify(processor, timeout(1500).times(1)).process(new Item("1", CustomState.START), EXECUTION_EXCEPTION, ex);
    }

    @Test
    public void testStateNotPresentInStateHolder() throws InterruptedException {
        StateRepository.StateRepositoryBuilder<Item, CustomState> builder = new StateRepository.StateRepositoryBuilder<>();
        StateRepository<Item, CustomState> stateHolder = builder
                .setStateChanger(new TimeoutOnTransition())
                .setAvailableStates(EnumSet.of(CustomState.START, CustomState.FINISH))
                .setUnhandledMessageProcessor(processor)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.FINISH)
                .build();
        stateListener.setStateRepository(stateHolder);

        stateListener.setStateRepository(stateHolder);
        publisher.publishEvent(new StateChangedEvent(this, "1", CustomState.STATE2));

        verify(processor, timeout(1500).times(1)).process(new Item("1", CustomState.START), INVALID_TRANSITION, null);
    }
}

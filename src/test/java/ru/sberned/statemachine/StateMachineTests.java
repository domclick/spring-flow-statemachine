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
import ru.sberned.statemachine.config.TestStateChangedEvent;
import ru.sberned.statemachine.state.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

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

    private class TestOnTransition implements OnTransition<Item, CustomState> {

        @Override
        public void moveToState(CustomState state, Item item) {
            item.state = state;
        }
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
        StateHolder.StateHolderBuilder<Item, CustomState> builder = new StateHolder.StateHolderBuilder<>();
        StateHolder<Item, CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .build();

        stateListener.setStateHolder(stateHolder);
        publisher.publishEvent(new TestStateChangedEvent(this, "1", CustomState.STATE1));
        verify(onTransition, times(1)).moveToState(CustomState.STATE1, new Item("1", CustomState.START));
	}

    @Test
    public void testCorrectStatesWithHandlersInOrder() {
        StateHolder.StateHolderBuilder<Item, CustomState> builder = new StateHolder.StateHolderBuilder<>();
        StateHolder<Item, CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .before(beforeTransition1, beforeTransition2)
                .after(afterTransition1, afterTransition2)
                .build();

        stateListener.setStateHolder(stateHolder);
        publisher.publishEvent(new TestStateChangedEvent(this, "1", CustomState.STATE1));

        Item item = new Item("1", CustomState.START);

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
        StateHolder.StateHolderBuilder<Item, CustomState> builder = new StateHolder.StateHolderBuilder<>();
        StateHolder<Item, CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .and()
                .from(CustomState.START)
                .to(CustomState.STATE2)
                .build();

        stateListener.setStateHolder(stateHolder);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        executor.submit(() -> {
            publisher.publishEvent(new TestStateChangedEvent(this, Arrays.asList("1","4"), CustomState.STATE1));
        });
        executor.submit(() -> {
            publisher.publishEvent(new TestStateChangedEvent(this, Arrays.asList("1","4"), CustomState.STATE2));
        });
        executor.submit(() -> {
            publisher.publishEvent(new TestStateChangedEvent(this, "6", CustomState.STATE1));
        });
        executor.submit(() -> {
            publisher.publishEvent(new TestStateChangedEvent(this, "6", CustomState.STATE2));
        });
        executor.shutdown();
        executor.awaitTermination(100, TimeUnit.SECONDS);

        verify(onTransition, times(1)).moveToState(any(CustomState.class), eq(new Item("1", CustomState.START)));
        verify(onTransition, times(1)).moveToState(any(CustomState.class), eq(new Item("4", CustomState.START)));
        verify(onTransition, times(1)).moveToState(any(CustomState.class), eq(new Item("6", CustomState.START)));
        Map<Item, CustomState> modifiedItems = stateProvider.getItemsState(Arrays.asList("1","4"));
        assertEquals(modifiedItems.size(), 2);
        Item[] items = modifiedItems.keySet().toArray(new Item[2]);
        assertEquals(items[0].getState(), items[1].getState());
    }

    @Test
    public void testNoTransition() {
        StateHolder.StateHolderBuilder<Item, CustomState> builder = new StateHolder.StateHolderBuilder<>();
        StateHolder<Item, CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<CustomState>allOf(CustomState.class))
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .build();

        stateListener.setStateHolder(stateHolder);
        publisher.publishEvent(new TestStateChangedEvent(this, Arrays.asList("1", "2"), CustomState.STATE1));
        verify(onTransition, times(1)).moveToState(CustomState.STATE1, new Item("1", CustomState.START));
        verify(onTransition, times(0)).moveToState(CustomState.STATE1, new Item("2", CustomState.STATE1));
    }

    @Test
    public void testAnyHandlers() {
        StateHolder.StateHolderBuilder<Item, CustomState> builder = new StateHolder.StateHolderBuilder<>();
        StateHolder<Item, CustomState> stateHolder = builder
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

        stateListener.setStateHolder(stateHolder);
        publisher.publishEvent(new TestStateChangedEvent(this, "1", CustomState.STATE1));
        Item item = new Item("1", CustomState.START);

        InOrder inOrder = inOrder(beforeTransition2, beforeTransition1, onTransition, afterTransition2, afterTransition1);
        inOrder.verify(beforeTransition2, times(1)).beforeTransition(item);
        inOrder.verify(beforeTransition1, times(1)).beforeTransition(item);
        inOrder.verify(onTransition, times(1)).moveToState(CustomState.STATE1, item);
        inOrder.verify(afterTransition1, times(1)).afterTransition(item);
        inOrder.verify(afterTransition2, times(1)).afterTransition(item);
    }

}

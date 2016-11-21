package ru.sberned.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import ru.sberned.statemachine.state.*;

import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = TestConfig.class
)
public class StateMachineTests {
	@Autowired
	private StateListener<TestConfig.Item, TestConfig.CustomState> stateListener;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private StateProvider<TestConfig.Item, TestConfig.CustomState> stateProvider;

    private OnTransition<TestConfig.Item, TestConfig.CustomState> onTransition = mock(OnTransition.class);
    private BeforeTransition<TestConfig.Item> beforeTransition1 = mock(BeforeTransition.class);
    private BeforeTransition<TestConfig.Item> beforeTransition2 = mock(BeforeTransition.class);
    private AfterTransition<TestConfig.Item> afterTransition1 = mock(AfterTransition.class);
    private AfterTransition<TestConfig.Item> afterTransition2 = mock(AfterTransition.class);

    @Before
    public void before() {
        Map<TestConfig.Item, TestConfig.CustomState> items = new HashMap<>();
        items.put(new TestConfig.Item("1"), TestConfig.CustomState.START);
        items.put(new TestConfig.Item("2"), TestConfig.CustomState.STATE1);
        items.put(new TestConfig.Item("3"), TestConfig.CustomState.STATE2);
        items.put(new TestConfig.Item("4"), TestConfig.CustomState.START);
        items.put(new TestConfig.Item("5"), TestConfig.CustomState.STATE3);
        items.put(new TestConfig.Item("6"), TestConfig.CustomState.START);
        ((TestConfig.CustomStateProvider)stateProvider).setItems(items);
    }

	@Test
	public void testCorrectStatesNoHandlers() {
        StateHolder.StateHolderBuilder<TestConfig.Item, TestConfig.CustomState> builder = new StateHolder.StateHolderBuilder<>();
        StateHolder<TestConfig.Item, TestConfig.CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<TestConfig.CustomState>allOf(TestConfig.CustomState.class))
                .defineTransitions()
                .from(TestConfig.CustomState.START)
                .to(TestConfig.CustomState.STATE1)
                .build();

        stateListener.setStateHolder(stateHolder);
        publisher.publishEvent(new TestStateChangedEvent(this, "1", TestConfig.CustomState.STATE1));
        verify(onTransition, times(1)).moveToState(TestConfig.CustomState.STATE1, Arrays.asList(new TestConfig.Item("1")));
	}

    @Test
    public void testCorrectStatesWithHandlersInOrder() {
        StateHolder.StateHolderBuilder<TestConfig.Item, TestConfig.CustomState> builder = new StateHolder.StateHolderBuilder<>();
        StateHolder<TestConfig.Item, TestConfig.CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<TestConfig.CustomState>allOf(TestConfig.CustomState.class))
                .defineTransitions()
                .from(TestConfig.CustomState.START)
                .to(TestConfig.CustomState.STATE1)
                .before(beforeTransition1, beforeTransition2)
                .after(afterTransition1, afterTransition2)
                .build();

        stateListener.setStateHolder(stateHolder);
        publisher.publishEvent(new TestStateChangedEvent(this, "1", TestConfig.CustomState.STATE1));

        List<TestConfig.Item> items = Arrays.asList(new TestConfig.Item("1"));

        InOrder inOrder = inOrder(beforeTransition1, beforeTransition2, onTransition, afterTransition1, afterTransition2);
        inOrder.verify(beforeTransition1, times(1)).beforeTransition(items);
        inOrder.verify(beforeTransition2, times(1)).beforeTransition(items);
        inOrder.verify(onTransition, times(1)).moveToState(TestConfig.CustomState.STATE1, items);
        inOrder.verify(afterTransition1, times(1)).afterTransition(items);
        inOrder.verify(afterTransition2, times(1)).afterTransition(items);
    }

    @Test
    public void testConflictingEventsLeadToOnlyOneStateChange() {
        StateHolder.StateHolderBuilder<TestConfig.Item, TestConfig.CustomState> builder = new StateHolder.StateHolderBuilder<>();
        StateHolder<TestConfig.Item, TestConfig.CustomState> stateHolder = builder
                .setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<TestConfig.CustomState>allOf(TestConfig.CustomState.class))
                .defineTransitions()
                .from(TestConfig.CustomState.START)
                .to(TestConfig.CustomState.STATE1)
                .and()
                .from(TestConfig.CustomState.START)
                .to(TestConfig.CustomState.STATE2)
                .build();

        stateListener.setStateHolder(stateHolder);

        publisher.publishEvent(new TestStateChangedEvent(this, "1", TestConfig.CustomState.STATE1));
        verify(onTransition, times(1)).moveToState(TestConfig.CustomState.STATE1, Arrays.asList(new TestConfig.Item("1")));
    }

}

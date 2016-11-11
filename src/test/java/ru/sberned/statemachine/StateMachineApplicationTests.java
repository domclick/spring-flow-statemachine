package ru.sberned.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import ru.sberned.statemachine.state.OnTransition;
import ru.sberned.statemachine.state.StateChangedEvent;
import ru.sberned.statemachine.state.StateProvider;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = TestConfig.class
)
public class StateMachineApplicationTests {
	@Autowired
	private StateListener<TestConfig.Item, TestConfig.CustomState> stateListener;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private StateProvider<TestConfig.Item, TestConfig.CustomState> stateProvider;

    private OnTransition<TestConfig.Item, TestConfig.CustomState> onTransition = (state, items) -> {
        // do nothing
    };

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
	public void testCorrectStates() {
        StateHolder.StateHolderBuilder<TestConfig.Item, TestConfig.CustomState> builder = new StateHolder.StateHolderBuilder<>();
        StateHolder<TestConfig.Item, TestConfig.CustomState> stateHolder = builder.setStateChanger(onTransition)
                .setAvailableStates(EnumSet.<TestConfig.CustomState>allOf(TestConfig.CustomState.class)).defineTransitions()
                .from(TestConfig.CustomState.START).to(TestConfig.CustomState.STATE1).build();


        stateListener.setStateHolder(stateHolder);
        publisher.publishEvent(new StateChangedEvent<TestConfig.CustomState>(this, "1", TestConfig.CustomState.STATE1));
	}



}

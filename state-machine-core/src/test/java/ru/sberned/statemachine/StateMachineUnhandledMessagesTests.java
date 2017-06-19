package ru.sberned.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.state.StateChangedEvent;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.CustomStateProvider;
import ru.sberned.statemachine.util.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.Mockito.*;
import static ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType.EXECUTION_EXCEPTION;
import static ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType.INVALID_TRANSITION;
import static ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType.TIMEOUT;

/**
 * Created by jpatuk on 17/06/2017.
 */
@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = UnhandledMessagesConfig.class
)
public class StateMachineUnhandledMessagesTests {
    @Autowired
    private StateMachine<Item, CustomState, String> stateMachine;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private CustomStateProvider stateProvider;
    @SpyBean
    private StateChanger<Item, CustomState> onTransition;
    private UnhandledMessageProcessor<String> processor = mock(UnhandledMessageProcessor.class);

    private StateRepository<Item, CustomState, String> getDefaultTransition(UnhandledMessageProcessor<String> unhandled) {
        return StateRepository.StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .setUnhandledMessageProcessor(unhandled)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .build();
    }

    @Before
    public void before() {
        stateProvider.setItems(Arrays.asList(new Item("1", CustomState.START)));
    }

    @Test
    public void testUnhandledMessageProcessorTimeout() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = StateRepository.StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .setUnhandledMessageProcessor(processor)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.STATE1)
                .and()
                .from(CustomState.STATE1)
                .to(CustomState.STATE2)
                .build();

        stateMachine.setStateRepository(stateHolder);

        publisher.publishEvent(new StateChangedEvent("1", CustomState.STATE1));
        Thread.sleep(100);
        publisher.publishEvent(new StateChangedEvent("1", CustomState.STATE2));

        verify(processor, timeout(3000).times(1)).process("1", TIMEOUT, null);
    }

    @Test
    public void testUnhandledMessageProcessorInvalidState() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = getDefaultTransition(processor);

        stateMachine.setStateRepository(stateHolder);
        stateMachine.handleStateChanged(new StateChangedEvent("1", CustomState.STATE2));

        verify(processor, timeout(1500).times(1)).process("1", INVALID_TRANSITION, null);
    }

    @Test
    public void testUnhandledMessageProcessorExecutionException() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = getDefaultTransition(processor);
        stateMachine.setStateRepository(stateHolder);

        RuntimeException ex = new RuntimeException();
        doThrow(ex).when(onTransition).moveToState(CustomState.STATE1, new Item("1", CustomState.START));

        publisher.publishEvent(new StateChangedEvent("1", CustomState.STATE1));

        verify(processor, timeout(1500).times(1)).process("1", EXECUTION_EXCEPTION, ex);
    }

    @Test
    public void testStateNotPresentInStateHolder() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = StateRepository.StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.of(CustomState.START, CustomState.FINISH))
                .setUnhandledMessageProcessor(processor)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.FINISH)
                .build();
        stateMachine.setStateRepository(stateHolder);

        stateMachine.setStateRepository(stateHolder);
        publisher.publishEvent(new StateChangedEvent("1", CustomState.STATE2));

        verify(processor, timeout(1500).times(1)).process("1", INVALID_TRANSITION, null);
    }
}

package ru.sberned.statemachine;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;
import ru.sberned.statemachine.processor.UnableToProcessException;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.state.StateChangedEvent;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.CustomStateProvider;
import ru.sberned.statemachine.util.Item;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static org.mockito.Mockito.*;
import static ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType.*;
import static ru.sberned.statemachine.util.CustomState.*;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 17/06/2017.
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
    private UnhandledMessageProcessor<String, CustomState> processor = mock(UnhandledMessageProcessor.class);

    private StateRepository<Item, CustomState, String> getDefaultTransition(UnhandledMessageProcessor<String, CustomState> unhandled) {
        return StateRepository.StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .setUnhandledMessageProcessor(unhandled)
                .defineTransitions()
                .from(CustomState.START)
                .to(STATE1)
                .build();
    }

    @Before
    public void before() {
        stateProvider.setItems(Collections.singletonList(new Item("1", CustomState.START)));
    }

    @Test
    public void testUnhandledMessageProcessorTimeout() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = StateRepository.StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .setUnhandledMessageProcessor(processor)
                .defineTransitions()
                .from(CustomState.START)
                .to(STATE1)
                .and()
                .from(STATE1)
                .to(STATE2)
                .build();

        stateMachine.setStateRepository(stateHolder);

        publisher.publishEvent(new StateChangedEvent("1", STATE1));
        Thread.sleep(100);
        publisher.publishEvent(new StateChangedEvent("1", STATE2));

        verify(processor, timeout(3000).times(1)).process("1", STATE2, TIMEOUT, null);
    }

    @Test
    public void testInterruptedException() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = StateRepository.StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.allOf(CustomState.class))
                .setUnhandledMessageProcessor(processor)
                .defineTransitions()
                .from(CustomState.START)
                .to(STATE1)
                .build();
        StateMachine<Item, CustomState, String> stateMachine = new StateMachine<>(stateProvider, (state, item, infos) -> {
        }, key -> new ErroringLockProvider());
        stateMachine.setStateRepository(stateHolder);
        Map dummy = stateMachine.changeState(Collections.singletonList("1"), STATE1, "dummy");

        verify(processor, timeout(2000).times(1)).process(eq("1"), eq(STATE1), eq(INTERRUPTED_EXCEPTION), any(InterruptedException.class));
    }

    @Test
    public void testUnhandledMessageProcessorInvalidState() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = getDefaultTransition(processor);

        stateMachine.setStateRepository(stateHolder);
        stateMachine.handleStateChanged(new StateChangedEvent("1", STATE2));

        verify(processor, timeout(1500).times(1)).process("1", STATE2, INVALID_TRANSITION, null);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailIfCalledDirectlyWithNullEvent() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = getDefaultTransition(processor);

        stateMachine.setStateRepository(stateHolder);
        stateMachine.handleStateChanged(null);
    }

    @Test
    public void testUnhandledMessageProcessorExecutionException() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = getDefaultTransition(processor);
        stateMachine.setStateRepository(stateHolder);

        RuntimeException ex = new RuntimeException();
        doThrow(ex).when(onTransition).moveToState(STATE1, new Item("1", CustomState.START));

        publisher.publishEvent(new StateChangedEvent("1", STATE1));

        verify(processor, timeout(1500).times(1)).process("1", STATE1, EXECUTION_EXCEPTION, ex);
    }

    @Test
    public void shouldFailIfBeforeFails() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = StateRepository.StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.of(CustomState.START, CustomState.FINISH))
                .setUnhandledMessageProcessor(processor)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.FINISH)
                .before(item -> false)
                .build();
        stateMachine.setStateRepository(stateHolder);

        publisher.publishEvent(new StateChangedEvent("1", FINISH));

        verify(processor, timeout(1500).times(1)).process(eq("1"), eq(FINISH), eq(EXECUTION_EXCEPTION), any(UnableToProcessException.class));
    }

    @Test
    public void shouldFailIfBeforeAnyFails() throws InterruptedException {
        StateRepository<Item, CustomState, String> stateHolder = StateRepository.StateRepositoryBuilder.<Item, CustomState, String>configure()
                .setAvailableStates(EnumSet.of(CustomState.START, CustomState.FINISH))
                .setUnhandledMessageProcessor(processor)
                .setAnyBefore((item, customState) -> false)
                .defineTransitions()
                .from(CustomState.START)
                .to(CustomState.FINISH)
                .build();
        stateMachine.setStateRepository(stateHolder);

        publisher.publishEvent(new StateChangedEvent("1", FINISH));

        verify(processor, timeout(1500).times(1)).process(eq("1"), eq(FINISH), eq(EXECUTION_EXCEPTION), any(UnableToProcessException.class));
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
        publisher.publishEvent(new StateChangedEvent("1", STATE2));

        verify(processor, timeout(1500).times(1)).process("1", STATE2, INVALID_TRANSITION, null);
    }

    private static class ErroringLockProvider implements Lock {
        @Override
        public void lock() {
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            throw new InterruptedException();
        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException();
        }

        @Override
        public void unlock() {

        }

        @Override
        public Condition newCondition() {
            return null;
        }
    }
}

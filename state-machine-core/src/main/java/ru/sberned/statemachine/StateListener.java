package ru.sberned.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType;
import ru.sberned.statemachine.state.HasId;
import ru.sberned.statemachine.state.StateChangedEvent;
import ru.sberned.statemachine.state.StateProvider;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType.*;

/**
 * Created by jpatuk on 09/11/2016.
 */
public class StateListener<ENTITY extends HasId<KEY>, STATE extends Enum<STATE>, KEY> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateListener.class);
    @Autowired
    private StateProvider<ENTITY, STATE, KEY> stateProvider;
    @Autowired
    private LockProvider lockProvider;
    @Autowired
    private StateMachine<ENTITY, STATE> stateMachine;
    private StateRepository<ENTITY, STATE> stateRepository;
    @Value("${statemachine.lock.timeout.ms:5000}")
    private long lockTimeout;

    public void setStateRepository(StateRepository<ENTITY, STATE> stateRepository) {
        this.stateRepository = stateRepository;
    }

    @EventListener
    public synchronized void handleStateChanged(StateChangedEvent<STATE, KEY> event) {
        Assert.notNull(stateRepository);
        Map<ENTITY, STATE> sourceMap = stateProvider.getItemsState(event.getIds());

        sourceMap.forEach((key, value) -> CompletableFuture.supplyAsync(() -> {
            handleMessage(key, value, event.getNewState());
            return null;
        }));
    }

    void handleMessage(ENTITY entity, STATE currentState, STATE newState) {
        Lock lockObject = lockProvider.getLockObject(entity.getId());
        boolean locked = false;
        try {
            if (locked = lockObject.tryLock(lockTimeout, TimeUnit.MILLISECONDS)) {
                if (stateRepository.isValidTransition(currentState, newState)) {
                    stateMachine.processItem(stateRepository, entity, currentState, newState);
                } else {
                    handleIncorrectCase(entity, currentState, INVALID_TRANSITION, null);
                }
            } else {
                handleIncorrectCase(entity, currentState, TIMEOUT, null);
            }
        } catch (InterruptedException e) {
            handleIncorrectCase(entity, currentState, INTERRUPTED_EXCEPTION, e);
        } catch (Exception e) {
            handleIncorrectCase(entity, currentState, EXECUTION_EXCEPTION, e);
        } finally {
            if (locked) lockObject.unlock();
        }
    }

    private void handleIncorrectCase(ENTITY entity, STATE currentState, IssueType issueType, Exception e) {
        String errorMsg = MessageFormat.format("Processing for item {0} failed. State is {1}. Issue type is {2}", entity, currentState, issueType);

        if (e != null) LOGGER.error(errorMsg, e);
        else LOGGER.error(errorMsg);

        UnhandledMessageProcessor<ENTITY> unhandledMessageProcessor = stateRepository.getUnhandledMessageProcessor();
        if (unhandledMessageProcessor != null) {
            unhandledMessageProcessor.process(entity, issueType, e);
        }
    }
}

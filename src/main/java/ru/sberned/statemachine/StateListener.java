package ru.sberned.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.sberned.statemachine.lock.StateLockProvider;
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
 * Created by empatuk on 09/11/2016.
 */
@Component
public abstract class StateListener<ENTITY extends HasId<KEY>, STATE extends Enum<STATE>, KEY> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateListener.class);
    @Autowired
    private StateProvider<ENTITY, STATE, KEY> stateProvider;
    @Autowired
    private StateLockProvider<KEY> stateLock;
    private StateHolder<ENTITY, STATE> stateHolder;
    @Value("${statemachine.lock.timeout.ms:5000}")
    private long lockTimeout;

    public void setStateHolder(StateHolder<ENTITY, STATE> stateHolder) {
        this.stateHolder = stateHolder;
    }

    @EventListener
    public synchronized void handleStateChanged(StateChangedEvent<STATE, KEY> event) {
        Assert.notNull(stateHolder);
        Map<ENTITY, STATE> sourceMap = stateProvider.getItemsState(event.getIds());

        sourceMap.entrySet().forEach((entry) -> {
            CompletableFuture.supplyAsync(() -> {
                handleMessage(entry.getKey(), entry.getValue(), event.getNewState());
                return null;
            });

        });
    }

    void handleMessage(ENTITY entity, STATE currentState, STATE newState) {
        Lock lockObject = stateLock.getLockObject(entity.getId());
        boolean locked = false;
        try {
            if (locked = lockObject.tryLock(lockTimeout, TimeUnit.MILLISECONDS)) {
                if (stateHolder.isValidTransition(currentState, newState)) {
                    processItems(entity, currentState, newState);
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

        UnhandledMessageProcessor<ENTITY> unhandledMessageProcessor = stateHolder.getUnhandledMessageProcessor();
        if (unhandledMessageProcessor != null) {
            unhandledMessageProcessor.process(entity, issueType, e);
        }
    }

    @Transactional
    private void processItems(ENTITY item, STATE from, STATE to) {
        stateHolder.getBeforeAll().forEach(handler -> handler.beforeTransition(item));
        stateHolder.getBefore(from, to).forEach(handler -> handler.beforeTransition(item));
        stateHolder.getTransition().moveToState(to, item);
        stateHolder.getAfter(from, to).forEach(handler -> handler.afterTransition(item));
        stateHolder.getAfterAll().forEach(handler -> handler.afterTransition(item));
    }
}

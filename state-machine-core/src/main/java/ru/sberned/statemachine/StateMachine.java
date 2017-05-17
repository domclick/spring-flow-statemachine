package ru.sberned.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.processor.UnableToProcessException;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor;
import ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType;
import ru.sberned.statemachine.state.HasStateAndId;
import ru.sberned.statemachine.state.StateChangedEvent;
import ru.sberned.statemachine.state.ItemWithStateProvider;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType.*;

/**
 * Created by jpatuk on 09/11/2016.
 */
public class StateMachine<ENTITY extends HasStateAndId<ID, STATE>, STATE extends Enum<STATE>, ID> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);
    @Autowired
    private ItemWithStateProvider<ENTITY, ID> stateProvider;
    @Autowired
    private LockProvider lockProvider;
    @Autowired
    private StateMachine<ENTITY, STATE, ID> stateMachine;
    private StateRepository<ENTITY, STATE> stateRepository;
    @Value("${statemachine.lock.timeout.ms:5000}")
    private long lockTimeout;

    public void setStateRepository(StateRepository<ENTITY, STATE> stateRepository) {
        this.stateRepository = stateRepository;
    }

    @EventListener
    public synchronized void handleStateChanged(StateChangedEvent<STATE, ID> event) {
        Assert.notNull(stateRepository);
        Collection<ENTITY> items = stateProvider.getItemsByIds(event.getIds());

        items.forEach(item -> CompletableFuture.supplyAsync(() -> {
            handleMessage(item, event.getNewState());
            return null;
        }));
    }

    void handleMessage(ENTITY entity, STATE newState) {
        STATE currentState = entity.getState();
        Lock lockObject = lockProvider.getLockObject(entity.getId());
        boolean locked = false;
        try {
            if (locked = lockObject.tryLock(lockTimeout, TimeUnit.MILLISECONDS)) {
                if (stateRepository.isValidTransition(currentState, newState)) {
                    stateMachine.processItem(entity, currentState, newState);
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

    // public is here to make @Transactional work
    @Transactional
    public void processItem(ENTITY item, STATE from, STATE to) {
        stateRepository.getBeforeAll().forEach(handler -> {
            if (!handler.beforeTransition(item)) {
                throw new UnableToProcessException();
            }
        });
        stateRepository.getBefore(from, to).forEach(handler -> {
            if (!handler.beforeTransition(item)) {
                throw new UnableToProcessException();
            }
        });
        stateRepository.getStateChanger().moveToState(to, item);
        stateRepository.getAfter(from, to).forEach(handler -> handler.afterTransition(item));
        stateRepository.getAfterAll().forEach(handler -> handler.afterTransition(item));
    }
}

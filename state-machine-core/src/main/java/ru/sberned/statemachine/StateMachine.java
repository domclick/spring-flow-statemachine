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
import ru.sberned.statemachine.state.StateChanger;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static ru.sberned.statemachine.processor.UnhandledMessageProcessor.IssueType.*;

/**
 * Created by jpatuk on 09/11/2016.
 */
public class StateMachine<ENTITY extends HasStateAndId<ID, STATE>, STATE extends Enum<STATE>, ID> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateMachine.class);
    private final ItemWithStateProvider<ENTITY, ID> stateProvider;
    private final StateChanger<ENTITY, STATE> stateChanger;
    private final LockProvider lockProvider;
    // To make Transactional work
    @Autowired
    private StateMachine<ENTITY, STATE, ID> stateMachine;
    private StateRepository<ENTITY, STATE, ID> stateRepository;
    @Value("${statemachine.lock.timeout.ms:5000}")
    private long lockTimeout;

    @Autowired
    public StateMachine(ItemWithStateProvider<ENTITY, ID> stateProvider,
                        StateChanger<ENTITY, STATE> stateChanger,
                        LockProvider lockProvider) {
        this.stateProvider = stateProvider;
        this.stateChanger = stateChanger;
        this.lockProvider = lockProvider;
    }

    public void setStateRepository(StateRepository<ENTITY, STATE, ID> stateRepository) {
        this.stateRepository = stateRepository;
    }

    @EventListener
    public synchronized void handleStateChanged(StateChangedEvent<STATE, ID> event) {
        Assert.notNull(stateRepository, "StateRepository must be initialized!");

        if (event.getIds() != null) {
            event.getIds().forEach(id -> CompletableFuture.supplyAsync(() -> {
                try {
                    stateMachine.handleMessage(id, event.getNewState(), event.getInfo());
                } catch (InterruptedException e) {
                    handleIncorrectCase(id, event.getNewState(), INTERRUPTED_EXCEPTION, e);
                } catch (Exception e) {
                    handleIncorrectCase(id, event.getNewState(), EXECUTION_EXCEPTION, e);
                }
                return null;
            }));
        }
    }

    // public is here to make @Transactional work
    @Transactional(rollbackFor = {Exception.class})
    public void handleMessage(ID id, STATE newState, Object info) throws Exception {
        Lock lockObject = lockProvider.getLockObject(id);
        boolean locked = false;
        try {
            if (locked = lockObject.tryLock(lockTimeout, TimeUnit.MILLISECONDS)) {
                ENTITY entity = stateProvider.getItemById(id);
                if (entity == null) {
                    handleIncorrectCase(null, null, ENTITY_NOT_FOUND, null);
                    return;
                }

                STATE currentState = entity.getState();
                if (stateRepository.isValidTransition(currentState, newState)) {
                    processItem(entity, currentState, newState, info);
                } else {
                    handleIncorrectCase(id, currentState, INVALID_TRANSITION, null);
                }
            } else {
                handleIncorrectCase(id, newState, TIMEOUT, null);
            }
        } finally {
            if (locked) lockObject.unlock();
        }
    }

    private void handleIncorrectCase(ID id, STATE newState, IssueType issueType, Exception e) {
        String errorMsg = MessageFormat.format("Processing for item with id {0} failed. New state is {1}. Issue type is {2}", id, newState, issueType);

        if (e != null) LOGGER.error(errorMsg, e);
        else LOGGER.error(errorMsg);

        UnhandledMessageProcessor<ID> unhandledMessageProcessor = stateRepository.getUnhandledMessageProcessor();
        if (unhandledMessageProcessor != null) {
            unhandledMessageProcessor.process(id, issueType, e);
        }
    }

    private void processItem(ENTITY item, STATE from, STATE to, Object info) {
        stateRepository.getBeforeAll().forEach(handler -> {
            if (!handler.beforeTransition(item, to)) {
                throw new UnableToProcessException();
            }
        });
        stateRepository.getBefore(from, to).forEach(handler -> {
            if (!handler.beforeTransition(item)) {
                throw new UnableToProcessException();
            }
        });

        if (info != null) {
            stateChanger.moveToState(to, item, info);
        } else {
            stateChanger.moveToState(to, item);
        }

        stateRepository.getAfter(from, to).forEach(handler -> handler.afterTransition(item));
        stateRepository.getAfterAll().forEach(handler -> handler.afterTransition(item, to));
    }
}

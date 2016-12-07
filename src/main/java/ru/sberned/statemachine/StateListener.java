package ru.sberned.statemachine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.sberned.statemachine.lock.StateLock;
import ru.sberned.statemachine.state.GetId;
import ru.sberned.statemachine.state.StateChangedEvent;
import ru.sberned.statemachine.state.StateProvider;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Created by empatuk on 09/11/2016.
 */
@Component
public abstract class StateListener<T extends GetId<K>, E extends Enum<E>, K> {
    @Autowired
    private StateProvider<T, E, K> stateProvider;
    @Autowired
    private StateLock<K> stateLock;
    private StateHolder<T, E> stateHolder;
    @Value("${statemachine.lock.timeout.ms}")
    private long lockTimeout = 5000;

    public void setStateHolder(StateHolder<T, E> stateHolder) {
        this.stateHolder = stateHolder;
    }

    @Async
    @EventListener
    public synchronized void handleStateChanged(StateChangedEvent<E, K> event) {
        Assert.notNull(stateHolder);
        Map<T, E> sourceMap = stateProvider.getItemsState(event.getIds());

        sourceMap.entrySet().parallelStream().forEach((entry) -> {
            Lock lockObject = stateLock.getLockObject(entry.getKey().getId());
            try {
                lockObject.tryLock(lockTimeout, TimeUnit.MILLISECONDS);
                if (stateHolder.isValidTransition(entry.getValue(), event.getNewState())) {
                    processItems(entry.getKey(), entry.getValue(), event.getNewState());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lockObject.unlock();
            }
        });
    }

    @Transactional
    private void processItems(T item, E from, E to) {
        stateHolder.getAnyBefore().forEach(handler -> handler.beforeTransition(item));
        stateHolder.getBefore(from, to).forEach(handler -> handler.beforeTransition(item));
        stateHolder.getTransition().moveToState(to, item);
        stateHolder.getAfter(from, to).forEach(handler -> handler.afterTransition(item));
        stateHolder.getAnyAfter().forEach(handler -> handler.afterTransition(item));
    }
}

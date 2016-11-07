package ru.sberned.statemachine.state;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by empatuk on 01/11/2016.
 */
@Component
public abstract class StateHolder<T, E extends Enum<E>&GetEnumValue> implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    private Map<String, BeanWithStates> stateBeans = new HashMap<>();
    private StateProvider<T, E> stateProvider;
    private Class<E> enumEx;
    private List<String> names;

    protected StateHolder(StateProvider<T, E> stateProvider, Class<E> enumEx, EnumSet<E> values) {
        this.stateProvider = stateProvider;
        this.enumEx = enumEx;
        this.names = values.stream().map(value -> value.getValue()).collect(Collectors.toList());
    }

    private static class BeanWithStates {
        private Object bean;
        private List<String> statesFrom;

        private BeanWithStates(Object bean, List<String> statesFrom) {
            this.bean = bean;
            this.statesFrom = statesFrom;
        }

        Object getBean() {
            return bean;
        }

        List<String> getStatesFrom() {
            return statesFrom;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void onContextInitialized(final ContextRefreshedEvent event) {
        System.out.println("context initialized, starting state gathering");
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ChangeState.class);

        beans.entrySet().stream().filter((e) -> valuesMatch(e.getValue())).forEach((e) -> {
            Object bean = e.getValue();
            String enumValue = bean.getClass().getAnnotation(ChangeState.class).to();
            stateBeans.put(enumValue, new BeanWithStates(bean, Arrays.asList(bean.getClass().getAnnotation(ChangeState.class).from())));
        });
    }

    private boolean valuesMatch(Object bean) {
        return enumEx.equals(bean.getClass().getAnnotation(ChangeState.class).enumClass())
                && checkValues(bean.getClass().getAnnotation(ChangeState.class));
    }

    private boolean checkValues(ChangeState annotation) {
        String[] fromStates = annotation.from();
        for (String fromState : fromStates) {
            if (!names.contains(fromState)) {
                return false;
            }
        }
        return names.contains(annotation.to());
    }

    @Async
    @EventListener
    public void handleStateChanged(StateChangedEvent event) {
        BeanWithStates beanWithStates = stateBeans.get(((GetEnumValue)event.getNewState()).getValue());
        if (beanWithStates != null) {
            Map<T, E> states = stateProvider.getItemsState(event.getIds());
            if (states != null) {
                List<T> items = states.entrySet().stream()
                        .filter(e -> beanWithStates.getStatesFrom().isEmpty() || beanWithStates.getStatesFrom().contains(e.getValue().getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                processItems(beanWithStates.getBean(), (E) event.getNewState(), items);
            }
        }
    }

    private void processItems(Object bean, E newState, List<T> items) {
        boolean runProcessing = false;
        if (bean instanceof OnTransition) {
            runProcessing= true;
        }
        if (runProcessing) ((OnTransition<T>)bean).beforeTransition(items);

        stateProvider.updateState(newState, items);

        if (runProcessing) ((OnTransition<T>)bean).afterTransition(items);
    }
}

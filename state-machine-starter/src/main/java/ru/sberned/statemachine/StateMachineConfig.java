package ru.sberned.statemachine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.lock.MapLockProvider;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 07/12/2016.
 */
@Configuration
public class StateMachineConfig {

    @Bean
    @ConditionalOnMissingBean(LockProvider.class)
    public LockProvider lockProvider() {
        return new MapLockProvider();
    }

}

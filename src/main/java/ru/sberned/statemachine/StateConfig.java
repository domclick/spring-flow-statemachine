package ru.sberned.statemachine;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.sberned.statemachine.lock.MapStateLock;
import ru.sberned.statemachine.lock.StateLock;

/**
 * Created by empatuk on 07/12/2016.
 */
@EnableAutoConfiguration
@Configuration
public class StateConfig {

    @Bean
    @ConditionalOnMissingBean
    public StateLock<?> stateLock() {
        return new MapStateLock();
    }
}

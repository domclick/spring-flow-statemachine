package ru.sberned.statemachine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import ru.sberned.statemachine.util.CustomState;
import ru.sberned.statemachine.util.DBStateProvider;
import ru.sberned.statemachine.util.Item;
import ru.sberned.statemachine.lock.LockProvider;
import ru.sberned.statemachine.lock.MapLockProvider;
import ru.sberned.statemachine.state.StateProvider;

import javax.sql.DataSource;

/**
 * Created by jpatuk on 25/04/2017.
 */
@Configuration
public class ITConfig {
    @Bean
    public StateProvider<Item, CustomState, String> stateProvider() {
        return new DBStateProvider();
    }

    @Bean
    public LockProvider stateLock() {
        return new MapLockProvider();
    }

    @Bean
    public StateListener<Item, CustomState, String> stateListener() {
        return new StateListener<>();
    }

    @Bean
    public StateMachine<Item, CustomState> stateMachine() {
        return new StateMachine<>();
    }

    @Bean
    public DataSource dataSource() {
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        return builder
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }
}

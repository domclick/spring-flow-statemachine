package ru.sberned.statemachine.samples.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import ru.sberned.statemachine.StateListener;
import ru.sberned.statemachine.StateRepository;
import ru.sberned.statemachine.StateRepository.StateRepositoryBuilder;
import ru.sberned.statemachine.samples.simple.store.ItemStore;
import ru.sberned.statemachine.state.*;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Scanner;

import static ru.sberned.statemachine.samples.simple.SimpleState.*;

/**
 * Created by jpatuk on 25/04/2017.
 */
@SuppressWarnings("unchecked")
@SpringBootApplication
public class Simple implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateListener.class);
    @Autowired
    private StateListener<SimpleItem, SimpleState, String> stateListener;
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private StateChanger<SimpleItem, SimpleState> stateChanger;
    @Autowired
    private ItemStore store;

    @PostConstruct
    public void configure() {
        StateRepositoryBuilder<SimpleItem, SimpleState> builder = new StateRepositoryBuilder<>();
        StateRepository<SimpleItem, SimpleState> repository = builder
                .setStateChanger(stateChanger)
                .setAvailableStates(EnumSet.allOf(SimpleState.class))
                .setUnhandledMessageProcessor((item, type, ex) -> LOGGER.error("Got unhandled item with id {}, issue is {}", item.getId(), type))
                .setAnyBefore((BeforeTransition<SimpleItem>) item -> {
                    LOGGER.info("Started working on item with id {}", item.getId());
                    return true;
                })
                .defineTransitions()
                .from(STARTED)
                .to(IN_PROGRESS)
                .after((AfterTransition<SimpleItem>) item -> LOGGER.info("Moved from STARTED to IN_PROGRESS"))
                .and()
                .from(IN_PROGRESS)
                .to(FINISHED)
                .after((AfterTransition<SimpleItem>) item -> LOGGER.info("Moved from IN_PROGRESS to FINISHED"))
                .and()
                .from(STARTED, IN_PROGRESS)
                .to(CANCELLED)
                .after((AfterTransition<SimpleItem>) item -> LOGGER.info("Moved from CANCELED"))
                .build();

        stateListener.setStateRepository(repository);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Simple.class, args);
    }

    public void run(String... strings) throws Exception {
        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\r?\n|\r");

        System.out.println("Type CREATE <itemId> in order to create a new item." +
                "Type MOVE <itemId> <state> in order to move item to a new state." +
                "Type EXIT to terminate.");

        while (true) {
            String command = scanner.next();
            parseCommand(command);
        }
    }

    private void parseCommand(String command) {
        String[] predicates = command.split("\\s+");

        switch (predicates[0]) {
            case "CREATE":
                if (predicates.length != 2) {
                    System.out.println("Incorrect syntax. Type CREATE <itemId> in order to create a new item.");
                    break;
                }
                if (store.itemExists(predicates[1])) {
                    System.out.println("Such item already exists");
                    break;
                }
                store.createNewItem(predicates[1]);
                break;
            case "MOVE":
                if (predicates.length != 3) {
                    System.out.println("Incorrect syntax. Type MOVE <itemId> <state> in order to move item to a new state.");
                    break;
                }
                if (!store.itemExists(predicates[1])) {
                    System.out.println("Such item doesn't exist");
                    break;
                }
                SimpleState state = SimpleState.getByName(predicates[2]);
                publisher.publishEvent(new StateChangedEvent<>(this, Collections.singletonList(store.getItem(predicates[1])), state));
                break;
            case "EXIT":
                System.exit(0);
                break;
            default:
                System.out.println("Unknown command submitted.");
        }
    }
}

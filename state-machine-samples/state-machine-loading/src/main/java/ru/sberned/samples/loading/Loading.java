package ru.sberned.samples.loading;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import ru.sberned.samples.loading.model.states.FirstState;
import ru.sberned.samples.loading.model.states.IAmLoadableState;
import ru.sberned.samples.loading.model.states.SecondState;
import ru.sberned.samples.loading.model.states.ThirdState;
import ru.sberned.samples.loading.service.StateInfoService;
import ru.sberned.samples.loading.store.ItemStore;
import ru.sberned.statemachine.state.StateChangedEvent;

import java.util.Scanner;


/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 25/04/2017.
 */
@SpringBootApplication
public class Loading implements CommandLineRunner {
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private ItemStore store;
    @Autowired
    private StateInfoService stateInfoService;
    @Autowired
    private LoadingConfig loadingConfig;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Loading.class, args);
    }

    public void run(String... strings) throws Exception {
        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\r?\n|\r");

        System.out.println("Type CREATE <itemId> in order to create a new item.\n" +
                "Type MOVE <itemId> <state> in order to move item to a new state.\n" +
                "Type BLOCK <state> in order to remove state from state machine.\n" +
                "Available <state> values are: first, second third\n" +
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
            case "BLOCK":
                if (predicates.length != 2) {
                    System.out.println("Incorrect syntax. Type BLOCK <state> in order to create a new item.");
                    break;
                }
                stateInfoService.removeState(simpleStateByName(predicates[1]));
                loadingConfig.reinitStateMachine();
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
                IAmLoadableState simpleState = simpleStateByName(predicates[2]);
                publisher.publishEvent(new StateChangedEvent<>(predicates[1], simpleState));
                break;
            case "EXIT":
                System.exit(0);
                break;
            default:
                System.out.println("Unknown command submitted.");
        }
    }

    private IAmLoadableState simpleStateByName(String predicate) {
        if ("first".equals(predicate)) return new FirstState();
        if ("second".equals(predicate)) return new SecondState();
        if ("third".equals(predicate)) return new ThirdState();
        return null;
    }

}

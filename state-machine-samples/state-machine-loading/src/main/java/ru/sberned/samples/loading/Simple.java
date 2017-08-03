package ru.sberned.samples.loading;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import ru.sberned.samples.loading.model.states.FirstState;
import ru.sberned.samples.loading.model.states.IAmSimpleState;
import ru.sberned.samples.loading.model.states.SecondState;
import ru.sberned.samples.loading.model.states.ThirdState;
import ru.sberned.samples.loading.store.ItemStore;
import ru.sberned.statemachine.state.StateChangedEvent;

import java.util.Scanner;


/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 25/04/2017.
 */
@SpringBootApplication
public class Simple implements CommandLineRunner {
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private ItemStore store;

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
                IAmSimpleState simpleState = simpleStateByName(predicates[2]);
                publisher.publishEvent(new StateChangedEvent<>(predicates[1], simpleState));
                break;
            case "EXIT":
                System.exit(0);
                break;
            default:
                System.out.println("Unknown command submitted.");
        }
    }

    private IAmSimpleState simpleStateByName(String predicate) {
        if ("first".equals(predicate)) return new FirstState();
        if ("second".equals(predicate)) return new SecondState();
        if ("third".equals(predicate)) return new ThirdState();
        return null;
    }

}

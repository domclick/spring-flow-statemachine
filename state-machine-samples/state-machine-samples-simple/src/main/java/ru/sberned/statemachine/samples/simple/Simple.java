package ru.sberned.statemachine.samples.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import ru.sberned.statemachine.samples.simple.store.ItemStore;
import ru.sberned.statemachine.state.*;

import java.util.Scanner;


/**
 * Created by jpatuk on 25/04/2017.
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
                SimpleState state = SimpleState.getByName(predicates[2]);
                publisher.publishEvent(new StateChangedEvent<>(predicates[1], state));
                break;
            case "EXIT":
                System.exit(0);
                break;
            default:
                System.out.println("Unknown command submitted.");
        }
    }
}

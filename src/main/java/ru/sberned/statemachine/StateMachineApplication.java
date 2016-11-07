package ru.sberned.statemachine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableAsync;
import ru.sberned.statemachine.impl.CustomState;
import ru.sberned.statemachine.state.StateChangedEvent;

import java.util.Arrays;

@SpringBootApplication
@EnableAsync
public class StateMachineApplication implements CommandLineRunner{
	@Autowired
	private ApplicationEventPublisher eventPublisher;

	public static void main(String[] args) {
		SpringApplication.run(StateMachineApplication.class, args);
	}

	@Override
	public void run(String... strings) throws Exception {
		System.out.println("123");
		eventPublisher.publishEvent(new StateChangedEvent<CustomState>(this, Arrays.asList("1"), CustomState.STATE2));
	}
}

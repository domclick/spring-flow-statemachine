package ru.sberned.statemachine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.Arrays;

@SpringBootApplication
@EnableAsync
public class StateMachineApplication {

	public static void main(String[] args) {
		SpringApplication.run(StateMachineApplication.class, args);
	}
}

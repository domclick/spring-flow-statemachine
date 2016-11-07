package ru.sberned.statemachine.state;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by empatuk on 31/10/2016.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeState {
    Class<? extends Enum<?>> enumClass();

    String[] from() default {};

    String to();
}

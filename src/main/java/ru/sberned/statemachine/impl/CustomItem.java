package ru.sberned.statemachine.impl;

/**
 * Created by empatuk on 03/11/2016.
 */
public class CustomItem {
    private String id;
    private int something;

    public CustomItem(String id, int something) {
        this.id = id;
        this.something = something;
    }

    public String getId() {
        return id;
    }

    public int getSomething() {
        return something;
    }
}

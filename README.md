# Spring Flow State Machine <a href="http://search.maven.org/#artifactdetails%7Cru.sberned.statemachine%7Cspring-flow-state-machine-starter%7C1.0.2%7Cjar" rel="some text">![(version 1.0.2)](https://maven-badges.herokuapp.com/maven-central/ru.sberned.statemachine/state-machine-core/badge.svg)</a> [![Build Status](https://travis-ci.org/Sberned/spring-flow-statemachine.svg?branch=master)](https://travis-ci.org/Sberned/spring-flow-statemachine)


## Goal

This project was born when we understood that [spring-statemachine](http://projects.spring.io/spring-statemachine/) doesn't meet our requirements.  Our requirements are very simple:

* It should be easy to use
* It should be easy to integrate
* It should manage not the state of our application, but the state of entities, which our application works with
* It should be easy to configure and extend it

Actually, spring-statemachine doesn't meet any of requirements.

Almost every system we work with has some entities which have its own lifecycle.

So we've created our own library, which allows you to achieve the following goals:

* Define your own states for any entity
* Define allowed transitions between states
* Define actions, which should be performed before and after specific transition
* Define actions, which should be performed before/after any transition

## Non-goals

It is a non-goal to replace spring-statemachine (and we didn't)
It is a non-goal to provide connectors to any available storage provider

## Components

1. `core` module provides all needed functionality for you to use spring-flow-statemachine
2. `starter` modules is Spring Boot Starter, which will configure as much as possible in your application for you.

## Installation

### Spring Boot

If you use Spring Boot then you can set up dependency on starter package

#### Maven 

```xml
<dependency>
    <groupId>ru.sberned.statemachine</groupId>
    <artifactId>spring-flow-state-machine-starter</artifactId>
    <version>1.0.2</version>
</dependency>
```

#### Gradle

```groovy
compile 'ru.sberned.statemachine:spring-flow-state-machine-starter:1.0.2'
```

### Spring without Boot

Otherwise you can set up dependency on state-machine-core artefact

## How To

### Configure

#### Step 1: Define state changer

`StateChanger` is interface, which implementations are responsible for moving entity from one state to another (i.e. it can just update one field in your relational DB)

#### Step 2: Define item provider

`ItemWithStateProvider` is interface which is responsible for finding object in store

#### Step 3: Define lock provider

It's always possible in distributed systems that several nodes of your system will want to change the state of your entity simultaneously. We're prepared for this case and ask you to provide us with `Lock` which will guarantee, that only one transition can happen at a time. If you work in single-node environment — you can just return simple in-memory, otherwise you'll need something distributed.

Interface, which should be implemented for this aim is `LockProvider`. By default we provide you with very simple lock implementation, `MapLockProvider`

#### Step 4: Create and configure state machine

```java
    @Bean
    public StateMachine<SimpleItem, SimpleState, String> stateMachine() {
        StateRepository<SimpleItem, SimpleState, String> repository = StateRepositoryBuilder.<SimpleItem, SimpleState, String>configure() // ①
                .setAvailableStates(EnumSet.allOf(SimpleState.class)) // ②
                .setUnhandledMessageProcessor((item, state, type, ex) -> LOGGER.error("Got unhandled item with id {}, issue is {}", item, type)) // ③
                .setAnyBefore((BeforeAnyTransition<SimpleItem, SimpleState>) (item, state) -> {
                    LOGGER.info("Started working on item with id {}", item.getId());
                    return true;
                }) // ④
                .defineTransitions()
                .from(STARTED) // ⑤
                .to(IN_PROGRESS) // ⑥
                .after((AfterTransition<SimpleItem>) item -> LOGGER.info("Moved from STARTED to IN_PROGRESS")) 
                .and() // ⑦
                // omitted for brevity
                .build(); 

        StateMachine<SimpleItem, SimpleState, String> stateMachine = new StateMachine<>(stateProvider(), stateChanger(), lockProvider); // ⑧
        stateMachine.setStateRepository(repository); // ⑨
        return stateMachine;
    }
```

① — Everything starts with strongly-typed `StateRepository#configure` method, where we define

1. Entity class
2. State class (should be enum)
3. Key class (it should be possible to fetch item with its state from your store by key of this type)
    
② — We think that it should be possible to use not all of the available states (i.e. if your application is in early stages of development), so you should pass subset of allowed states into method `setAvailableStates`

③ — You should, but not necessarily, provide an implementation of `UnhandledMessageProcessor`. It's always possible in distributed system that something will go wrong and we give you the ability to handle this.
 
④ — You can define several types of handlers for your state machine:

1. `anyBefore` handlers will be executed before any transition
2. `before` handlers will be executed before concrete transition
3. `after` handlers will be executed after the concrete transition
4. `anyAfter` handlers will be executed after any transition
    
⑤ — `from` should be read as "Transition may start at any of these states"

⑥ — `to` should be read as "and can stop at any of these ones"

⑦ — `and` is delimiter method between defining several transition rulesets

⑧ — Create `StateMachine` itself

⑨ — Configure state machine behavior rules by providing it with `StateRepository`

### Use

You have 2 ways to interact with state machine

#### Inject StateMachine

If you choose to inject StateMachine into your service, then you can call `changeState` method. It returns map of your entity id to `Future` of results of execution

#### Use event publisher

You can inject `ApplicationEventPublisher` into your service and send `StateChangedEvent`s there. It is the type of one-way communication when you actually don't care about the final result.

## Requirements

Project requires Java 8 and Spring 4+

## Tests and readiness

We've done our best to write as many tests as we can. Also, we use this project at work, so we think that this project is production-ready

## Examples

You can find example of usage in state-machine-sample module

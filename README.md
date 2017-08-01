# Spring Flow State Machine

## Goal

This project was born when we understood that [spring-statemachine](http://projects.spring.io/spring-statemachine/) doesn't meet our requirements.  Our requirements are very simple:

* It should be easy to use
* It should be easy to integrate
* It should manage not state of our application, but state of entities, which our application work with
* It should be easy to configure and extend it

Actually, spring-statemachine doesn't meet any of requirements.

Almost every system we work with has some entities which have their own lifecycle.

So we've created our own library, which allows you to achieve following goals:

* Define your own states for any entity
* Define allowed transitions between states
* Define actions, which should be performed before and after transitions

## Non-goals

It is a non-goal to replace spring-statemachine (and we didn't)
It is a non-goal to provide connectors to any available storage provider

## Installation

TBD

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
        StateRepository<SimpleItem, SimpleState, String> repository = StateRepositoryBuilder.<SimpleItem, SimpleState, String>configure() // 1
                .setAvailableStates(EnumSet.allOf(SimpleState.class)) // 2
                .setUnhandledMessageProcessor((item, state, type, ex) -> LOGGER.error("Got unhandled item with id {}, issue is {}", item, type)) // 3
                .setAnyBefore((BeforeAnyTransition<SimpleItem, SimpleState>) (item, state) -> {
                    LOGGER.info("Started working on item with id {}", item.getId());
                    return true;
                }) // 4
                .defineTransitions()
                .from(STARTED) // 5
                .to(IN_PROGRESS) // 6
                .after((AfterTransition<SimpleItem>) item -> LOGGER.info("Moved from STARTED to IN_PROGRESS")) 
                .and() // 7
                // omitted for brevity
                .build(); 

        StateMachine<SimpleItem, SimpleState, String> stateMachine = new StateMachine<>(stateProvider(), stateChanger(), lockProvider); // 8
        stateMachine.setStateRepository(repository); // 9
        return stateMachine;
    }
```

1. Everything starts with strongly-typed `StateRepository#configure` method, where we define
    1. Entity class
    2. State class (should be enum)
    3. Key class (it should be possible to fetch item with its state from your store by key of this type)
2. We think that it should be possible to use not all of the available state (i.e. if your application is in early stages of development), so you should pass subset of allowed states into method `setAvailableStates`
3. You should provide an implementation of `UnhandledMessageProcessor`. It's always possible in distributed system that something will go wrong and we give you the ability to handle this. 
4. You can define several types of handlers for your statemachine:
    1. `anyBefore` handlers will be executed before any transition
    2. `before` handlers will be executed before concrete transition
    3. `after` handlers will be executed after concrete transition
    4. `anyAfter` handlers will be executed after any transition
5. `from` should be read as "Transition may start at any of these states"
6. `to` should be read as "and can stop at any of these ones"
7. `and` is delimiter method between defining several transition rulesets
8. Create `StateMachine` itself
9. Configure state machine behaviour rules by providing it with `StateRepository`

### Use

You have 2 ways to interact with statemachine

#### Inject StateMachine

If you choose to inject StateMachine into your service, than you can call `changeState` method. It returns map of your entity id to `Future` of results of execution

#### Use event publisher

You can inject `ApplicationEventPublisher` into your service and send `StateChangedEvent`s there. It is type of one-way communication, when you actually don't care about final result.

## Requirements

Project requires Java 8 and Spring 4+

## Tests and readiness

We've did our best to write as much tests as we can. Also we use this project at work, so we think that this project is production-ready
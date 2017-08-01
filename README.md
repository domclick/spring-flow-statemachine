## Spring Flow State Machine

Yet another state machine, which allows processing for multiple items through the set of defined states. You can use it rather directly or through the Spring Boot Starter (see state-machine-startr module).

The sample configuration is shown in *state-machine-samples* module.

Your items, which you move through states, must implement HasStateAndId interface. You should also implement ItemWithStateProvider and StateChanger interfaces.
Except the transitions itself, you could define before and after handlers, which would be executed before/after state transition.
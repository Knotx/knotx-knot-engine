# Fragments Actions
It is a tool-kit for integrating with different data sources (e.g. HTTP-based APIs). It 
implements stability patterns to prevent/handle network issues.

## How to start?
- add `knotx-fragments-action-core.X.Y.Z.jar` and `knotx-fragments-action-library.X.Y.Z.jar` to the classpath
- define an [action](#action) identified by name, e.g.
  ```hocon
  fetch-user {
    factory = http
    config.endpointOptions {
      path = "/service/user"
      domain = localhost
      port = 1024
      allowedRequestHeaders = ["Content-Type"]
    }
  }  
- use an [action provider](https://github.com/Knotx/knotx-fragments/tree/master/action/core#action-provider) to execute action logic (that updates a [fragment](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment))
  ```java
  provider.get("fetch-user").apply(fragmentContext, resultHandler)
  ```

## Modules:
- [API](https://github.com/Knotx/knotx-fragments/tree/master/action/api) - provides classes and interfaces required during custom actions implementation
- [Core](https://github.com/Knotx/knotx-fragments/tree/master/action/core) - initializes actions using the [action provider](https://github.com/Knotx/knotx-fragments/tree/master/action/core#action-provider)
- [Library](https://github.com/Knotx/knotx-fragments/tree/master/action/library) - a library of actions' implementations

## How does it work?
[Action](#action) is ready to use [fragment operation](https://github.com/Knotx/knotx-fragments/tree/master/api#fragment-operation) 
created by an [action factory](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action-factory). Action may be parametrized with JSON (`config`). 
You can use actions' factories to create actions, however, we recommend using the [action provider](https://github.com/Knotx/knotx-fragments/tree/master/action/core#action-provider) 
that would hide the complexity of initialization (maintaining stateful actions, combining actions 
with their behaviours).

### Action
[Action](https://github.com/Knotx/knotx-fragments/tree/master/action/api#action) can invoke an API 
and save the response body in a fragment payload or simply modify a fragment's body.

Action may be decorated with [behaviours](#behaviours). Actions deployed on the [Vert.x Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus) 
are called [Knots](https://github.com/Knotx/knotx-fragments/tree/master/action/api#knot).

Below there is a list of core actions:
- [HTTP Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#http-action) - call an external Web API and store the data in a fragment's payload
- [Inline Body Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#inline-body-action) - replaces a fragment's body with the new one
- [Inline Payload Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#inline-payload-action) - adds some data into a fragment's payload
- [Payload To Body Action](https://github.com/Knotx/knotx-fragments/tree/master/action/core#payload-to-body-action) - rewrite a fragment's payload to the body

> Please note that actions can be **stateless** and **stateful**.

### Behaviours
Behaviour is a special [action](#action) that wraps the original action and adds some functionality e.g 
employ the circuit breaker pattern or add a cache for data.

Below there is a list of core behaviours:
- [Circuit Breaker Behaviour](https://github.com/Knotx/knotx-fragments/tree/master/action/core#circuit-breaker-behaviour) - it is a kind of quarantine for actions, it use the [Vert.x Circuit Breaker](https://vertx.io/docs/vertx-circuit-breaker/java/) implementation
- [In-memory Cache Behaviour](https://github.com/Knotx/knotx-fragments/tree/master/action/core#in-memory-cache-behaviour) - caches a fragment's payload to reduce number of action invocations
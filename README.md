# build-finished DSL (prototype)

A small Gradle plugin exploring a **sweeter syntax for "do something when the build finishes"** that
is **configuration-cache compatible**.

`Gradle.buildFinished { }` is deprecated and incompatible with the configuration cache, because the
closure can capture arbitrary, mutable build state. The supported replacement —
[Flow Actions](https://docs.gradle.org/current/userguide/dataflow_actions.html) (`FlowAction` /
`FlowScope` / `FlowProviders`) — is CC-safe but verbose: you write an isolated action class, a
parameters interface, inject `FlowScope`/`FlowProviders`, and wire the build result in by hand.

This prototype hides that ceremony behind a `buildFinished.onBuildFinished { … }` DSL while keeping
the isolation guarantees of Flow Actions.

> **Status: prototype / exploration.** It uses the incubating Gradle Flow API and the `org.example.*`
> namespace. Not published; not production-ready.

## Usage

Apply the plugin in `settings.gradle(.kts)` (recommended — one registration for the whole build) or in
a `build.gradle(.kts)`:

```kotlin
plugins {
    id("org.example.buildfinished")
}
```

### React to the build finishing

```kotlin
buildFinished.onBuildFinished { outcome ->
    println(if (outcome.isFailed) "Build failed" else "Build succeeded")
}
```

`outcome` is a `BuildOutcome`: `isFailed()`, `isSuccessful()`, `getFailure()`.

### Need Gradle services? Declare them on a `BuildOutcome` subinterface

The action runs in isolation, so it can't capture services from the build script. Instead declare the
services you need as `@Inject` getters on a `BuildOutcome` subinterface, and pass its type — Gradle
injects them onto the outcome you receive:

```java
public interface NotifyOutcome extends BuildOutcome {
    @Inject ExecOperations getExec();
}

buildFinished.onBuildFinished(NotifyOutcome.class, outcome -> {
    if (outcome.isFailed()) {
        outcome.getExec().exec(spec -> spec.commandLine("say", "build failed"));
    }
});
```

### Reusable behavior as a named class

```java
public class PrintResult implements BuildFinishedAction<BuildOutcome> {
    @Override public void execute(BuildOutcome outcome) {
        System.out.println(outcome.isFailed() ? "FAILED" : "SUCCESS");
    }
}

buildFinished.onBuildFinished(new PrintResult());
```

Works the same from Groovy (`{ outcome -> … }`), Kotlin (`{ outcome -> … }`), and Java lambdas.

## Configuration cache

The whole point: this is CC-compatible. The action is serialized as an isolated input, so the **action
must not capture live build state** (no `Project`, `Task`, etc.). If it does, the configuration cache
reports it rather than silently misbehaving:

```
Invocation of 'project' references a Gradle script object from a Groovy closure
at execution time, which is unsupported with the configuration cache.
```

Injected services declared on a `BuildOutcome` subinterface are re-wired across the cache, so they
remain available on a cache hit.

## API

| Type | Role |
| --- | --- |
| `buildFinished` | the DSL extension (`BuildFinishedExtension`), on settings and project |
| `onBuildFinished(action)` | run `action` when the build finishes |
| `onBuildFinished(Class<O>, action)` | same, with services declared on a `BuildOutcome` subinterface `O` |
| `BuildOutcome` | result view: `isFailed()`, `isSuccessful()`, `getFailure()`; subinterface to add services |
| `BuildFinishedAction<O extends BuildOutcome>` | the callback: `execute(O outcome)` |

Public API lives in `org.example.buildfinished`; the Flow Actions plumbing is hidden in
`org.example.internal.buildfinished`.

## Building

```sh
./gradlew :plugin:build
```

Requires the Gradle wrapper in this repo (Gradle 9.4.1). Tests are Spock/TestKit functional tests under
`plugin/src/functionalTest`, covering project & settings application, closures/lambdas/named classes in
Groovy/Kotlin/Java, injected services, and configuration-cache store/reuse.

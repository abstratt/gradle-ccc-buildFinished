package org.example.buildflow;

/**
 * Marker for a user-defined bundle of Gradle services that build-finished behavior needs.
 *
 * <p>Because the behavior you pass to {@code buildFlow.onBuildFinished} is serialized into the
 * configuration cache, it cannot itself receive injected services. Instead, declare the services you
 * need on a specialization of this type — typically an abstract class with {@code @Inject} getters —
 * and Gradle will instantiate it (decorated, with services wired) and hand it to your behavior:
 *
 * <pre>{@code
 * abstract class MyServices implements ServiceAccess {
 *     @Inject abstract ExecOperations getExec();
 * }
 *
 * buildFlow.onBuildFinished(MyServices.class, (outcome, services) ->
 *     services.getExec().exec(spec -> spec.commandLine("say", "build done")));
 * }</pre>
 *
 * <p>The {@link ServiceAccess} instance is created fresh at execution time and is never part of the
 * serialized state, so it stays configuration-cache safe.
 */
public interface ServiceAccess {
}

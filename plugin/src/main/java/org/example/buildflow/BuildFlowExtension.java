package org.example.buildflow;

/**
 * DSL for reacting to Gradle build lifecycle events — currently, the end of the build. A
 * configuration-cache-friendly successor to {@code Gradle.buildFinished {}}.
 *
 * <p>Register a {@link BuildFinishedAction}. When the action needs Gradle services, declare them on a
 * {@link BuildOutcome} subinterface and pass its type to
 * {@link #onBuildFinished(Class, BuildFinishedAction)}.
 */
public interface BuildFlowExtension {

    /**
     * Registers an action to run when the build finishes, receiving the {@link BuildOutcome}.
     *
     * <p>The action must be compatible with the configuration cache: it must not capture live build
     * state.
     *
     * @param action the action to run once the build finishes
     */
    default void onBuildFinished(BuildFinishedAction<BuildOutcome> action) {
        onBuildFinished(BuildOutcome.class, action);
    }

    /**
     * Registers an action to run when the build finishes, receiving a {@link BuildOutcome}
     * subinterface with the Gradle services it declares.
     *
     * <p>Declare the services as {@code @Inject} getters on a {@link BuildOutcome} subinterface.
     *
     * @param outcomeType the {@link BuildOutcome} subinterface declaring the required services
     * @param action      the action to run once the build finishes
     * @param <O>         the outcome subinterface type
     */
    <O extends BuildOutcome> void onBuildFinished(
            Class<O> outcomeType, BuildFinishedAction<? super O> action);
}

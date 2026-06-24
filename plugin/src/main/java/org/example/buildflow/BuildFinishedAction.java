package org.example.buildflow;

import org.gradle.api.Action;

/**
 * Behavior to run when the build finishes, receiving the {@link BuildOutcome}.
 *
 * <p>Implement it (or pass a lambda) and register it with
 * {@link BuildFlowExtension#onBuildFinished(BuildFinishedAction)}, or, when the behavior needs
 * Gradle services, with {@link BuildFlowExtension#onBuildFinished(Class, BuildFinishedAction)}
 * passing a {@link BuildOutcome} subinterface that declares them.
 *
 * @param <O> the {@link BuildOutcome} the action receives — the base type, or a subinterface
 *            declaring the {@code @Inject} services the action needs
 */
@FunctionalInterface
public interface BuildFinishedAction<O extends BuildOutcome> extends Action<O> {

    /**
     * Invoked once the build has finished.
     *
     * @param outcome the build outcome (and any services declared on its type)
     */
    void execute(O outcome);
}

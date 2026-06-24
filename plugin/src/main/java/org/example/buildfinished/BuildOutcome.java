package org.example.buildfinished;

import org.gradle.api.flow.BuildWorkResult;
import org.gradle.api.provider.Property;

import java.util.Optional;

/**
 * A friendly, stable view of a finished build's outcome — the successor to the old
 * {@code org.gradle.BuildResult} that {@code Gradle.buildFinished {}} handlers received.
 *
 * <p>Subinterface it to declare the Gradle services your action needs, as {@code @Inject} getters;
 * they are injected onto the outcome your action receives:
 *
 * <pre>{@code
 * interface MyOutcome extends BuildOutcome {
 *     @Inject ProviderFactory getProviders();
 * }
 *
 * buildLifecycle.onBuildFinished(MyOutcome.class, outcome -> {
 *     if (outcome.isFailed()) { ... }
 *     outcome.getProviders().systemProperty("x").get();
 * });
 * }</pre>
 */
public interface BuildOutcome {

    /** {@code true} if the build's work failed. */
    default boolean isFailed() {
        return getBuildWorkResult().get().getFailure().isPresent();
    }

    /** {@code true} if the build's work completed successfully. */
    default boolean isSuccessful() {
        return !isFailed();
    }

    /** The failure that ended the build, if any. */
    default Optional<Throwable> getFailure() {
        return getBuildWorkResult().get().getFailure();
    }

    /** The underlying Gradle result this outcome is based on. */
    Property<BuildWorkResult> getBuildWorkResult();
}

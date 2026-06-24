package org.example.buildflow;

import org.gradle.api.flow.BuildWorkResult;

import java.util.Optional;

/**
 * A friendly, stable view of a build's outcome — the spiritual successor to the old
 * {@code org.gradle.BuildResult} that {@code Gradle.buildFinished {}} handlers received.
 *
 * <p>Shields consumer code from the incubating {@link BuildWorkResult}.
 */
public interface BuildOutcome {

    /** {@code true} if the build's work failed. */
    boolean isFailed();

    /** {@code true} if the build's work completed successfully. */
    boolean isSuccessful();

    /** The failure that occurred, if any. */
    Optional<Throwable> getFailure();
}

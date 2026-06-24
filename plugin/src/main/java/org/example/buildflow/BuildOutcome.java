package org.example.buildflow;

import org.gradle.api.flow.BuildWorkResult;

import java.util.Optional;

/**
 * A friendly, stable view of a build's outcome — the spiritual successor to the old
 * {@code org.gradle.BuildResult} that {@code Gradle.buildFinished {}} handlers received.
 *
 * <p>Wraps the incubating {@link BuildWorkResult} so consumer code does not depend on it directly.
 */
public final class BuildOutcome {

    private final BuildWorkResult result;

    public BuildOutcome(BuildWorkResult result) {
        this.result = result;
    }

    /** {@code true} if the build's work failed. */
    public boolean isFailed() {
        return result.getFailure().isPresent();
    }

    /** {@code true} if the build's work completed successfully. */
    public boolean isSuccessful() {
        return !isFailed();
    }

    /** The failure that occurred, if any. */
    public Optional<Throwable> getFailure() {
        return result.getFailure();
    }

    /** The underlying incubating result, for advanced use. */
    public BuildWorkResult getBuildWorkResult() {
        return result;
    }
}

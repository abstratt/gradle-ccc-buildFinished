package org.example.internal.buildflow;

import org.example.buildflow.BuildOutcome;
import org.gradle.api.flow.BuildWorkResult;

import java.util.Optional;

/**
 * Default implementation of {@link BuildOutcome}, wrapping the incubating {@link BuildWorkResult}.
 */
public final class DefaultBuildOutcome implements BuildOutcome {

    private final BuildWorkResult result;

    public DefaultBuildOutcome(BuildWorkResult result) {
        this.result = result;
    }

    @Override
    public boolean isFailed() {
        return result.getFailure().isPresent();
    }

    @Override
    public boolean isSuccessful() {
        return !isFailed();
    }

    @Override
    public Optional<Throwable> getFailure() {
        return result.getFailure();
    }
}

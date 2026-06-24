package org.example.internal.buildflow;

import org.example.buildflow.BuildFinishedAction;
import org.example.buildflow.BuildOutcome;
import org.gradle.api.flow.BuildWorkResult;
import org.gradle.api.flow.FlowAction;
import org.gradle.api.flow.FlowParameters;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

/**
 * Internal {@link FlowAction} backing the {@code buildFlow} DSL. It carries the decorated
 * outcome, the user action, and the build result as isolated parameters; once the build finishes it
 * feeds the result into the outcome and invokes the action. Gradle re-wires the outcome's injected
 * services across the configuration cache.
 */
public abstract class BuildFinishedFlowAction
        implements FlowAction<BuildFinishedFlowAction.Parameters> {

    public interface Parameters extends FlowParameters {

        /** The outcome of the build's work, wired in from {@code FlowProviders}. */
        @Input
        Property<BuildWorkResult> getBuildResult();

        /** The decorated outcome instance (with any injected services) handed to the action. */
        @Input
        Property<BuildOutcome> getOutcome();

        /** The user-supplied action to run once the build finishes. */
        @Input
        Property<BuildFinishedAction<BuildOutcome>> getAction();
    }

    @Override
    public void execute(Parameters parameters) {
        BuildOutcome outcome = parameters.getOutcome().get();
        outcome.getBuildWorkResult().set(parameters.getBuildResult().get());
        parameters.getAction().get().execute(outcome);
    }
}

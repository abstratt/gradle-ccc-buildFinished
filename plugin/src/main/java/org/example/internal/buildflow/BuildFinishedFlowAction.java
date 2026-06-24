package org.example.internal.buildflow;

import org.example.buildflow.BuildFinishedAction;
import org.example.buildflow.BuildOutcome;
import org.example.buildflow.ServiceAccess;
import org.gradle.api.flow.BuildWorkResult;
import org.gradle.api.flow.FlowAction;
import org.gradle.api.flow.FlowParameters;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

/**
 * Internal adapter: the single isolated {@link FlowAction} that backs the {@code buildFlow} DSL.
 *
 * <p>Consumers never see this type. They provide a plain {@link BuildFinishedAction}, which is carried
 * here as an isolated parameter and invoked once the build finishes. Using a Flow Action is purely an
 * implementation detail.
 *
 * <p>When a {@link ServiceAccess} bundle is supplied, it is created (decorated, with {@code @Inject}
 * services wired) by the DSL and passed in as a parameter; Gradle re-wires its services across the
 * configuration cache.
 */
public abstract class BuildFinishedFlowAction
        implements FlowAction<BuildFinishedFlowAction.Parameters> {

    public interface Parameters extends FlowParameters {

        /** The outcome of the build's work, wired in from {@code FlowProviders}. */
        @Input
        Property<BuildWorkResult> getBuildResult();

        /** The user-supplied behavior to run once the build finishes. */
        @Input
        Property<BuildFinishedAction<ServiceAccess>> getBehavior();

        /** The decorated {@link ServiceAccess} bundle handed to the behavior, if any. */
        @Input
        @Optional
        Property<ServiceAccess> getServices();
    }

    @Override
    public void execute(Parameters parameters) {
        BuildOutcome outcome = new DefaultBuildOutcome(parameters.getBuildResult().get());
        ServiceAccess services = parameters.getServices().getOrNull();
        parameters.getBehavior().get().execute(outcome, services);
    }
}

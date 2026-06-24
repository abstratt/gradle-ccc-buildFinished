package org.example.internal.buildflow;

import org.example.buildflow.BuildFinishedAction;
import org.example.buildflow.BuildFlowExtension;
import org.example.buildflow.BuildOutcome;
import org.gradle.api.flow.FlowProviders;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

/**
 * Default implementation of {@link BuildFlowExtension}.
 *
 * <p>Instantiates the (decorated) {@link BuildOutcome} via {@link ObjectFactory} so its
 * {@code @Inject} services are wired, wires the build result from
 * {@link FlowProviders#getBuildWorkResult()}, and carries both plus the user action as isolated
 * parameters of {@link BuildFinishedFlowAction}.
 */
public abstract class DefaultBuildFlowExtension implements BuildFlowExtension {

    private final FlowScope flowScope;
    private final FlowProviders flowProviders;

    @Inject
    public DefaultBuildFlowExtension(FlowScope flowScope, FlowProviders flowProviders) {
        this.flowScope = flowScope;
        this.flowProviders = flowProviders;
    }

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @SuppressWarnings("unchecked")
    @Override
    public <O extends BuildOutcome> void onBuildFinished(
            Class<O> outcomeType, BuildFinishedAction<? super O> action) {
        O outcome = getObjectFactory().newInstance(outcomeType);
        flowScope.always(BuildFinishedFlowAction.class, spec -> {
            spec.getParameters().getOutcome().set(outcome);
            spec.getParameters().getAction().set((BuildFinishedAction<BuildOutcome>) action);
            spec.getParameters().getBuildResult().set(flowProviders.getBuildWorkResult());
        });
    }
}

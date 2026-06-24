package org.example.internal.buildflow;

import org.example.buildflow.BuildFinishedAction;
import org.example.buildflow.BuildFlowExtension;
import org.example.buildflow.BuildOutcome;
import org.example.buildflow.ServiceAccess;
import org.gradle.api.flow.FlowProviders;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;
import java.util.function.Consumer;

/**
 * Default implementation of the {@link BuildFlowExtension} DSL.
 *
 * <p>Centralizes the {@link FlowScope#always} registration: it auto-wires the build result from
 * {@link FlowProviders#getBuildWorkResult()} and, when services are requested, instantiates the
 * decorated {@link ServiceAccess} bundle via {@link ObjectFactory}, then carries both as isolated
 * parameters of the internal {@link BuildFinishedFlowAction}.
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

    @Override
    public void onBuildFinished(Consumer<BuildOutcome> behavior) {
        register(new IgnoreServices(behavior), ServiceAccess.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends ServiceAccess> void onBuildFinished(
            Class<S> serviceAccessType, BuildFinishedAction<? super S> action) {
        register((BuildFinishedAction<ServiceAccess>) action, serviceAccessType);
    }

    private <S extends ServiceAccess> void register(BuildFinishedAction<ServiceAccess> action, Class<S> serviceAccessType) {
        flowScope.always(BuildFinishedFlowAction.class, spec -> {
            spec.getParameters().getBuildResult().set(flowProviders.getBuildWorkResult());
            spec.getParameters().getBehavior().set(action);
            if (serviceAccessType != ServiceAccess.class) {
                ServiceAccess services = getObjectFactory().newInstance(serviceAccessType);
                spec.getParameters().getServices().set(services);
            }
        });
    }

    /** Adapts a service-free {@link Consumer} to the internal {@link BuildFinishedAction} shape. */
    private static final class IgnoreServices implements BuildFinishedAction<ServiceAccess> {

        private final Consumer<BuildOutcome> delegate;

        IgnoreServices(Consumer<BuildOutcome> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(BuildOutcome outcome, ServiceAccess services) {
            delegate.accept(outcome);
        }
    }
}

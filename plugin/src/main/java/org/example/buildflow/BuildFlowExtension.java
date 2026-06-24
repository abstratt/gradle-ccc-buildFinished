package org.example.buildflow;

import org.gradle.api.flow.FlowProviders;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;
import java.util.function.Consumer;

/**
 * The {@code buildFlow} DSL. Provides a sweet, {@code buildFinished}-like syntax for reacting to the
 * end of a build.
 *
 * <p>Consumers supply plain behavior describing what should happen when the build finishes. The DSL
 * runs it inside an isolated Flow Action — that detail is hidden — and the configuration cache
 * isolates the behavior like any other action input, so it must not capture non-serializable build
 * state. When the behavior needs Gradle services, declare them on a {@link ServiceAccess} type and
 * use the {@link #onBuildFinished(Class, BuildFinishedAction)} overload.
 */
public abstract class BuildFlowExtension {

    private final FlowScope flowScope;
    private final FlowProviders flowProviders;

    @Inject
    public BuildFlowExtension(FlowScope flowScope, FlowProviders flowProviders) {
        this.flowScope = flowScope;
        this.flowProviders = flowProviders;
    }

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    /**
     * Registers behavior to run when the build finishes, receiving the {@link BuildOutcome}.
     *
     * <p>The behavior is executed in an isolated context. Any state it holds is captured into the
     * configuration cache, so it must be serializable and must not reference live build state.
     *
     * @param behavior the action to run once the build finishes
     */
    public void onBuildFinished(Consumer<BuildOutcome> behavior) {
        register(new IgnoreServices(behavior), null);
    }

    /**
     * Registers behavior to run when the build finishes, receiving the {@link BuildOutcome} and a
     * bundle of Gradle services.
     *
     * <p>Declare the services you need on a {@link ServiceAccess} specialization (typically an
     * abstract class with {@code @Inject} getters). Gradle instantiates it fresh at execution time —
     * it is never serialized — so injected services stay configuration-cache safe.
     *
     * @param serviceAccessType the {@link ServiceAccess} type declaring the required services
     * @param behavior          the action to run once the build finishes
     * @param <S>               the service-access type
     */
    @SuppressWarnings("unchecked")
    public <S extends ServiceAccess> void onBuildFinished(
            Class<S> serviceAccessType, BuildFinishedAction<? super S> action) {
        S services = getObjectFactory().newInstance(serviceAccessType);
        register((BuildFinishedAction<ServiceAccess>) action, services);
    }

    private void register(BuildFinishedAction<ServiceAccess> action, ServiceAccess services) {
        flowScope.always(BuildFinishedFlowAction.class, spec -> {
            spec.getParameters().getBuildResult().set(flowProviders.getBuildWorkResult());
            spec.getParameters().getBehavior().set(action);
            if (services != null) {
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

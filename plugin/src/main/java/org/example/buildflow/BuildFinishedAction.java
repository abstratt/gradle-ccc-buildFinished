package org.example.buildflow;

/**
 * Behavior to run when the build finishes, with access to a bundle of Gradle services.
 *
 * <p>A plain functional callback — not coupled to the Flow Actions API. Implement it (or pass a
 * lambda) and register it via {@link BuildFlowExtension#onBuildFinished(Class, BuildFinishedAction)}.
 * The {@code services} bundle is the {@link ServiceAccess} specialization you declared, which Gradle
 * instantiates with its {@code @Inject} services wired.
 *
 * <p>For build-finished behavior that needs no services, use
 * {@link BuildFlowExtension#onBuildFinished(java.util.function.Consumer)} instead.
 *
 * @param <SA> the {@link ServiceAccess} type declaring the services this action needs
 */
@FunctionalInterface
public interface BuildFinishedAction<SA extends ServiceAccess> {
    /**
     * Invoked once the build has finished.
     *
     * @param outcome  whether the build succeeded or failed, and the failure if any
     * @param services the requested Gradle services
     */
    void execute(BuildOutcome outcome, SA services);
}

package org.example.buildflow;

import org.gradle.api.Plugin;
import org.gradle.api.flow.FlowProviders;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.plugins.ExtensionAware;

import javax.inject.Inject;

/**
 * Registers the {@code buildFlow} DSL extension.
 *
 * <p>Applies to both settings and project targets: {@code Settings} and {@code Project} are both
 * {@link ExtensionAware}, and {@link FlowScope}/{@link FlowProviders} are injectable in both
 * contexts, so the same plugin id can be used in {@code settings.gradle(.kts)} and
 * {@code build.gradle(.kts)}.
 */
public abstract class BuildFlowPlugin implements Plugin<ExtensionAware> {

    @Inject
    protected abstract FlowScope getFlowScope();

    @Inject
    protected abstract FlowProviders getFlowProviders();

    @Override
    public void apply(ExtensionAware target) {
        target.getExtensions().create(
                "buildFlow", BuildFlowExtension.class, getFlowScope(), getFlowProviders());
    }
}

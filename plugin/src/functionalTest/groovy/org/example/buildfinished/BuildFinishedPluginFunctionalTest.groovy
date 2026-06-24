package org.example.buildfinished

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

/**
 * Functional tests for the {@code org.example.buildfinished} plugin, exercised through Gradle TestKit
 * against real builds.
 *
 * <p>Each build defines its own behavior as a plain {@code Consumer<BuildOutcome>} — the user-facing
 * type is not coupled to the Flow Actions API.
 */
class BuildFinishedPluginFunctionalTest extends Specification {

    @TempDir
    File projectDir

    /** A user-authored behavior: a named {@code BuildFinishedAction} carrying its own state. */
    private static final String PRINT_CONSUMER = '''
        import org.example.buildfinished.BuildOutcome
        import org.example.buildfinished.BuildFinishedAction

        class PrintBuildResult implements BuildFinishedAction<BuildOutcome> {
            String successMessage = "Build finished: SUCCESS"
            String failureMessage = "Build finished: FAILED"

            void execute(BuildOutcome outcome) {
                println outcome.failed ? failureMessage : successMessage
            }
        }
    '''

    /** A named, CC-serializable Java behavior carrying state — the configuration-cache-safe Java form. */
    private static final String PRINT_OUTCOME_JAVA = '''
        package com.example;

        import org.example.buildfinished.BuildOutcome;
        import org.example.buildfinished.BuildFinishedAction;

        public class PrintOutcome implements BuildFinishedAction<BuildOutcome> {
            private final String prefix;

            public PrintOutcome(String prefix) {
                this.prefix = prefix;
            }

            @Override
            public void execute(BuildOutcome outcome) {
                System.out.println(prefix + (outcome.isFailed() ? "FAILED" : "SUCCESS"));
            }
        }
    '''

    /** A Java Gradle plugin that registers build-finished behavior via a (non-capturing) lambda. */
    private static final String LAMBDA_PLUGIN_JAVA = '''
        package com.example;

        import org.example.buildfinished.BuildFinishedExtension;
        import org.example.internal.buildfinished.BuildFinishedPlugin;
        import org.gradle.api.Plugin;
        import org.gradle.api.Project;

        public class LambdaPlugin implements Plugin<Project> {
            @Override
            public void apply(Project project) {
                project.getPluginManager().apply(BuildFinishedPlugin.class);
                BuildFinishedExtension buildFinished = project.getExtensions().getByType(BuildFinishedExtension.class);
                buildFinished.onBuildFinished(outcome ->
                        System.out.println("Java lambda: " + (outcome.isFailed() ? "FAILED" : "SUCCESS")));
            }
        }
    '''

    private File file(String name) {
        new File(projectDir, name)
    }

    private GradleRunner runner(String... args) {
        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(args)
                .withProjectDir(projectDir)
    }

    def "runs the behavior when applied to a project"() {
        given:
        file("settings.gradle") << ""
        file("build.gradle") << """
            plugins { id('org.example.buildfinished') }
            $PRINT_CONSUMER
            buildFinished.onBuildFinished(new PrintBuildResult())
        """

        when:
        BuildResult result = runner("help").build()

        then:
        result.output.contains("Build finished: SUCCESS")
    }

    def "runs the behavior exactly once when applied to settings"() {
        given:
        file("settings.gradle") << """
            plugins { id('org.example.buildfinished') }
            $PRINT_CONSUMER
            buildFinished.onBuildFinished(new PrintBuildResult())
        """
        file("build.gradle") << ""

        when:
        BuildResult result = runner("help").build()

        then:
        result.output.count("Build finished: SUCCESS") == 1
    }

    def "carries state held by the behavior instance"() {
        given:
        file("settings.gradle") << ""
        file("build.gradle") << """
            plugins { id('org.example.buildfinished') }
            $PRINT_CONSUMER
            buildFinished.onBuildFinished(new PrintBuildResult(successMessage: 'all good ✨'))
        """

        when:
        BuildResult result = runner("help").build()

        then:
        result.output.contains("all good ✨")
    }

    def "provides access to injected services via a BuildOutcome subinterface"() {
        given:
        file("settings.gradle") << ""
        file("build.gradle") << """
            import org.example.buildfinished.BuildOutcome
            import org.example.buildfinished.BuildFinishedAction
            import org.gradle.api.provider.ProviderFactory
            import javax.inject.Inject

            plugins { id('org.example.buildfinished') }

            interface MyOutcome extends BuildOutcome {
                @Inject ProviderFactory getProviders()
            }

            class CheckService implements BuildFinishedAction<MyOutcome> {
                void execute(MyOutcome outcome) {
                    println "marker=" + outcome.providers.systemProperty("marker").get()
                }
            }

            buildFinished.onBuildFinished(MyOutcome, new CheckService())
        """

        when:
        BuildResult result = runner("help", "-Dmarker=service-works").build()

        then:
        result.output.contains("marker=service-works")
    }

    def "supports a Groovy closure instead of a class"() {
        given:
        file("settings.gradle") << ""
        file("build.gradle") << """
            plugins { id('org.example.buildfinished') }
            buildFinished.onBuildFinished {
                println it.successful ? 'closure: SUCCESS' : 'closure: FAILED'
            }
        """

        when:
        BuildResult result = runner("help").build()

        then:
        result.output.contains("closure: SUCCESS")
    }

    def "supports a Kotlin DSL lambda instead of a class"() {
        given:
        file("settings.gradle.kts") << ""
        file("build.gradle.kts") << """
            plugins { id("org.example.buildfinished") }
            buildFinished.onBuildFinished {
                println(if (it.isFailed) "lambda: FAILED" else "lambda: SUCCESS")
            }
        """

        when:
        BuildResult result = runner("help").build()

        then:
        result.output.contains("lambda: SUCCESS")
    }

    def "a closure is configuration cache compatible when it captures only serializable state"() {
        given:
        String filesArg = pluginClasspath()
                .collect { "'${it.absolutePath.replace('\\', '/')}'" }
                .join(', ')
        file("settings.gradle") << ""
        file("build.gradle") << """
            buildscript {
                dependencies { classpath files($filesArg) }
            }
            apply plugin: 'org.example.buildfinished'
            def label = 'closure'
            buildFinished.onBuildFinished { outcome ->
                println label + (outcome.successful ? ": SUCCESS" : ": FAILED")
            }
        """

        when: "the build runs for the first time with the configuration cache"
        BuildResult first = runnerWithoutInjection("help", "--configuration-cache").build()

        then:
        first.output.contains("Configuration cache entry stored")
        !first.output.contains("problems were found")
        first.output.contains("closure: SUCCESS")

        when: "the build runs a second time, served from the cache"
        BuildResult second = runnerWithoutInjection("help", "--configuration-cache").build()

        then:
        second.output.contains("Configuration cache entry reused")
        second.output.contains("closure: SUCCESS")
    }

    def "the configuration cache rejects a closure that captures live build state"() {
        given:
        String filesArg = pluginClasspath()
                .collect { "'${it.absolutePath.replace('\\', '/')}'" }
                .join(', ')
        file("settings.gradle") << ""
        file("build.gradle") << """
            buildscript {
                dependencies { classpath files($filesArg) }
            }
            apply plugin: 'org.example.buildfinished'
            buildFinished.onBuildFinished { outcome ->
                // Referencing 'project' captures live build state — the isolation the Flow API enforces.
                println "project name is " + project.name
            }
        """

        when:
        BuildResult result = runnerWithoutInjection("help", "--configuration-cache").buildAndFail()

        then: "the configuration cache flags the captured build state instead of silently misbehaving"
        result.output.contains("references a Gradle script object from a Groovy closure")
    }

    def "runs the behavior when the build fails"() {
        given:
        file("settings.gradle") << ""
        file("build.gradle") << """
            plugins { id('org.example.buildfinished') }
            $PRINT_CONSUMER
            buildFinished.onBuildFinished(new PrintBuildResult(failureMessage: 'something broke 💥'))
            tasks.register('boom') { doLast { throw new RuntimeException('kaboom') } }
        """

        when:
        BuildResult result = runner("boom").buildAndFail()

        then:
        result.output.contains("something broke 💥")
    }

    def "is configuration cache compatible"() {
        given: "the plugin on the build's own classpath (a real, persisted classpath) rather than injected"
        // TestKit's injected plugin classpath is not a real classpath, so a build-script-defined
        // consumer that references plugin types (BuildOutcome) cannot be reconstructed at flow
        // execution under the configuration cache. Putting the plugin on the build's own classpath
        // mirrors how a published plugin is consumed in practice.
        String filesArg = pluginClasspath()
                .collect { "'${it.absolutePath.replace('\\', '/')}'" }
                .join(', ')
        file("settings.gradle") << ""
        file("build.gradle") << """
            buildscript {
                dependencies { classpath files($filesArg) }
            }
            apply plugin: 'org.example.buildfinished'
            $PRINT_CONSUMER
            buildFinished.onBuildFinished(new PrintBuildResult())
        """

        when: "the build runs for the first time with the configuration cache"
        BuildResult first = runnerWithoutInjection("help", "--configuration-cache").build()

        then: "the entry is stored with no problems"
        first.output.contains("Configuration cache entry stored")
        !first.output.contains("problems were found")

        when: "the build runs a second time"
        BuildResult second = runnerWithoutInjection("help", "--configuration-cache").build()

        then: "the entry is reused and the behavior still runs on a cache hit"
        second.output.contains("Configuration cache entry reused")
        second.output.contains("Build finished: SUCCESS")
    }

    def "runs Java build logic provided as a binary plugin"() {
        given: "a reusable Gradle plugin written in Java that registers buildFinished behavior"
        writeIncludedBuild("com.example.ReportingPlugin", [
                "PrintOutcome.java"   : PRINT_OUTCOME_JAVA,
                "ReportingPlugin.java": '''
                    package com.example;
            
                    import org.example.buildfinished.BuildFinishedExtension;
                    import org.example.internal.buildfinished.BuildFinishedPlugin;
                    import org.gradle.api.Plugin;
                    import org.gradle.api.Project;
            
                    public class ReportingPlugin implements Plugin<Project> {
                        @Override
                        public void apply(Project project) {
                            project.getPluginManager().apply(BuildFinishedPlugin.class);
                            BuildFinishedExtension buildFinished = project.getExtensions().getByType(BuildFinishedExtension.class);
                            buildFinished.onBuildFinished(outcome -> System.out.println("Java plugin: " + (outcome.isFailed() ? "FAILED" : "SUCCESS")));
                        }
                    }
                ''',
        ])
        file("settings.gradle") << "pluginManagement { includeBuild('build-logic') }\n"
        file("build.gradle") << "plugins { id('org.example.reporting') }\n"

        when: "the build runs for the first time with the configuration cache"
        BuildResult first = runnerWithoutInjection("help", "--configuration-cache").build()

        then:
        first.output.contains("Configuration cache entry stored")
        !first.output.contains("problems were found")
        first.output.contains("Java plugin: SUCCESS")

        when: "the build runs a second time, served from the cache"
        BuildResult second = runnerWithoutInjection("help", "--configuration-cache").build()

        then:
        second.output.contains("Configuration cache entry reused")
        second.output.contains("Java plugin: SUCCESS")
    }

    def "Java build logic can register a lambda and stays configuration cache compatible"() {
        given: "a Java plugin that registers behavior via a non-capturing lambda"
        writeIncludedBuild("com.example.LambdaPlugin", ["LambdaPlugin.java": LAMBDA_PLUGIN_JAVA])
        file("settings.gradle") << "pluginManagement { includeBuild('build-logic') }\n"
        file("build.gradle") << "plugins { id('org.example.reporting') }\n"

        when: "the build runs for the first time with the configuration cache"
        BuildResult first = runnerWithoutInjection("help", "--configuration-cache").build()

        then:
        first.output.contains("Configuration cache entry stored")
        !first.output.contains("problems were found")
        first.output.contains("Java lambda: SUCCESS")

        when: "the build runs a second time, served from the cache"
        BuildResult second = runnerWithoutInjection("help", "--configuration-cache").build()

        then:
        second.output.contains("Configuration cache entry reused")
        second.output.contains("Java lambda: SUCCESS")
    }

    def "injected services survive the configuration cache"() {
        given:
        String filesArg = pluginClasspath()
                .collect { "'${it.absolutePath.replace('\\', '/')}'" }
                .join(', ')
        file("settings.gradle") << ""
        file("build.gradle") << """
            buildscript {
                dependencies { classpath files($filesArg) }
            }
            import org.example.buildfinished.BuildOutcome
            import org.example.buildfinished.BuildFinishedAction
            import org.gradle.api.provider.ProviderFactory
            import javax.inject.Inject

            apply plugin: 'org.example.buildfinished'

            interface MyOutcome extends BuildOutcome {
                @Inject ProviderFactory getProviders()
            }

            class CheckService implements BuildFinishedAction<MyOutcome> {
                void execute(MyOutcome outcome) {
                    println "marker=" + outcome.providers.systemProperty("marker").get()
                }
            }

            buildFinished.onBuildFinished(MyOutcome, new CheckService())
        """

        when: "the build runs for the first time with the configuration cache"
        BuildResult first = runnerWithoutInjection("help", "--configuration-cache", "-Dmarker=service-works").build()

        then:
        first.output.contains("Configuration cache entry stored")
        first.output.contains("marker=service-works")

        when: "the build runs a second time, served from the cache"
        BuildResult second = runnerWithoutInjection("help", "--configuration-cache", "-Dmarker=service-works").build()

        then: "the re-wired service still works on a cache hit"
        second.output.contains("Configuration cache entry reused")
        second.output.contains("marker=service-works")
    }

    /**
     * A runner that relies on the plugin being on the build's own classpath rather than the injected
     * TestKit classpath, which is required for configuration-cache compatibility of build-script
     * classes that reference plugin types.
     */
    private GradleRunner runnerWithoutInjection(String... args) {
        GradleRunner.create()
                .forwardOutput()
                .withArguments(args)
                .withProjectDir(projectDir)
    }

    /**
     * Writes a {@code build-logic} included build exposing the {@code org.example.reporting} plugin,
     * implemented in Java by {@code implementationClass} from the given {@code com.example} Java
     * sources. The plugin compiles against — and runs from — the plugin under test on a real classpath
     * (no TestKit injection), which is what a published-plugin consumer would do.
     */
    private void writeIncludedBuild(String implementationClass, Map<String, String> javaSources) {
        String filesArg = pluginClasspath()
                .collect { "'${it.absolutePath.replace('\\', '/')}'" }
                .join(', ')
        file("build-logic").mkdirs()
        file("build-logic/settings.gradle") << "rootProject.name = 'build-logic'\n"
        file("build-logic/build.gradle") << """
            plugins { id 'java-gradle-plugin' }
            dependencies { implementation files($filesArg) }
            gradlePlugin {
                plugins {
                    reporting {
                        id = 'org.example.reporting'
                        implementationClass = '$implementationClass'
                    }
                }
            }
        """
        File pkg = file("build-logic/src/main/java/com/example")
        pkg.mkdirs()
        javaSources.each { name, source -> new File(pkg, name) << source }
    }

    /** The implementation classpath of the plugin under test, as injected by {@code java-gradle-plugin}. */
    private List<File> pluginClasspath() {
        URL resource = getClass().classLoader.getResource("plugin-under-test-metadata.properties")
        assert resource != null: "plugin-under-test-metadata.properties not found on the test classpath"
        Properties props = new Properties()
        resource.openStream().withCloseable { props.load(it) }
        props.getProperty("implementation-classpath")
                .split(File.pathSeparator)
                .collect { new File(it) }
    }
}

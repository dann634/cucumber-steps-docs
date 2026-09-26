package com.jackson.cucumberdocs.maven;

import com.jackson.cucumberdocs.CucumberStep;
import com.jackson.cucumberdocs.HtmlDocumentationGenerator;
import com.jackson.cucumberdocs.StepScanner;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true)
public class GenerateStepDocsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(
            defaultValue = "${project.build.outputDirectory}",
            readonly = true,
            required = true
    )
    private File classesDirectory;

    @Parameter(
            defaultValue = "${project.build.testOutputDirectory}",
            readonly = true,
            required = true
    )
    private File testClassesDirectory;

    @Parameter(defaultValue = "${project.build.directory}/cucumber-step-documentation.html")
    private File outputFile;

    @Parameter(property = "cucumber.step.docs.failOnError", defaultValue = "true")
    private boolean failOnError;

    @Parameter(property = "cucumber.step.docs.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {

        getLog().info("STARTING CUCUMBER GENERATOR");

        if (skip) {
            getLog().info("Cucumber step documentation generation skipped.");
            return;
        }

        getLog().info("Generating Cucumber step documentation...");

        try {

            List<String> classpath = buildClasspath();

            StepScanner scanner = new StepScanner();

            List<CucumberStep> steps = scanner.scan(
                    List.of(classesDirectory.toPath(), testClassesDirectory.toPath()),
                    classpath
            );

            getLog().info("Found " + steps.size() + " Cucumber step definitions.");

            HtmlDocumentationGenerator generator = new HtmlDocumentationGenerator();

            generator.generate(steps, outputFile.toPath());

            getLog().info("Cucumber step documentation generated at: " + outputFile.getAbsolutePath());

        } catch (Exception e) {

            if (failOnError) {
                throw new MojoExecutionException("Failed to generate Cucumber step documentation.", e);
            }

            getLog().warn("Failed to generate Cucumber step documentation.", e);
        }
    }

    private List<String> buildClasspath() throws MojoExecutionException {

        List<String> classpath = new ArrayList<>();

        classpath.add(classesDirectory.getAbsolutePath());
        classpath.add(testClassesDirectory.getAbsolutePath());

        try {
            classpath.addAll(project.getTestClasspathElements());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Failed to resolve the test classpath", e);
        }

        return classpath;
    }
}

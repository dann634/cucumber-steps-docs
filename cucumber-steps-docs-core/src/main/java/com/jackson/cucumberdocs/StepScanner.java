package com.jackson.cucumberdocs;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Finds compiled Cucumber step definitions in the consuming project's output
 * directories and test classpath.
 *
 * <p>The scanner recognizes the standard Cucumber {@code Given}, {@code When},
 * {@code Then}, {@code And}, and {@code But} annotations by name, allowing the
 * consuming project to use its own Cucumber version.</p>
 */
public class StepScanner {

    private static final java.util.Map<String, String> STEP_ANNOTATIONS = java.util.Map.of(
            "io.cucumber.java.en.Given", "Given",
            "io.cucumber.java.en.When", "When",
            "io.cucumber.java.en.Then", "Then",
            "io.cucumber.java.en.And", "And",
            "io.cucumber.java.en.But", "But"
    );

    /** Creates a scanner for Cucumber step definition classes. */
    public StepScanner() {
    }

    /**
     * Scans class directories for Cucumber step definition methods.
     *
     * @param classDirectories directories containing compiled project or test classes
     * @param classpath classpath entries required to load those classes and annotations
     * @return the discovered step definitions
     * @throws Exception if a classpath entry, class, or step annotation cannot be loaded or inspected
     */
    public List<CucumberStep> scan(List<Path> classDirectories, List<String> classpath) throws Exception {

        List<CucumberStep> steps = new ArrayList<>();

        URL[] urls = classpath.stream().map(this::toUrl).toArray(URL[]::new);

        try (URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader())) {

            for (Path classDirectory : classDirectories) {

                if (!Files.exists(classDirectory)) {
                    continue;
                }

                try (Stream<Path> files = Files.walk(classDirectory)) {

                    files.filter(path -> path.toString().endsWith(".class")).filter(path -> !path.getFileName().toString().contains("$")).sorted(Comparator.naturalOrder()).forEach(path -> {
                        try {
                            String className = toClassName(classDirectory, path);

                            Class<?> clazz = Class.forName(className, false, classLoader);

                            scanClass(clazz, steps);

                        } catch (Exception e) {
                            throw new RuntimeException("Failed to scan class: " + path, e);
                        }
                    });
                }
            }
        }

        return steps;
    }

    private void scanClass(Class<?> clazz, List<CucumberStep> steps) {

        for (Method method : clazz.getDeclaredMethods()) {

            for (Annotation annotation : method.getDeclaredAnnotations()) {
                String keyword = STEP_ANNOTATIONS.get(annotation.annotationType().getName());
                if (keyword == null) {
                    continue;
                }

                String expression = getExpression(method, annotation);

                StepDescription description = method.getAnnotation(StepDescription.class);

                String descriptionText = description != null ? description.value() : "";

                steps.add(new CucumberStep(keyword, expression, descriptionText, clazz.getName(), method.getName()));
            }
        }
    }

    private String getExpression(Method method, Annotation annotation) {
        try {
            Method valueMethod = annotation.annotationType().getMethod("value");

            return (String) valueMethod.invoke(annotation);

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to get Cucumber expression from " +
                            method.getDeclaringClass().getName() + "#" + method.getName(),
                    e
            );
        }
    }

    private String toClassName(Path root, Path classFile) {

        String relative = root.relativize(classFile).toString();

        return relative.substring(0, relative.length() - ".class".length()).replace('/', '.').replace('\\', '.');
    }

    private URL toUrl(String path) {

        try {
            return Path.of(path).toUri().toURL();
        } catch (Exception e) {
            throw new RuntimeException("Could not create classpath URL: " + path, e);
        }
    }
}

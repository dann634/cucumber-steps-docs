package com.jackson.cucumberdocs;


import io.cucumber.java.en.*;

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

public class StepScanner {

    private static final Class<?>[] STEP_ANNOTATIONS = {Given.class, When.class, Then.class, And.class, But.class};

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

            for (Class<?> annotationClass : STEP_ANNOTATIONS) {

                if (!method.isAnnotationPresent((Class) annotationClass)) {
                    continue;
                }

                String keyword = getKeyword(annotationClass);
                String expression = getExpression(method, annotationClass);

                StepDescription description = method.getAnnotation(StepDescription.class);

                String descriptionText = description != null ? description.value() : "";

                steps.add(new CucumberStep(keyword, expression, descriptionText, clazz.getName(), method.getName()));
            }
        }
    }

    private String getKeyword(Class<?> annotationClass) {

        if (annotationClass == Given.class) {
            return "Given";
        }

        if (annotationClass == When.class) {
            return "When";
        }

        if (annotationClass == Then.class) {
            return "Then";
        }

        if (annotationClass == And.class) {
            return "And";
        }

        if (annotationClass == But.class) {
            return "But";
        }

        throw new IllegalArgumentException("Unknown Cucumber annotation: " + annotationClass);
    }

    private String getExpression(Method method, Class<?> annotationClass) {
        try {
            Annotation annotation = method.getAnnotation((Class<? extends Annotation>) annotationClass);

            Method valueMethod = annotationClass.getMethod("value");

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
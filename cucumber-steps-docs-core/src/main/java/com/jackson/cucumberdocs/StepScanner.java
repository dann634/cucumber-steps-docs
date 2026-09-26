package com.jackson.cucumberdocs;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Finds compiled Cucumber step definitions in project output directories and
 * dependency JARs on the consuming project's test classpath.
 *
 * <p>Class files are inspected directly, without loading the step classes or a
 * particular Cucumber version into the Maven plugin classloader.</p>
 */
public class StepScanner {

    private static final java.util.Map<String, String> STEP_ANNOTATIONS = java.util.Map.of(
            "Lio/cucumber/java/en/Given;", "Given",
            "Lio/cucumber/java/en/When;", "When",
            "Lio/cucumber/java/en/Then;", "Then",
            "Lio/cucumber/java/en/And;", "And",
            "Lio/cucumber/java/en/But;", "But"
    );
    private static final String STEP_DESCRIPTION = "Lcom/jackson/cucumberdocs/StepDescription;";

    /** Creates a scanner for Cucumber step definition classes. */
    public StepScanner() {
    }

    /**
     * Scans project class directories and dependency JARs for Cucumber step definition methods.
     *
     * @param classDirectories directories containing compiled project or test classes
     * @param classpath classpath entries containing classes and dependency JARs
     * @return the discovered step definitions
     * @throws Exception if a project class file cannot be read or inspected
     */
    public List<CucumberStep> scan(List<Path> classDirectories, List<String> classpath) throws Exception {
        return scan(classDirectories, classpath, ignored -> { });
    }

    /**
     * Scans project class directories and dependency JARs for Cucumber step definitions.
     *
     * @param classDirectories directories containing compiled project or test classes
     * @param classpath classpath entries containing classes and dependency JARs
     * @param debugLog receives scan progress and files that could not be inspected
     * @return the discovered step definitions
     * @throws Exception if a project class file cannot be read or inspected
     */
    public List<CucumberStep> scan(List<Path> classDirectories, List<String> classpath,
                                   Consumer<String> debugLog) throws Exception {
        List<CucumberStep> steps = new ArrayList<>();
        Set<Path> scannedDirectories = new HashSet<>();
        Set<String> scannedClasses = new HashSet<>();

        for (Path directory : classDirectories) {
            scanDirectory(directory, steps, scannedDirectories, scannedClasses, debugLog, false);
        }

        for (String classpathEntry : classpath) {
            Path entry = Path.of(classpathEntry);
            if (Files.isDirectory(entry)) {
                scanDirectory(entry, steps, scannedDirectories, scannedClasses, debugLog, true);
            } else if (Files.isRegularFile(entry) && entry.toString().toLowerCase().endsWith(".jar")) {
                scanJar(entry, steps, scannedClasses, debugLog);
            }
        }

        return steps;
    }

    private void scanDirectory(Path directory, List<CucumberStep> steps, Set<Path> scannedDirectories,
                               Set<String> scannedClasses, Consumer<String> debugLog,
                               boolean dependencyDirectory) throws Exception {
        Path normalizedDirectory = directory.toAbsolutePath().normalize();
        if (!scannedDirectories.add(normalizedDirectory)) {
            return;
        }
        if (!Files.isDirectory(normalizedDirectory)) {
            debugLog.accept("Class directory does not exist: " + normalizedDirectory);
            return;
        }

        int[] classCount = {0};
        try (Stream<Path> files = Files.walk(normalizedDirectory)) {
            for (Path classFile : files.filter(path -> path.toString().endsWith(".class"))
                    .sorted(Comparator.naturalOrder()).toList()) {
                String className = toClassName(normalizedDirectory, classFile);
                if (!scannedClasses.add(className)) {
                    continue;
                }
                try (InputStream input = Files.newInputStream(classFile)) {
                    readClass(input, className, steps);
                    classCount[0]++;
                } catch (Exception | LinkageError e) {
                    if (!dependencyDirectory) {
                        throw e;
                    }
                    debugLog.accept("Skipping dependency class " + className + " from " + normalizedDirectory
                            + ": " + e);
                }
            }
        }
        debugLog.accept("Inspected " + classCount[0] + " class files from " + normalizedDirectory);
    }

    private void scanJar(Path jarPath, List<CucumberStep> steps, Set<String> scannedClasses,
                         Consumer<String> debugLog) {
        int[] classCount = {0};
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.stream().filter(this::isScannableClass)
                    .sorted(Comparator.comparing(JarEntry::getName)).toList();
            for (JarEntry entry : entries) {
                String className = toClassName(entry.getName());
                if (!scannedClasses.add(className)) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    readClass(input, className, steps);
                    classCount[0]++;
                } catch (Exception | LinkageError e) {
                    debugLog.accept("Skipping dependency class " + className + " from " + jarPath + ": " + e);
                }
            }
            debugLog.accept("Inspected " + classCount[0] + " class files from dependency JAR " + jarPath);
        } catch (Exception e) {
            debugLog.accept("Could not inspect dependency JAR " + jarPath + ": " + e);
        }
    }

    private void readClass(InputStream input, String classNameHint, List<CucumberStep> steps) throws Exception {
        String[] className = {classNameHint};

        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                className[0] = name.replace('/', '.');
            }

            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor,
                                             String signature, String[] exceptions) {
                List<StepAnnotation> methodSteps = new ArrayList<>();
                String[] description = {""};

                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                        String keyword = STEP_ANNOTATIONS.get(annotationDescriptor);
                        boolean isDescription = STEP_DESCRIPTION.equals(annotationDescriptor);
                        if (keyword == null && !isDescription) {
                            return null;
                        }

                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override
                            public void visit(String name, Object value) {
                                if (!(value instanceof String text) || !"value".equals(name)) {
                                    return;
                                }
                                if (isDescription) {
                                    description[0] = text;
                                } else {
                                    methodSteps.add(new StepAnnotation(keyword, text));
                                }
                            }
                        };
                    }

                    @Override
                    public void visitEnd() {
                        for (StepAnnotation step : methodSteps) {
                            steps.add(new CucumberStep(step.keyword(), step.expression(), description[0],
                                    className[0], methodName));
                        }
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private boolean isScannableClass(JarEntry entry) {
        String name = entry.getName();
        return !entry.isDirectory()
                && name.endsWith(".class")
                && !name.startsWith("META-INF/")
                && !name.equals("module-info.class")
                && !name.endsWith("/module-info.class")
                && !name.endsWith("package-info.class");
    }

    private String toClassName(Path root, Path classFile) {
        String relative = root.relativize(classFile).toString();
        return relative.substring(0, relative.length() - ".class".length()).replace('/', '.').replace('\\', '.');
    }

    private String toClassName(String classFile) {
        return classFile.substring(0, classFile.length() - ".class".length()).replace('/', '.');
    }

    private record StepAnnotation(String keyword, String expression) {
    }
}

package com.jackson.cucumberdocs;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private static final String STEP_DESCRIPTION_ARGUMENT = "Lcom/jackson/cucumberdocs/StepDescription$Argument;";
    private static final String NOT_NULL = "/NotNull;";
    private static final String NOT_BLANK = "/NotBlank;";

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
        Map<String, List<CucumberInput>> inputFields = new HashMap<>();
        Map<String, List<String>> enumValues = new HashMap<>();
        Set<Path> scannedDirectories = new HashSet<>();
        Set<String> scannedClasses = new HashSet<>();

        for (Path directory : classDirectories) {
            scanDirectory(directory, steps, inputFields, enumValues,
                    scannedDirectories, scannedClasses, debugLog, false);
        }

        for (String classpathEntry : classpath) {
            Path entry = Path.of(classpathEntry);
            if (Files.isDirectory(entry)) {
                scanDirectory(entry, steps, inputFields, enumValues,
                        scannedDirectories, scannedClasses, debugLog, true);
            } else if (Files.isRegularFile(entry) && entry.toString().toLowerCase().endsWith(".jar")) {
                scanJar(entry, steps, inputFields, enumValues, scannedClasses, debugLog);
            }
        }

        return steps.stream()
                .map(step -> new CucumberStep(step.keyword(), step.expression(), step.description(),
                        step.className(), step.methodName(), step.inputClassName(),
                        inputFields.getOrDefault(step.inputClassName(), List.of()),
                        step.arguments().stream()
                                .map(argument -> new CucumberArgument(argument.key(), argument.className(),
                                        enumValues.containsKey(argument.className()),
                                        enumValues.getOrDefault(argument.className(), List.of())))
                                .toList()))
                .toList();
    }

    private void scanDirectory(Path directory, List<CucumberStep> steps,
                               Map<String, List<CucumberInput>> inputFields,
                               Map<String, List<String>> enumValues,
                               Set<Path> scannedDirectories,
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
                    readClass(input, className, steps, inputFields, enumValues);
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

    private void scanJar(Path jarPath, List<CucumberStep> steps,
                         Map<String, List<CucumberInput>> inputFields,
                         Map<String, List<String>> enumValues, Set<String> scannedClasses,
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
                    readClass(input, className, steps, inputFields, enumValues);
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

    private void readClass(InputStream input, String classNameHint, List<CucumberStep> steps,
                           Map<String, List<CucumberInput>> inputFields,
                           Map<String, List<String>> enumValues) throws Exception {
        String[] className = {classNameHint};
        boolean[] enumClass = {false};
        List<CucumberInput> fields = new ArrayList<>();
        List<String> constants = new ArrayList<>();

        new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                className[0] = name.replace('/', '.');
                enumClass[0] = (access & Opcodes.ACC_ENUM) != 0;
            }

            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String fieldName, String descriptor,
                                                              String signature, Object value) {
                if (enumClass[0] && (access & Opcodes.ACC_ENUM) != 0) {
                    constants.add(fieldName);
                    return null;
                }
                if ((access & (Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC)) != 0) {
                    return null;
                }
                boolean[] required = {false};
                return new org.objectweb.asm.FieldVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                        markRequired(annotationDescriptor, required);
                        return null;
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
                                                                 String annotationDescriptor, boolean visible) {
                        markRequired(annotationDescriptor, required);
                        return null;
                    }

                    @Override
                    public void visitEnd() {
                        fields.add(new CucumberInput(fieldName, Type.getType(descriptor).getClassName(), required[0]));
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor,
                                             String signature, String[] exceptions) {
                List<StepAnnotation> methodSteps = new ArrayList<>();
                String[] description = {""};
                String[] inputClassName = {""};
                List<CucumberArgument> documentedArguments = new ArrayList<>();

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
                                if (isDescription && "input".equals(name) && value instanceof Type inputType) {
                                    inputClassName[0] = inputType.getClassName();
                                    return;
                                }
                                if (!(value instanceof String text) || !"value".equals(name)) {
                                    return;
                                }
                                if (isDescription) {
                                    description[0] = text;
                                } else {
                                    methodSteps.add(new StepAnnotation(keyword, text));
                                }
                            }

                            @Override
                            public AnnotationVisitor visitArray(String name) {
                                if (!isDescription || !"arguments".equals(name)) {
                                    return null;
                                }
                                return new AnnotationVisitor(Opcodes.ASM9) {
                                    @Override
                                    public AnnotationVisitor visitAnnotation(String ignored, String descriptor) {
                                        if (!STEP_DESCRIPTION_ARGUMENT.equals(descriptor)) {
                                            return null;
                                        }
                                        String[] key = {""};
                                        String[] className = {""};
                                        return new AnnotationVisitor(Opcodes.ASM9) {
                                            @Override
                                            public void visit(String name, Object value) {
                                                if ("key".equals(name) && value instanceof String text) {
                                                    key[0] = text;
                                                } else if ("type".equals(name) && value instanceof Type type) {
                                                    className[0] = type.getClassName();
                                                }
                                            }

                                            @Override
                                            public void visitEnd() {
                                                documentedArguments.add(new CucumberArgument(
                                                        key[0], className[0], false, List.of()));
                                            }
                                        };
                                    }
                                };
                            }
                        };
                    }

                    @Override
                    public void visitEnd() {
                        for (StepAnnotation step : methodSteps) {
                            steps.add(new CucumberStep(step.keyword(), step.expression(), description[0],
                                    className[0], methodName, inputClassName[0], List.of(), documentedArguments));
                        }
                    }
                };
            }

            @Override
            public void visitEnd() {
                inputFields.put(className[0], List.copyOf(fields));
                if (enumClass[0]) {
                    enumValues.put(className[0], List.copyOf(constants));
                }
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private void markRequired(String annotationDescriptor, boolean[] required) {
        if (annotationDescriptor.endsWith(NOT_NULL) || annotationDescriptor.endsWith(NOT_BLANK)) {
            required[0] = true;
        }
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

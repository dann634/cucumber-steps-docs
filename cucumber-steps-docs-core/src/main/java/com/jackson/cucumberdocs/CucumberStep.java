package com.jackson.cucumberdocs;

/**
 * A Cucumber step definition and the information needed to document it.
 *
 * @param keyword the step keyword, such as {@code Given}, {@code When}, or {@code Then}
 * @param expression the Cucumber expression declared by the step definition
 * @param description the optional description supplied with {@link StepDescription}
 * @param className the fully qualified name of the step definition class
 * @param methodName the name of the step definition method
 */
public record CucumberStep(
        String keyword,
        String expression,
        String description,
        String className,
        String methodName
) {
}

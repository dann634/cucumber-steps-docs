package com.jackson.cucumberdocs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds tester-facing documentation to a Cucumber step definition.
 *
 * <p>Place this annotation on the same method as a Cucumber {@code Given},
 * {@code When}, {@code Then}, {@code And}, or {@code But} annotation. The value
 * is displayed alongside the step expression in the generated HTML reference.</p>
 *
 * @see CucumberStep
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StepDescription {

    /**
     * Returns the human-readable description of the step.
     *
     * @return the description to display in the generated documentation
     */
    String value();

}

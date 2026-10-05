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

    /**
     * Returns the class whose fields describe the values used as input by this step.
     * Fields annotated with {@code NotNull} or {@code NotBlank} are documented as required.
     *
     * @return the input class, or {@link Void} when the step has no documented input class
     */
    Class<?> input() default Void.class;

    /**
     * Returns the documented values for step arguments, in the same order as their
     * parameter placeholders appear in the Cucumber expression. Each key is
     * used in the generated documentation in place of the corresponding placeholder.
     *
     * @return the named step arguments to document
     */
    Argument[] arguments() default {};

    /** A named step argument and the Java type used to document its value. */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Argument {

        /**
         * Returns the short name displayed in place of a parameter placeholder.
         *
         * @return the argument key
         */
        String key();

        /**
         * Returns the argument's Java type. Enum types are expanded to their constants.
         *
         * @return the argument type
         */
        Class<?> type();
    }

}

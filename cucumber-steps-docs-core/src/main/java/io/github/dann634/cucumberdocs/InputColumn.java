package io.github.dann634.cucumberdocs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the data-table column name used for an input field in generated documentation.
 *
 * <p>Place this annotation on a field in the class supplied to
 * {@link StepDescription#input()}.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InputColumn {

    /**
     * Returns the data-table column name to display for the field.
     *
     * @return the documented column name
     */
    String value();
}

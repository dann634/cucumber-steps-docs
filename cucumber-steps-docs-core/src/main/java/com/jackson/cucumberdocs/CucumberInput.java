package com.jackson.cucumberdocs;

/**
 * A field on a class declared as the input to a Cucumber step.
 *
 * @param name the field name
 * @param type the Java type of the field
 * @param required whether the field has a {@code NotNull} or {@code NotBlank} annotation
 */
public record CucumberInput(String name, String type, boolean required) {
}

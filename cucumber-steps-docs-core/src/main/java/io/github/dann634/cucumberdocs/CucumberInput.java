package io.github.dann634.cucumberdocs;

/**
 * A field on a class declared as the input to a Cucumber step.
 *
 * @param name the Java field name
 * @param columnName the documented data-table column name, or an empty string when not specified
 * @param type the Java type of the field
 * @param required whether the field has a {@code NotNull} or {@code NotBlank} annotation
 */
public record CucumberInput(String name, String columnName, String type, boolean required) {

    /**
     * Creates an input field without a documented data-table column name.
     *
     * @param name the Java field name
     * @param type the Java type of the field
     * @param required whether the field has a {@code NotNull} or {@code NotBlank} annotation
     */
    public CucumberInput(String name, String type, boolean required) {
        this(name, "", type, required);
    }
}

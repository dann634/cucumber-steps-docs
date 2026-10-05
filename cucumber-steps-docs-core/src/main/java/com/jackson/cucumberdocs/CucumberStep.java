package com.jackson.cucumberdocs;

import java.util.List;

/**
 * A Cucumber step definition and the information needed to document it.
 *
 * @param keyword the step keyword, such as {@code Given}, {@code When}, or {@code Then}
 * @param expression the Cucumber expression declared by the step definition
 * @param description the optional description supplied with {@link StepDescription}
 * @param className the fully qualified name of the step definition class
 * @param methodName the name of the step definition method
 * @param inputClassName the fully qualified name of the declared input class, if any
 * @param inputs the fields discovered on the declared input class
 * @param arguments named step arguments and their documented types
 */
public record CucumberStep(
        String keyword,
        String expression,
        String description,
        String className,
        String methodName,
        String inputClassName,
        List<CucumberInput> inputs,
        List<CucumberArgument> arguments
) {

    public CucumberStep {
        inputClassName = inputClassName == null ? "" : inputClassName;
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }

    /**
     * Creates a step without declared input documentation.
     *
     * @param keyword the step keyword
     * @param expression the Cucumber expression
     * @param description the optional step description
     * @param className the fully qualified step definition class name
     * @param methodName the step definition method name
     */
    public CucumberStep(String keyword, String expression, String description,
                        String className, String methodName) {
        this(keyword, expression, description, className, methodName, "", List.of(), List.of());
    }

    /**
     * Creates a step with input fields but no named step argument documentation.
     *
     * @param keyword the step keyword
     * @param expression the Cucumber expression
     * @param description the optional step description
     * @param className the fully qualified step definition class name
     * @param methodName the step definition method name
     * @param inputClassName the fully qualified input class name
     * @param inputs the input fields
     */
    public CucumberStep(String keyword, String expression, String description,
                        String className, String methodName, String inputClassName,
                        List<CucumberInput> inputs) {
        this(keyword, expression, description, className, methodName, inputClassName, inputs, List.of());
    }

    /**
     * Returns the expression with documented keys substituted for successive
     * {@code {string}} placeholders.
     *
     * @return the display expression for the generated documentation
     */
    public String documentedExpression() {
        String result = expression;
        int searchFrom = 0;
        for (CucumberArgument argument : arguments) {
            int placeholder = result.indexOf("{string}", searchFrom);
            if (placeholder < 0) {
                break;
            }
            String replacement = "{" + argument.key() + "}";
            result = result.substring(0, placeholder) + replacement
                    + result.substring(placeholder + "{string}".length());
            searchFrom = placeholder + replacement.length();
        }
        return result;
    }
}

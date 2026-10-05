package com.jackson.cucumberdocs;

import java.util.List;

/**
 * A named step argument and its documented Java type and enum values.
 *
 * @param key the key displayed in place of the matching {@code {string}} placeholder
 * @param className the fully qualified Java type name
 * @param enumType whether the Java type is an enum
 * @param values the enum constants, in declaration order
 */
public record CucumberArgument(String key, String className, boolean enumType, List<String> values) {

    public CucumberArgument {
        values = values == null ? List.of() : List.copyOf(values);
    }
}

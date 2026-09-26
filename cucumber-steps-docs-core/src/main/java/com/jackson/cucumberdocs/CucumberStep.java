package com.jackson.cucumberdocs;

public record CucumberStep(
        String keyword,
        String expression,
        String description,
        String className,
        String methodName
) {
}

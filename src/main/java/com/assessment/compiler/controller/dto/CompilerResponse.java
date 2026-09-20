package com.assessment.compiler.controller.dto;

import java.util.List;

public record CompilerResponse(
    boolean success,
    String output,
    String error,
    List<TestResult> testResults
) {
    public record TestResult(
        String input,
        String expectedOutput,
        String actualOutput,
        boolean passed
    ) {}
}

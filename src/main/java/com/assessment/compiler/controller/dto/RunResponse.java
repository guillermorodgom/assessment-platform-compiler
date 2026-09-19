package com.assessment.compiler.controller.dto;

public record RunResponse(
    boolean success,
    String stdout,
    String stderr,
    String compileOutput,
    String status
) {}

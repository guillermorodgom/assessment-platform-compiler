package com.assessment.compiler.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record RunRequest(
    @NotBlank(message = "El codigo fuente es obligatorio")
    String sourceCode,

    @NotBlank(message = "El lenguaje es obligatorio")
    String language,

    String stdin
) {}

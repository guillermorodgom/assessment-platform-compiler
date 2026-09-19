package com.assessment.compiler.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompilerRequest(
    @NotBlank(message = "El codigo fuente es obligatorio")
    String sourceCode,

    @NotBlank(message = "El lenguaje es obligatorio")
    String language,

    @NotEmpty(message = "Debe incluir al menos un caso de prueba")
    List<@Valid TestCaseDto> testCases
) {
    public record TestCaseDto(
        String input,
        @NotBlank(message = "El output esperado es obligatorio")
        String expectedOutput
    ) {}
}

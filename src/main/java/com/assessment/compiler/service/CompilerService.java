package com.assessment.compiler.service;

import com.assessment.compiler.controller.dto.*;
import com.assessment.compiler.service.Judge0Client.Judge0Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilerService {

    private final Judge0Client judge0Client;

    public CompilerResponse execute(CompilerRequest request) {
        log.info("[EJECUTAR] Ejecutando codigo — language: {}, testCases: {}", request.language(), request.testCases().size());

        List<CompilerResponse.TestResult> testResults = new ArrayList<>();
        boolean allPassed = true;
        String lastError = null;

        for (CompilerRequest.TestCaseDto testCase : request.testCases()) {
            Judge0Result result = judge0Client.submit(request.sourceCode(), request.language(), testCase.input());

            if (result.isCompilationError()) {
                log.warn("[EJECUTAR] Error de compilacion — language: {}", request.language());
                return new CompilerResponse(
                    false,
                    null,
                    result.compileOutput() != null ? result.compileOutput() : "Error de compilacion",
                    List.of()
                );
            }

            String actualOutput = result.stdout() != null ? result.stdout().trim() : "";
            String expectedOutput = testCase.expectedOutput() != null ? testCase.expectedOutput().trim() : "";
            boolean passed = actualOutput.equals(expectedOutput);

            if (!passed) {
                allPassed = false;
            }

            if (result.stderr() != null && !result.stderr().isBlank()) {
                lastError = result.stderr();
            }

            testResults.add(new CompilerResponse.TestResult(
                testCase.input(),
                testCase.expectedOutput(),
                result.stdout(),
                passed
            ));
        }

        int passedCount = (int) testResults.stream().filter(CompilerResponse.TestResult::passed).count();
        log.info("[EJECUTAR] Resultado — success: {}, testsPassed: {}/{}", allPassed, passedCount, testResults.size());

        return new CompilerResponse(allPassed, null, lastError, testResults);
    }

    public RunResponse run(RunRequest request) {
        log.info("[EJECUTAR] Ejecucion libre — language: {}", request.language());

        Judge0Result result = judge0Client.submit(request.sourceCode(), request.language(), request.stdin());

        boolean success = result.isAccepted();
        log.info("[EJECUTAR] Resultado libre — success: {}, status: {}", success, result.statusDescription());

        return new RunResponse(
            success,
            result.stdout(),
            result.stderr(),
            result.compileOutput(),
            result.statusDescription()
        );
    }

    public List<LanguageResponse> getSupportedLanguages() {
        return List.of(
            new LanguageResponse(62, "Java"),
            new LanguageResponse(63, "JavaScript"),
            new LanguageResponse(71, "Python")
        );
    }
}

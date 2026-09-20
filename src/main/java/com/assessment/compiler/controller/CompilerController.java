package com.assessment.compiler.controller;

import com.assessment.compiler.controller.dto.*;
import com.assessment.compiler.service.CompilerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compiler")
@RequiredArgsConstructor
@Slf4j
public class CompilerController {

    private final CompilerService compilerService;

    @PostMapping("/execute")
    public ResponseEntity<CompilerResponse> execute(@Valid @RequestBody CompilerRequest request) {
        log.info("[REQUEST] POST /api/compiler/execute — language: {}, testCases: {}", request.language(), request.testCases().size());
        CompilerResponse response = compilerService.execute(request);
        log.info("[RESPONSE] POST /api/compiler/execute — success: {}", response.success());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/run")
    public ResponseEntity<RunResponse> run(@Valid @RequestBody RunRequest request) {
        log.info("[REQUEST] POST /api/compiler/run — language: {}", request.language());
        RunResponse response = compilerService.run(request);
        log.info("[RESPONSE] POST /api/compiler/run — success: {}", response.success());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/languages")
    public ResponseEntity<List<LanguageResponse>> getLanguages() {
        log.info("[REQUEST] GET /api/compiler/languages");
        return ResponseEntity.ok(compilerService.getSupportedLanguages());
    }
}

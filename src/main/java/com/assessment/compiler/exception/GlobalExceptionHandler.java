package com.assessment.compiler.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnsupportedLanguageException.class)
    public ResponseEntity<ApiError> handleUnsupportedLanguage(UnsupportedLanguageException ex) {
        log.warn("[ERROR] Lenguaje no soportado — {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiError(400, "UNSUPPORTED_LANGUAGE", ex.getMessage()));
    }

    @ExceptionHandler(CompilerException.class)
    public ResponseEntity<ApiError> handleCompiler(CompilerException ex) {
        log.error("[ERROR] Error de compilador — {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(new ApiError(502, "COMPILER_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        log.warn("[ERROR] Validacion fallida — campos: {}", errors.keySet());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.builder()
                .status(400)
                .code("VALIDATION_ERROR")
                .message("Errores de validacion")
                .details(errors)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        log.error("[ERROR] Error interno del servidor", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError(500, "INTERNAL_ERROR", "Error interno del servidor"));
    }
}

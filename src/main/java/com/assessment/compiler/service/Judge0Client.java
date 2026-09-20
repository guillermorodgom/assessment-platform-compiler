package com.assessment.compiler.service;

import com.assessment.compiler.exception.CompilerException;
import com.assessment.compiler.exception.UnsupportedLanguageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class Judge0Client {

    private final RestTemplate restTemplate;

    @Value("${judge0.url}")
    private String judge0Url;

    @Value("${judge0.api-key:}")
    private String apiKey;

    public Judge0Result submit(String sourceCode, String language, String stdin) {
        int languageId = mapLanguageId(language);
        log.info("[JUDGE0] Enviando submission — languageId: {}", languageId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("X-RapidAPI-Key", apiKey);
            headers.set("X-RapidAPI-Host", "judge0-ce.p.rapidapi.com");
        }

        Map<String, Object> body = Map.of(
            "source_code", sourceCode,
            "language_id", languageId,
            "stdin", stdin != null ? stdin.replace("\\n", "\n").replace("\\t", "\t") : ""
        );

        String url = judge0Url + "/submissions?base64_encoded=false&wait=true";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response == null) {
                throw new CompilerException("Respuesta nula de Judge0");
            }

            String stdout = getString(response, "stdout");
            String stderr = getString(response, "stderr");
            String compileOutput = getString(response, "compile_output");

            @SuppressWarnings("unchecked")
            Map<String, Object> status = (Map<String, Object>) response.get("status");
            int statusId = status != null ? (int) status.get("id") : 0;
            String statusDescription = status != null ? (String) status.get("description") : "Unknown";

            log.info("[JUDGE0] Respuesta recibida — status: {} ({})", statusId, statusDescription);

            return new Judge0Result(stdout, stderr, compileOutput, statusId, statusDescription);
        } catch (RestClientException e) {
            log.error("[JUDGE0] Error de comunicacion con Judge0", e);
            throw new CompilerException("Error de comunicacion con Judge0: " + e.getMessage(), e);
        }
    }

    public int mapLanguageId(String language) {
        return switch (language.toLowerCase().trim()) {
            case "java" -> 62;
            case "javascript", "js" -> 63;
            case "python", "python3" -> 71;
            default -> throw new UnsupportedLanguageException(language);
        };
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    public record Judge0Result(
        String stdout,
        String stderr,
        String compileOutput,
        int statusId,
        String statusDescription
    ) {
        public boolean isAccepted() {
            return statusId == 3;
        }

        public boolean isCompilationError() {
            return statusId == 6;
        }
    }
}

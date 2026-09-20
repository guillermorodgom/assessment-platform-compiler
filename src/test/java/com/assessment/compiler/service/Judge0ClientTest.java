package com.assessment.compiler.service;

import com.assessment.compiler.exception.CompilerException;
import com.assessment.compiler.exception.UnsupportedLanguageException;
import com.assessment.compiler.service.Judge0Client.Judge0Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Judge0ClientTest {

    @Mock
    private RestTemplate restTemplate;

    private Judge0Client judge0Client;

    @BeforeEach
    void setUp() {
        judge0Client = new Judge0Client(restTemplate);
        ReflectionTestUtils.setField(judge0Client, "judge0Url", "https://judge0.test");
        ReflectionTestUtils.setField(judge0Client, "apiKey", "");
    }

    // --- mapLanguageId() ---

    @ParameterizedTest
    @CsvSource({"java,62", "javascript,63", "js,63", "python,71", "python3,71", "JAVA,62", "JavaScript,63"})
    void mapLanguageId_validLanguages(String language, int expectedId) {
        assertThat(judge0Client.mapLanguageId(language)).isEqualTo(expectedId);
    }

    @Test
    void mapLanguageId_unsupported_throwsException() {
        assertThatThrownBy(() -> judge0Client.mapLanguageId("ruby"))
            .isInstanceOf(UnsupportedLanguageException.class)
            .hasMessageContaining("ruby");
    }

    // --- submit() ---

    @Test
    void submit_successfulExecution_returnsResult() {
        Map<String, Object> status = Map.of("id", 3, "description", "Accepted");
        Map<String, Object> response = Map.of(
            "stdout", "hello",
            "stderr", "",
            "compile_output", "",
            "status", status
        );
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(response);

        Judge0Result result = judge0Client.submit("code", "java", "input");

        assertThat(result.stdout()).isEqualTo("hello");
        assertThat(result.statusId()).isEqualTo(3);
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.isCompilationError()).isFalse();
    }

    @Test
    void submit_compilationError_returnsStatusId6() {
        Map<String, Object> status = Map.of("id", 6, "description", "Compilation Error");
        Map<String, Object> response = Map.of(
            "compile_output", "error at line 1",
            "status", status
        );
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(response);

        Judge0Result result = judge0Client.submit("bad", "java", "");

        assertThat(result.isCompilationError()).isTrue();
        assertThat(result.compileOutput()).isEqualTo("error at line 1");
    }

    @Test
    void submit_nullResponse_throwsCompilerException() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> judge0Client.submit("code", "java", ""))
            .isInstanceOf(CompilerException.class)
            .hasMessageContaining("Respuesta nula");
    }

    @Test
    void submit_restClientException_throwsCompilerException() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> judge0Client.submit("code", "java", ""))
            .isInstanceOf(CompilerException.class)
            .hasMessageContaining("Error de comunicacion")
            .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void submit_withApiKey_setsRapidApiHeaders() {
        ReflectionTestUtils.setField(judge0Client, "apiKey", "my-key");
        Map<String, Object> status = Map.of("id", 3, "description", "Accepted");
        Map<String, Object> response = Map.of("status", status);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForObject(anyString(), captor.capture(), eq(Map.class)))
            .thenReturn(response);

        judge0Client.submit("code", "java", "");

        HttpEntity<?> entity = captor.getValue();
        assertThat(entity.getHeaders().getFirst("X-RapidAPI-Key")).isEqualTo("my-key");
        assertThat(entity.getHeaders().getFirst("X-RapidAPI-Host")).isEqualTo("judge0-ce.p.rapidapi.com");
    }

    @Test
    void submit_withoutApiKey_noRapidApiHeaders() {
        Map<String, Object> status = Map.of("id", 3, "description", "Accepted");
        Map<String, Object> response = Map.of("status", status);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForObject(anyString(), captor.capture(), eq(Map.class)))
            .thenReturn(response);

        judge0Client.submit("code", "java", "");

        HttpEntity<?> entity = captor.getValue();
        assertThat(entity.getHeaders().containsKey("X-RapidAPI-Key")).isFalse();
    }

    @Test
    void submit_nullStdin_sendsEmptyString() {
        Map<String, Object> status = Map.of("id", 3, "description", "Accepted");
        Map<String, Object> response = Map.of("status", status);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForObject(anyString(), captor.capture(), eq(Map.class)))
            .thenReturn(response);

        judge0Client.submit("code", "java", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body.get("stdin")).isEqualTo("");
    }

    @Test
    void submit_stdinWithEscapedNewlines_replacedWithActual() {
        Map<String, Object> status = Map.of("id", 3, "description", "Accepted");
        Map<String, Object> response = Map.of("status", status);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.postForObject(anyString(), captor.capture(), eq(Map.class)))
            .thenReturn(response);

        judge0Client.submit("code", "java", "line1\\nline2");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body.get("stdin")).isEqualTo("line1\nline2");
    }

    @Test
    void submit_responseWithNullStatus_handlesGracefully() {
        Map<String, Object> response = Map.of("stdout", "hi");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(response);

        Judge0Result result = judge0Client.submit("code", "java", "");

        assertThat(result.statusId()).isEqualTo(0);
        assertThat(result.statusDescription()).isEqualTo("Unknown");
    }

    // --- Judge0Result record ---

    @Test
    void judge0Result_isAccepted_onlyForStatusId3() {
        assertThat(new Judge0Result(null, null, null, 3, "Accepted").isAccepted()).isTrue();
        assertThat(new Judge0Result(null, null, null, 6, "CE").isAccepted()).isFalse();
        assertThat(new Judge0Result(null, null, null, 11, "RE").isAccepted()).isFalse();
    }

    @Test
    void judge0Result_isCompilationError_onlyForStatusId6() {
        assertThat(new Judge0Result(null, null, null, 6, "CE").isCompilationError()).isTrue();
        assertThat(new Judge0Result(null, null, null, 3, "OK").isCompilationError()).isFalse();
    }
}

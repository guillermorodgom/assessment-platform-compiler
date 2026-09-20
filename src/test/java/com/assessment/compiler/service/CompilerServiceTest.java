package com.assessment.compiler.service;

import com.assessment.compiler.controller.dto.*;
import com.assessment.compiler.service.Judge0Client.Judge0Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilerServiceTest {

    @Mock
    private Judge0Client judge0Client;

    @InjectMocks
    private CompilerService compilerService;

    // --- execute() ---

    @Test
    void execute_allTestsPass_returnsSuccess() {
        var testCase = new CompilerRequest.TestCaseDto("5", "25");
        var request = new CompilerRequest("code", "java", List.of(testCase));
        when(judge0Client.submit("code", "java", "5"))
            .thenReturn(new Judge0Result("25", null, null, 3, "Accepted"));

        CompilerResponse response = compilerService.execute(request);

        assertThat(response.success()).isTrue();
        assertThat(response.testResults()).hasSize(1);
        assertThat(response.testResults().get(0).passed()).isTrue();
        assertThat(response.error()).isNull();
    }

    @Test
    void execute_testFails_returnsFailure() {
        var testCase = new CompilerRequest.TestCaseDto("5", "25");
        var request = new CompilerRequest("code", "java", List.of(testCase));
        when(judge0Client.submit("code", "java", "5"))
            .thenReturn(new Judge0Result("30", null, null, 3, "Accepted"));

        CompilerResponse response = compilerService.execute(request);

        assertThat(response.success()).isFalse();
        assertThat(response.testResults().get(0).passed()).isFalse();
        assertThat(response.testResults().get(0).actualOutput()).isEqualTo("30");
    }

    @Test
    void execute_compilationError_returnsEarlyWithError() {
        var tc1 = new CompilerRequest.TestCaseDto("1", "1");
        var tc2 = new CompilerRequest.TestCaseDto("2", "2");
        var request = new CompilerRequest("bad code", "java", List.of(tc1, tc2));
        when(judge0Client.submit("bad code", "java", "1"))
            .thenReturn(new Judge0Result(null, null, "Main.java:1: error", 6, "Compilation Error"));

        CompilerResponse response = compilerService.execute(request);

        assertThat(response.success()).isFalse();
        assertThat(response.error()).isEqualTo("Main.java:1: error");
        assertThat(response.testResults()).isEmpty();
        verify(judge0Client, times(1)).submit(anyString(), anyString(), anyString());
    }

    @Test
    void execute_compilationError_nullCompileOutput_returnsDefaultMessage() {
        var request = new CompilerRequest("bad", "java", List.of(new CompilerRequest.TestCaseDto("1", "1")));
        when(judge0Client.submit(anyString(), anyString(), anyString()))
            .thenReturn(new Judge0Result(null, null, null, 6, "Compilation Error"));

        CompilerResponse response = compilerService.execute(request);

        assertThat(response.error()).isEqualTo("Error de compilacion");
    }

    @Test
    void execute_multipleTests_mixedResults() {
        var tc1 = new CompilerRequest.TestCaseDto("1", "1");
        var tc2 = new CompilerRequest.TestCaseDto("2", "4");
        var request = new CompilerRequest("code", "python", List.of(tc1, tc2));
        when(judge0Client.submit("code", "python", "1"))
            .thenReturn(new Judge0Result("1", null, null, 3, "Accepted"));
        when(judge0Client.submit("code", "python", "2"))
            .thenReturn(new Judge0Result("5", null, null, 3, "Accepted"));

        CompilerResponse response = compilerService.execute(request);

        assertThat(response.success()).isFalse();
        assertThat(response.testResults()).hasSize(2);
        assertThat(response.testResults().get(0).passed()).isTrue();
        assertThat(response.testResults().get(1).passed()).isFalse();
    }

    @Test
    void execute_stdoutWithWhitespace_trimmedBeforeCompare() {
        var testCase = new CompilerRequest.TestCaseDto("1", "hello");
        var request = new CompilerRequest("code", "java", List.of(testCase));
        when(judge0Client.submit(anyString(), anyString(), anyString()))
            .thenReturn(new Judge0Result("hello\n", null, null, 3, "Accepted"));

        CompilerResponse response = compilerService.execute(request);

        assertThat(response.testResults().get(0).passed()).isTrue();
    }

    @Test
    void execute_nullStdout_treatedAsEmptyString() {
        var testCase = new CompilerRequest.TestCaseDto("1", "");
        var request = new CompilerRequest("code", "java", List.of(testCase));
        when(judge0Client.submit(anyString(), anyString(), anyString()))
            .thenReturn(new Judge0Result(null, null, null, 3, "Accepted"));

        CompilerResponse response = compilerService.execute(request);

        assertThat(response.testResults().get(0).passed()).isTrue();
    }

    @Test
    void execute_stderr_capturedAsLastError() {
        var testCase = new CompilerRequest.TestCaseDto("1", "1");
        var request = new CompilerRequest("code", "java", List.of(testCase));
        when(judge0Client.submit(anyString(), anyString(), anyString()))
            .thenReturn(new Judge0Result("1", "warning: something", null, 3, "Accepted"));

        CompilerResponse response = compilerService.execute(request);

        assertThat(response.success()).isTrue();
        assertThat(response.error()).isEqualTo("warning: something");
    }

    // --- run() ---

    @Test
    void run_successfulExecution_returnsAccepted() {
        var request = new RunRequest("print('hi')", "python", null);
        when(judge0Client.submit("print('hi')", "python", null))
            .thenReturn(new Judge0Result("hi\n", null, null, 3, "Accepted"));

        RunResponse response = compilerService.run(request);

        assertThat(response.success()).isTrue();
        assertThat(response.stdout()).isEqualTo("hi\n");
        assertThat(response.status()).isEqualTo("Accepted");
    }

    @Test
    void run_runtimeError_returnsFailure() {
        var request = new RunRequest("bad", "python", null);
        when(judge0Client.submit("bad", "python", null))
            .thenReturn(new Judge0Result(null, "NameError", null, 11, "Runtime Error"));

        RunResponse response = compilerService.run(request);

        assertThat(response.success()).isFalse();
        assertThat(response.stderr()).isEqualTo("NameError");
        assertThat(response.status()).isEqualTo("Runtime Error");
    }

    // --- getSupportedLanguages() ---

    @Test
    void getSupportedLanguages_returnsThreeLanguages() {
        var languages = compilerService.getSupportedLanguages();

        assertThat(languages).hasSize(3);
        assertThat(languages).extracting("name").containsExactly("Java", "JavaScript", "Python");
        assertThat(languages).extracting("id").containsExactly(62, 63, 71);
    }
}

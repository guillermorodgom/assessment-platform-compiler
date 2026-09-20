package com.assessment.compiler.controller;

import com.assessment.compiler.controller.dto.*;
import com.assessment.compiler.exception.CompilerException;
import com.assessment.compiler.exception.GlobalExceptionHandler;
import com.assessment.compiler.exception.UnsupportedLanguageException;
import com.assessment.compiler.service.CompilerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompilerController.class)
@Import(GlobalExceptionHandler.class)
class CompilerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilerService compilerService;

    // --- POST /api/compiler/execute ---

    @Test
    void execute_validRequest_returns200() throws Exception {
        var response = new CompilerResponse(true, null, null,
            List.of(new CompilerResponse.TestResult("1", "1", "1", true)));
        when(compilerService.execute(any())).thenReturn(response);

        String body = """
            {"sourceCode":"code","language":"java","testCases":[{"input":"1","expectedOutput":"1"}]}
            """;

        mockMvc.perform(post("/api/compiler/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.testResults[0].passed").value(true));
    }

    @Test
    void execute_missingSourceCode_returns400() throws Exception {
        String body = """
            {"language":"java","testCases":[{"input":"1","expectedOutput":"1"}]}
            """;

        mockMvc.perform(post("/api/compiler/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void execute_emptyTestCases_returns400() throws Exception {
        String body = """
            {"sourceCode":"code","language":"java","testCases":[]}
            """;

        mockMvc.perform(post("/api/compiler/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void execute_blankExpectedOutput_returns400() throws Exception {
        String body = """
            {"sourceCode":"code","language":"java","testCases":[{"input":"1","expectedOutput":""}]}
            """;

        mockMvc.perform(post("/api/compiler/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void execute_unsupportedLanguage_returns400() throws Exception {
        when(compilerService.execute(any())).thenThrow(new UnsupportedLanguageException("ruby"));

        String body = """
            {"sourceCode":"code","language":"ruby","testCases":[{"input":"1","expectedOutput":"1"}]}
            """;

        mockMvc.perform(post("/api/compiler/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_LANGUAGE"));
    }

    @Test
    void execute_compilerError_returns502() throws Exception {
        when(compilerService.execute(any())).thenThrow(new CompilerException("Judge0 down"));

        String body = """
            {"sourceCode":"code","language":"java","testCases":[{"input":"1","expectedOutput":"1"}]}
            """;

        mockMvc.perform(post("/api/compiler/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("COMPILER_ERROR"));
    }

    // --- POST /api/compiler/run ---

    @Test
    void run_validRequest_returns200() throws Exception {
        var response = new RunResponse(true, "hello", null, null, "Accepted");
        when(compilerService.run(any())).thenReturn(response);

        String body = """
            {"sourceCode":"print('hello')","language":"python"}
            """;

        mockMvc.perform(post("/api/compiler/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.stdout").value("hello"));
    }

    @Test
    void run_missingLanguage_returns400() throws Exception {
        String body = """
            {"sourceCode":"code"}
            """;

        mockMvc.perform(post("/api/compiler/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- GET /api/compiler/languages ---

    @Test
    void getLanguages_returns200WithList() throws Exception {
        when(compilerService.getSupportedLanguages()).thenReturn(List.of(
            new LanguageResponse(62, "Java"),
            new LanguageResponse(63, "JavaScript"),
            new LanguageResponse(71, "Python")
        ));

        mockMvc.perform(get("/api/compiler/languages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("Java"));
    }

    // --- Generic error ---

    @Test
    void execute_unexpectedException_returns500() throws Exception {
        when(compilerService.execute(any())).thenThrow(new RuntimeException("unexpected"));

        String body = """
            {"sourceCode":"code","language":"java","testCases":[{"input":"1","expectedOutput":"1"}]}
            """;

        mockMvc.perform(post("/api/compiler/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}

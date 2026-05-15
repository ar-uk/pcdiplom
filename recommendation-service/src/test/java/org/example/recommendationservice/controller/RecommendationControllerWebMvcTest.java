package org.example.recommendationservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.example.recommendationservice.dto.BuildRequest;
import org.example.recommendationservice.dto.BuildResponse;
import org.example.recommendationservice.dto.RecommendationEvaluationRequest;
import org.example.recommendationservice.dto.RecommendationEvaluationResponse;
import org.example.recommendationservice.dto.ResolutionTarget;
import org.example.recommendationservice.dto.WorkloadType;
import org.example.recommendationservice.service.FreshBuildRecommendationService;
import org.example.recommendationservice.service.ManualBuildService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RecommendationController.class)
class RecommendationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FreshBuildRecommendationService buildRecommendationService;

    @MockBean
    private ManualBuildService manualBuildService;

    @Test
    void buildReturnsServicePayload() throws Exception {
        BuildResponse.RequirementsDto requirements =
                new BuildResponse.RequirementsDto(
                        new BigDecimal("500000"),
                        "gaming",
                        WorkloadType.GAMING,
                        ResolutionTarget.P1080,
                        144,
                        List.of(),
                        new BuildResponse.ConstraintsDto(null, null, false, null, false),
                        "KZ",
                        true,
                        false);

        BuildResponse.ChecksDto checks =
                new BuildResponse.ChecksDto(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        "high");

        BuildResponse.MetricsDto metrics =
                new BuildResponse.MetricsDto(50L, 1, 1, "v1", BigDecimal.ZERO, 0, "high");

        BuildResponse body =
                new BuildResponse("sess-1", requirements, "mid", List.of(), List.of(), checks, metrics, List.of());

        when(buildRecommendationService.createBuild(any(BuildRequest.class))).thenReturn(body);

        mockMvc.perform(
                        post("/api/recommendation/build")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new BuildRequest("gaming PC 500k KZT", "KZT", "KZ", true, "u1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-1"));

        verify(buildRecommendationService).createBuild(any(BuildRequest.class));
    }

    @Test
    void buildWithBlankPromptReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/recommendation/build")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new BuildRequest("   ", "KZT", "KZ", false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateDelegatesToService() throws Exception {
        when(buildRecommendationService.evaluate(any(RecommendationEvaluationRequest.class)))
                .thenReturn(
                        new RecommendationEvaluationResponse(
                                0,
                                0,
                                BigDecimal.ZERO,
                                0,
                                BigDecimal.ZERO,
                                0L,
                                0,
                                BigDecimal.ZERO,
                                List.of()));

        mockMvc.perform(
                        post("/api/recommendation/evaluate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RecommendationEvaluationRequest(
                                                        List.of("a", "b"), "KZT", "KZ", true, "u1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(0));

        verify(buildRecommendationService).evaluate(any(RecommendationEvaluationRequest.class));
    }

    @Test
    void evaluateWithEmptyPromptsReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/recommendation/evaluate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RecommendationEvaluationRequest(
                                                        List.of(), "KZT", "KZ", true, null))))
                .andExpect(status().isBadRequest());
    }
}

package org.example.recommendationservice.controller;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.example.recommendationservice.service.ComponentCompatibilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ComponentCompatibilityController.class)
class ComponentCompatibilityControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComponentCompatibilityService compatibilityService;

    @Test
    void socketCheckReturnsJson() throws Exception {
        when(compatibilityService.compatibleSocket("AM4", "AM4")).thenReturn(true);

        String body = "{\"cpu_socket\":\"AM4\",\"motherboard_socket\":\"AM4\"}";

        mockMvc.perform(post("/api/components/compatibility/socket-check").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(true));
    }

    @Test
    void validateBuildUsesService() throws Exception {
        var result = new ComponentCompatibilityService.CompatibilityCheckResult(false, List.of("socket mismatch"), List.of());
        when(compatibilityService.validateBuild(anyMap())).thenReturn(result);

        String payload =
                """
                {
                  "cpu": {"id":"1","name":"Ryzen 7600X","socket":"AM5","memory_type":"DDR5"},
                  "gpu": {"id":"2","name":"RTX 4060"},
                  "motherboard": {"id":"3","name":"B450","socket":"AM4","memory_type":"DDR4"},
                  "memory": {"id":"4","name":"16GB DDR4"},
                  "psu": {"id":"5","name":"650W","wattage":650}
                }
                """;

        mockMvc.perform(post("/api/components/compatibility/validate-build").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatible").value(false))
                .andExpect(jsonPath("$.issues[0]").value("socket mismatch"));
    }

    @Test
    void socketInfoGet() throws Exception {
        mockMvc.perform(get("/api/components/compatibility/socket-info/AM5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.socket").value("AM5"));
    }
}

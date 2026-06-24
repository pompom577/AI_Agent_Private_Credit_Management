package com.platform.gateway.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.gateway.dto.LineageResponse;
import com.platform.gateway.exceptions.GlobalExceptionHandler;
import com.platform.gateway.services.LineageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TC-BE-01: GET /api/v1/metrics/{metric_id}/lineage returns 200 with metric value,
 * source_doc_id, page_number, and the bounding box coordinate array.
 */
@WebMvcTest(LineageController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class LineageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    LineageService lineageService;

    /**
     * TC-BE-01: A validated metric with coordinates must return all lineage fields.
     * The bbox array must contain [x_min, y_min, x_max, y_max] in that order —
     * order matters because the frontend uses positional indexing to draw the overlay.
     */
    @Test
    void tcBe01_lineageEndpoint_returns200WithAllLineageFields() throws Exception {
        LineageResponse response = new LineageResponse(
                1L, "Revenue", "$1.5M", 42L, 3,
                List.of(10.0, 20.0, 100.0, 40.0));

        when(lineageService.getLineage(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/metrics/1/lineage")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric_id").value(1))
                .andExpect(jsonPath("$.raw_value").value("$1.5M"))
                .andExpect(jsonPath("$.source_doc_id").value(42))
                .andExpect(jsonPath("$.page_number").value(3))
                .andExpect(jsonPath("$.bbox[0]").value(10.0))
                .andExpect(jsonPath("$.bbox[1]").value(20.0))
                .andExpect(jsonPath("$.bbox[2]").value(100.0))
                .andExpect(jsonPath("$.bbox[3]").value(40.0));
    }

    /**
     * TC-BE-01 (not found): Requesting lineage for an unknown metric_id must return 404,
     * not a 500, so the frontend can distinguish "metric doesn't exist" from a server error.
     */
    @Test
    void tcBe01_lineageEndpoint_unknownMetricId_returns404() throws Exception {
        when(lineageService.getLineage(99L))
                .thenThrow(new NoSuchElementException("Metric not found: 99"));

        mockMvc.perform(get("/api/v1/metrics/99/lineage")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

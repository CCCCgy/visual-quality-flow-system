package com.example.visualqms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CoreFlowIntegrationTest {

    private static final long ADMIN_USER_ID = 1L;
    private static final long INSPECTOR_USER_ID = 2L;
    private static final long QUALITY_ENGINEER_USER_ID = 3L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dashboardApisShouldReturnSuccess() throws Exception {
        assertSuccess(getJson("/api/dashboard/summary"), "GET /api/dashboard/summary");
        assertSuccess(getJson("/api/dashboard/batch-status"), "GET /api/dashboard/batch-status");
        assertSuccess(getJson("/api/dashboard/detection-status"), "GET /api/dashboard/detection-status");
        assertSuccess(getJson("/api/dashboard/defect-class"), "GET /api/dashboard/defect-class");
        assertSuccess(getJson("/api/dashboard/ncr-status"), "GET /api/dashboard/ncr-status");
        assertSuccess(getJson("/api/dashboard/capa-status"), "GET /api/dashboard/capa-status");
    }

    @Test
    void completeQualityClosedLoopShouldWork() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        JsonNode batch = postJson("/api/batches", Map.of(
                "batchNo", "BATCH-TEST-" + suffix,
                "productCode", "PROD-TEST-CERAMIC",
                "productName", "Test Ceramic Surface Part",
                "plannedQuantity", 10,
                "createdBy", ADMIN_USER_ID,
                "remark", "MockMvc core flow integration test"
        ));
        long batchId = data(batch, "create batch").path("id").asLong();
        assertThat(batchId).as("created batch id").isPositive();

        JsonNode task = postJson("/api/inspection-tasks", Map.of(
                "taskNo", "TASK-TEST-" + suffix,
                "batchId", batchId,
                "modelName", "test-yolo-model",
                "modelVersion", "v1.0-test",
                "sourceType", "TEST_JSON",
                "createdBy", ADMIN_USER_ID
        ));
        long taskId = data(task, "create inspection task").path("id").asLong();
        assertThat(taskId).as("created inspection task id").isPositive();

        JsonNode importResult = postJson("/api/detections/import-json", Map.of(
                "taskId", taskId,
                "yoloJson", Map.of(
                        "sourceName", "test-surface-" + suffix + ".jpg",
                        "weightsName", "test-model.pt",
                        "visualizationName", "test-surface-" + suffix + "-pred.jpg",
                        "classNames", List.of("DS"),
                        "parameters", Map.of("conf", 0.25, "iou", 0.6, "imgsz", 800),
                        "inferenceTimeMs", 100L,
                        "boxes", List.of(Map.of(
                                "classId", 1,
                                "className", "DS",
                                "confidence", new BigDecimal("0.85"),
                                "bboxXyxy", List.of(
                                        new BigDecimal("100"),
                                        new BigDecimal("120"),
                                        new BigDecimal("240"),
                                        new BigDecimal("260")
                                )
                        ))
                )
        ));
        assertThat(data(importResult, "import YOLO JSON").path("detectionCount").asInt())
                .as("imported detection count")
                .isEqualTo(1);

        JsonNode detections = getJson("/api/detections?taskId=" + taskId);
        JsonNode records = data(detections, "query detection results").path("records");
        assertThat(records.isArray()).as("detection result records should be an array").isTrue();
        assertThat(records).as("detection result records for created task").hasSize(1);
        long detectionResultId = records.get(0).path("id").asLong();
        assertThat(detectionResultId).as("created detection result id").isPositive();

        JsonNode review = postJson("/api/reviews", Map.of(
                "detectionResultId", detectionResultId,
                "reviewerId", INSPECTOR_USER_ID,
                "reviewResult", "CONFIRMED_DEFECT",
                "reviewComment", "Confirmed by integration test"
        ));
        long reviewId = data(review, "create review").path("id").asLong();
        assertThat(reviewId).as("created review id").isPositive();

        JsonNode ncr = postJson("/api/ncrs", Map.of(
                "ncrNo", "NCR-TEST-" + suffix,
                "reviewId", reviewId,
                "severity", "HIGH",
                "description", "Integration test confirmed defect",
                "createdBy", QUALITY_ENGINEER_USER_ID
        ));
        long ncrId = data(ncr, "create NCR").path("id").asLong();
        assertThat(ncrId).as("created NCR id").isPositive();

        JsonNode capa = postJson("/api/capas", Map.of(
                "capaNo", "CAPA-TEST-" + suffix,
                "ncrId", ncrId,
                "ownerId", QUALITY_ENGINEER_USER_ID,
                "rootCause", "Integration test root cause",
                "correctiveAction", "Integration test corrective action",
                "preventiveAction", "Integration test preventive action",
                "dueDate", "2026-12-31"
        ));
        long capaId = data(capa, "create CAPA").path("id").asLong();
        assertThat(capaId).as("created CAPA id").isPositive();

        patchJson("/api/capas/" + capaId + "/status", Map.of("status", "CLOSED"));

        JsonNode finalCapa = getJson("/api/capas/" + capaId);
        JsonNode finalNcr = getJson("/api/ncrs/" + ncrId);
        JsonNode finalBatch = getJson("/api/batches/" + batchId);

        assertThat(data(finalCapa, "get final CAPA").path("status").asText())
                .as("final CAPA status")
                .isEqualTo("CLOSED");
        assertThat(data(finalNcr, "get final NCR").path("status").asText())
                .as("final NCR status")
                .isEqualTo("CLOSED");
        assertThat(data(finalBatch, "get final batch").path("status").asText())
                .as("final production batch status")
                .isEqualTo("CLOSED");
    }

    private JsonNode getJson(String url) throws Exception {
        String response = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode postJson(String url, Object requestBody) throws Exception {
        String response = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        assertSuccess(json, "POST " + url);
        return json;
    }

    private JsonNode patchJson(String url, Object requestBody) throws Exception {
        String response = mockMvc.perform(patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        assertSuccess(json, "PATCH " + url);
        return json;
    }

    private void assertSuccess(JsonNode json, String step) {
        assertThat(json.path("code").asInt())
                .as(step + " should return Result.code=200, response=" + json)
                .isEqualTo(200);
    }

    private JsonNode data(JsonNode json, String step) {
        assertSuccess(json, step);
        JsonNode data = json.path("data");
        assertThat(data.isMissingNode() || data.isNull())
                .as(step + " should return non-null data, response=" + json)
                .isFalse();
        return data;
    }
}

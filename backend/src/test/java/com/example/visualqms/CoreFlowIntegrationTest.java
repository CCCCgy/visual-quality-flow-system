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

/**
 * 文件职责：
 * 用真实 Spring Boot 上下文验证核心接口和质量闭环链路。
 *
 * 所属层级：
 * Test。
 *
 * 上游调用：
 * Maven Surefire 在执行 mvn test 时运行本测试。
 *
 * 下游依赖：
 * @SpringBootTest 会加载真实 Controller、Service、Mapper 和 application.yml；
 * MockMvc 不启动真实浏览器，但会通过 Spring MVC 调用接口；
 * 测试依赖可访问的 visual_qms MySQL，MYSQL_PASSWORD 可通过环境变量传入。
 *
 * 主要业务链路：
 * MockMvc -> Controller -> ServiceImpl -> Mapper -> MySQL。
 *
 * 注意事项：
 * 测试不使用固定自增 ID，而是用当前时间戳生成唯一 batchNo/taskNo/NCR/CAPA 编号，
 * 避免与已有样例数据或历史测试数据冲突。
 */
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

    /**
     * 验证 Dashboard 接口均返回业务成功。
     *
     * 覆盖接口：
     * /summary、/batch-status、/detection-status、/defect-class、/ncr-status、/capa-status。
     *
     * 断言说明：
     * HTTP 200 只能证明请求被 Spring MVC 正常处理；
     * 还必须断言统一 Result.code=200，才能证明业务层没有返回失败结果。
     */
    @Test
    void dashboardApisShouldReturnSuccess() throws Exception {
        assertSuccess(getJson("/api/dashboard/summary"), "GET /api/dashboard/summary");
        assertSuccess(getJson("/api/dashboard/batch-status"), "GET /api/dashboard/batch-status");
        assertSuccess(getJson("/api/dashboard/detection-status"), "GET /api/dashboard/detection-status");
        assertSuccess(getJson("/api/dashboard/defect-class"), "GET /api/dashboard/defect-class");
        assertSuccess(getJson("/api/dashboard/ncr-status"), "GET /api/dashboard/ncr-status");
        assertSuccess(getJson("/api/dashboard/capa-status"), "GET /api/dashboard/capa-status");
    }

    /**
     * 验证从批次到 CAPA 关闭的完整质量闭环。
     *
     * 步骤：
     * 1. 创建批次；
     * 2. 提取 batchId；
     * 3. 创建检测任务；
     * 4. 提取 taskId；
     * 5. 导入 YOLO JSON；
     * 6. 查询 detectionResultId；
     * 7. 创建人工复核；
     * 8. 提取 reviewId；
     * 9. 创建 NCR；
     * 10. 提取 ncrId；
     * 11. 创建 CAPA；
     * 12. 提取 capaId；
     * 13. 关闭 CAPA；
     * 14. 查询并断言 CAPA、NCR、batch 都为 CLOSED。
     *
     * 该测试能够验证核心闭环，是因为它跨越了所有关键 Controller、事务性 Service 和多表状态同步。
     */
    @Test
    void completeQualityClosedLoopShouldWork() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        // 1-2. 创建批次并读取数据库生成的 batchId，避免依赖固定自增值。
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

        // 3-4. 基于新批次创建检测任务，验证 inspection_task 与 production_batch 的关系。
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

        // 5. 导入 YOLO JSON，期望写入 inspection_image 和 detection_result，并把任务推进到 WAIT_REVIEW。
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

        // 6. 按 taskId 查询检测结果，提取刚导入的 detectionResultId。
        JsonNode detections = getJson("/api/detections?taskId=" + taskId);
        JsonNode records = data(detections, "query detection results").path("records");
        assertThat(records.isArray()).as("detection result records should be an array").isTrue();
        assertThat(records).as("detection result records for created task").hasSize(1);
        long detectionResultId = records.get(0).path("id").asLong();
        assertThat(detectionResultId).as("created detection result id").isPositive();

        // 7-8. 人工确认缺陷，验证 review_record 写入且 detection_result.status 同步为 CONFIRMED_DEFECT。
        JsonNode review = postJson("/api/reviews", Map.of(
                "detectionResultId", detectionResultId,
                "reviewerId", INSPECTOR_USER_ID,
                "reviewResult", "CONFIRMED_DEFECT",
                "reviewComment", "Confirmed by integration test"
        ));
        long reviewId = data(review, "create review").path("id").asLong();
        assertThat(reviewId).as("created review id").isPositive();

        // 9-10. 基于确认缺陷创建 NCR，验证 review -> detection -> task -> batch 的追溯链。
        JsonNode ncr = postJson("/api/ncrs", Map.of(
                "ncrNo", "NCR-TEST-" + suffix,
                "reviewId", reviewId,
                "severity", "HIGH",
                "description", "Integration test confirmed defect",
                "createdBy", QUALITY_ENGINEER_USER_ID
        ));
        long ncrId = data(ncr, "create NCR").path("id").asLong();
        assertThat(ncrId).as("created NCR id").isPositive();

        // 11-12. 基于 OPEN NCR 创建 CAPA，验证 NCR 状态和批次状态由后端事务同步推进。
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

        // 13. 关闭 CAPA；后端应在同一事务中关闭 capa_record、ncr_record 和 production_batch。
        patchJson("/api/capas/" + capaId + "/status", Map.of("status", "CLOSED"));

        // 14. 重新查询三张表对应接口，确认闭环最终状态全部为 CLOSED。
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

    /**
     * 执行 GET 请求并解析统一响应 JSON。
     */
    private JsonNode getJson(String url) throws Exception {
        String response = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    /**
     * 执行 POST JSON 请求，并断言 Result.code=200。
     */
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

    /**
     * 执行 PATCH JSON 请求，并断言 Result.code=200。
     */
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

    /**
     * 断言统一响应结构中的业务成功码。
     */
    private void assertSuccess(JsonNode json, String step) {
        assertThat(json.path("code").asInt())
                .as(step + " should return Result.code=200, response=" + json)
                .isEqualTo(200);
    }

    /**
     * 读取 Result.data，并确保需要继续使用的响应数据不为空。
     */
    private JsonNode data(JsonNode json, String step) {
        assertSuccess(json, step);
        JsonNode data = json.path("data");
        assertThat(data.isMissingNode() || data.isNull())
                .as(step + " should return non-null data, response=" + json)
                .isFalse();
        return data;
    }
}

package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.DetectionImportDTO;
import com.example.visualqms.service.DetectionImportService;
import com.example.visualqms.service.DetectionResultService;
import com.example.visualqms.vo.DetectionImportResultVO;
import com.example.visualqms.vo.DetectionResultVO;
import com.example.visualqms.vo.DetectionVisualDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件职责：
 * 提供 YOLO JSON 导入、检测结果列表、检测结果详情和 bbox 视觉详情接口。
 *
 * 所属层级：
 * Controller。
 *
 * 上游调用：
 * detectionApi.js 被 DetectionResultListView.vue 与 DetectionResultDetailView.vue 调用；导入接口可由接口文档或导入页面调用。
 *
 * 下游依赖：
 * 导入请求交给 DetectionImportService；查询请求交给 DetectionResultService。
 *
 * 主要业务链路：
 * 前端或接口调用方 -> detectionApi.js -> /api/detections/* -> DetectionController
 * -> DetectionImportService / DetectionResultService -> Mapper -> inspection_task / inspection_image / detection_result。
 *
 * 注意事项：
 * Controller 不解析 YOLO boxes 细节，避免 Web 层承担落库转换和事务一致性职责。
 */
@Validated
@RestController
@RequestMapping("/api/detections")
public class DetectionController {

    private final DetectionImportService detectionImportService;
    private final DetectionResultService detectionResultService;

    public DetectionController(
            DetectionImportService detectionImportService,
            DetectionResultService detectionResultService) {
        this.detectionImportService = detectionImportService;
        this.detectionResultService = detectionResultService;
    }

    /**
     * 导入 YOLO JSON 检测结果。
     *
     * 调用链：
     * 接口调用方
     * -> detectionApi.js 或直接 POST /api/detections/import-json
     * -> DetectionController
     * -> DetectionImportService#importYoloJson
     * -> DetectionImportServiceImpl
     * -> InspectionTaskMapper / InspectionImageMapper / DetectionResultMapper
     * -> inspection_task / inspection_image / detection_result
     *
     * @param dto 包含 taskId 和 yoloJson，yoloJson 内含 sourceName 与 boxes
     * @return 导入的 imageId、imageCount 和 detectionCount
     */
    @PostMapping("/import-json")
    public Result<DetectionImportResultVO> importYoloJson(@Valid @RequestBody DetectionImportDTO dto) {
        return Result.success(detectionImportService.importYoloJson(dto));
    }

    /**
     * 分页查询检测结果。
     *
     * @param taskId inspection_task 主键过滤条件
     * @param imageId inspection_image 主键过滤条件
     * @param className 模型类别名称过滤条件
     * @param status 复核状态过滤条件
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 检测结果分页数据
     */
    @GetMapping
    public Result<PageResult<DetectionResultVO>> pageDetectionResults(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long imageId,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(detectionResultService.pageDetectionResults(
                taskId,
                imageId,
                className,
                status,
                pageNo,
                pageSize));
    }

    /**
     * 查询检测结果基础详情。
     *
     * @param id detection_result 主键
     * @return 单条检测结果，包含类别、置信度、bbox 和状态
     */
    @GetMapping("/{id}")
    public Result<DetectionResultVO> getDetectionResultDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(detectionResultService.getDetectionResultDetail(id));
    }

    /**
     * 查询用于前端图片叠加 bbox 的视觉详情。
     *
     * 调用链：
     * DetectionResultDetailView.vue
     * -> detectionApi.js#getDetectionVisualDetail
     * -> GET /api/detections/{id}/visual-detail
     * -> DetectionResultService#getDetectionVisualDetail
     * -> DetectionResultMapper / InspectionImageMapper
     * -> detection_result / inspection_image
     * -> DetectionVisualDetailVO
     *
     * @param id detection_result 主键
     * @return 检测结果、图片名称、图片 URI 与 bbox 坐标
     */
    @GetMapping("/{id}/visual-detail")
    public Result<DetectionVisualDetailVO> getDetectionVisualDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(detectionResultService.getDetectionVisualDetail(id));
    }
}

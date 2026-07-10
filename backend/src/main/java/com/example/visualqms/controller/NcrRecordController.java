package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.NcrCreateDTO;
import com.example.visualqms.dto.NcrStatusUpdateDTO;
import com.example.visualqms.service.NcrRecordService;
import com.example.visualqms.vo.NcrRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件职责：
 * 提供 NCR 不合格记录的创建、分页查询、详情、按复核记录查询和状态调整接口。
 *
 * 所属层级：
 * Controller。
 *
 * 上游调用：
 * ncrApi.js 被 ReviewListView.vue 和 NcrListView.vue 调用。
 *
 * 下游依赖：
 * 调用 NcrRecordService，由 Service 追溯 review_record、detection_result、inspection_task 与 production_batch。
 *
 * 主要业务链路：
 * ReviewListView.vue -> ncrApi.js -> POST /api/ncrs -> NcrRecordController
 * -> NcrRecordService -> NcrRecordServiceImpl
 * -> ReviewRecordMapper / DetectionResultMapper / InspectionTaskMapper / NcrRecordMapper / ProductionBatchMapper
 * -> review_record / detection_result / inspection_task / ncr_record / production_batch。
 *
 * 注意事项：
 * 只有 CONFIRMED_DEFECT 复核结果才能创建 NCR；批次状态同步为 NCR_OPEN 的事务在 Service 中完成。
 */
@Validated
@RestController
@RequestMapping("/api/ncrs")
public class NcrRecordController {

    private final NcrRecordService ncrRecordService;

    public NcrRecordController(NcrRecordService ncrRecordService) {
        this.ncrRecordService = ncrRecordService;
    }

    /**
     * 创建 NCR 不合格记录。
     *
     * @param dto NCR 编号、reviewId、严重度、描述和创建人
     * @return 创建后的 NCR，默认状态为 OPEN
     */
    @PostMapping
    public Result<NcrRecordVO> createNcr(@Valid @RequestBody NcrCreateDTO dto) {
        return Result.success(ncrRecordService.createNcr(dto));
    }

    /**
     * 分页查询 NCR。
     *
     * @param ncrNo NCR 编号模糊查询条件
     * @param batchId 批次 ID 过滤条件
     * @param taskId 任务 ID 过滤条件
     * @param severity 严重度过滤条件
     * @param status NCR 状态过滤条件
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页数量
     * @return NCR 分页结果
     */
    @GetMapping
    public Result<PageResult<NcrRecordVO>> pageNcrs(
            @RequestParam(required = false) String ncrNo,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(ncrRecordService.pageNcrs(
                ncrNo,
                batchId,
                taskId,
                severity,
                status,
                pageNo,
                pageSize));
    }

    /**
     * 查询 NCR 详情。
     *
     * @param id ncr_record 主键
     * @return NCR 详情
     */
    @GetMapping("/{id}")
    public Result<NcrRecordVO> getNcrDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(ncrRecordService.getNcrDetail(id));
    }

    /**
     * 根据复核记录查询 NCR，用于避免同一 review_record 重复创建 NCR。
     *
     * @param reviewId review_record 主键
     * @return 已关联的 NCR；不存在时返回 null
     */
    @GetMapping("/by-review/{reviewId}")
    public Result<NcrRecordVO> getNcrByReview(
            @PathVariable @NotNull(message = "reviewId cannot be null") Long reviewId) {
        return Result.success(ncrRecordService.getNcrByReview(reviewId));
    }

    /**
     * 更新 NCR 状态。
     *
     * @param id ncr_record 主键
     * @param dto 目标状态；终态 CLOSED/CANCELLED 不能切换到其他状态
     * @return 状态更新后的 NCR
     */
    @PatchMapping("/{id}/status")
    public Result<NcrRecordVO> updateNcrStatus(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody NcrStatusUpdateDTO dto) {
        return Result.success(ncrRecordService.updateNcrStatus(id, dto));
    }
}

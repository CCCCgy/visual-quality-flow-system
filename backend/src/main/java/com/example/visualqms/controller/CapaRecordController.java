package com.example.visualqms.controller;

import com.example.visualqms.common.PageResult;
import com.example.visualqms.common.Result;
import com.example.visualqms.dto.CapaCreateDTO;
import com.example.visualqms.dto.CapaStatusUpdateDTO;
import com.example.visualqms.dto.CapaUpdateDTO;
import com.example.visualqms.service.CapaRecordService;
import com.example.visualqms.vo.CapaRecordVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件职责：
 * 提供 CAPA 整改记录的创建、分页查询、详情、按 NCR 查询、内容编辑和状态调整接口。
 *
 * 所属层级：
 * Controller。
 *
 * 上游调用：
 * capaApi.js 被 NcrListView.vue 和 CapaListView.vue 调用。
 *
 * 下游依赖：
 * 调用 CapaRecordService，由 Service 校验 OPEN NCR、负责人用户和一条 NCR 只能对应一条 CAPA。
 *
 * 主要业务链路：
 * NcrListView.vue -> capaApi.js -> POST /api/capas -> CapaRecordController
 * -> CapaRecordService -> CapaRecordServiceImpl
 * -> NcrRecordMapper / CapaRecordMapper / ProductionBatchMapper
 * -> ncr_record / capa_record / production_batch。
 *
 * 注意事项：
 * 关闭 CAPA 时需要同步关闭 NCR 和批次，事务边界在 Service 层而不是 Controller 层。
 */
@Validated
@RestController
@RequestMapping("/api/capas")
public class CapaRecordController {

    private final CapaRecordService capaRecordService;

    public CapaRecordController(CapaRecordService capaRecordService) {
        this.capaRecordService = capaRecordService;
    }

    /**
     * 创建 CAPA。
     *
     * @param dto CAPA 编号、ncrId、负责人和措施内容
     * @return 创建后的 CAPA；Service 会同步 NCR=CAPA_CREATED、批次=CAPA_OPEN
     */
    @PostMapping
    public Result<CapaRecordVO> createCapa(@Valid @RequestBody CapaCreateDTO dto) {
        return Result.success(capaRecordService.createCapa(dto));
    }

    /**
     * 分页查询 CAPA。
     *
     * @param capaNo CAPA 编号模糊查询条件
     * @param ncrId NCR ID 过滤条件
     * @param batchId 批次 ID 过滤条件
     * @param ownerId 负责人用户 ID 过滤条件
     * @param status CAPA 状态过滤条件
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页数量
     * @return CAPA 分页结果
     */
    @GetMapping
    public Result<PageResult<CapaRecordVO>> pageCapas(
            @RequestParam(required = false) String capaNo,
            @RequestParam(required = false) Long ncrId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNo must be greater than 0") Long pageNo,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be greater than 0") Long pageSize) {
        return Result.success(capaRecordService.pageCapas(
                capaNo,
                ncrId,
                batchId,
                ownerId,
                status,
                pageNo,
                pageSize));
    }

    /**
     * 查询 CAPA 详情。
     *
     * @param id capa_record 主键
     * @return CAPA 详情
     */
    @GetMapping("/{id}")
    public Result<CapaRecordVO> getCapaDetail(
            @PathVariable @NotNull(message = "id cannot be null") Long id) {
        return Result.success(capaRecordService.getCapaDetail(id));
    }

    /**
     * 根据 NCR 查询 CAPA，用于确认同一 NCR 是否已经创建过整改记录。
     *
     * @param ncrId ncr_record 主键
     * @return 对应 CAPA；不存在时返回 null
     */
    @GetMapping("/by-ncr/{ncrId}")
    public Result<CapaRecordVO> getCapaByNcr(
            @PathVariable @NotNull(message = "ncrId cannot be null") Long ncrId) {
        return Result.success(capaRecordService.getCapaByNcr(ncrId));
    }

    /**
     * 编辑 CAPA 措施和验证信息。
     *
     * @param id capa_record 主键
     * @param dto 根因、纠正措施、预防措施、验证结果和计划日期
     * @return 更新后的 CAPA
     */
    @PutMapping("/{id}")
    public Result<CapaRecordVO> updateCapa(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody CapaUpdateDTO dto) {
        return Result.success(capaRecordService.updateCapa(id, dto));
    }

    /**
     * 更新 CAPA 状态。
     *
     * 调用链：
     * CapaListView.vue
     * -> capaApi.js#updateCapaStatus
     * -> PATCH /api/capas/{id}/status
     * -> CapaRecordService#updateCapaStatus
     * -> CapaRecordServiceImpl
     * -> CapaRecordMapper / NcrRecordMapper / ProductionBatchMapper
     * -> capa_record / ncr_record / production_batch
     *
     * @param id capa_record 主键
     * @param dto 目标状态；关闭时同步写入 closedTime 并关闭 NCR 与批次
     * @return 状态更新后的 CAPA
     */
    @PatchMapping("/{id}/status")
    public Result<CapaRecordVO> updateCapaStatus(
            @PathVariable @NotNull(message = "id cannot be null") Long id,
            @Valid @RequestBody CapaStatusUpdateDTO dto) {
        return Result.success(capaRecordService.updateCapaStatus(id, dto));
    }
}

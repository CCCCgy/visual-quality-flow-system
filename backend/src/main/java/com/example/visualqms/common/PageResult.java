package com.example.visualqms.common;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件职责：
 * 承载分页列表接口的统一 data 结构。
 *
 * 所属层级：
 * common。
 *
 * 上游调用：
 * 由批次、任务、检测结果、复核、NCR、CAPA 等 ServiceImpl 将 MyBatis-Plus Page 转换后返回。
 *
 * 下游依赖：
 * 前端各列表页读取 records 渲染 el-table，读取 total/pageNum/pageSize 驱动 el-pagination。
 *
 * 注意事项：
 * PageResult 位于 Result.data 内部，前端 request.js 已剥离 Result 外壳，因此页面直接拿到本对象。
 *
 * @param <T> 当前页记录类型，通常为 VO 而不是 Entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<T> records;

    /**
     * 从分页查询结果组装返回对象。
     */
    public static <T> PageResult<T> of(Long total, Long pageNum, Long pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records);
    }

    /**
     * 构造空页；当前代码较少直接使用，但保留给无记录场景复用。
     */
    public static <T> PageResult<T> empty(Long pageNum, Long pageSize) {
        return new PageResult<>(0L, pageNum, pageSize, Collections.emptyList());
    }
}

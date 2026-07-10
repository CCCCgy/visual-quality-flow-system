<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">批次详情</h2>
        <p class="page-subtitle">批次基础信息与当前质量状态</p>
      </div>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <div class="content-panel">
      <el-skeleton v-if="loading" :rows="8" animated />

      <el-descriptions
        v-else
        :column="2"
        border
        class="batch-descriptions"
      >
        <el-descriptions-item label="批次号">
          {{ detail.batchNo }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag class="status-tag" :type="statusTagType(detail.status)">
            {{ detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="产品编码">
          {{ detail.productCode }}
        </el-descriptions-item>
        <el-descriptions-item label="产品名称">
          {{ detail.productName }}
        </el-descriptions-item>
        <el-descriptions-item label="计划数量">
          {{ detail.plannedQuantity }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">
          {{ detail.remark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ detail.createdTime }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ detail.updatedTime }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="content-panel task-panel">
      <div class="section-header">
        <div>
          <h3>该批次下的检测任务</h3>
          <p>从批次追溯到检测任务，并继续查看检测结果。</p>
        </div>
      </div>

      <el-table
        v-loading="tasksLoading"
        :data="tasks"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="86" />
        <el-table-column prop="taskNo" label="任务号" min-width="220" />
        <el-table-column prop="modelName" label="模型名称" min-width="180" />
        <el-table-column prop="modelVersion" label="模型版本" min-width="130" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="taskStatusTagType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="importedTime" label="导入时间" min-width="180" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetections(row.id)">
              查看检测结果
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup>
/**
 * 页面或模块职责：
 * 批次详情页，展示单个 production_batch，并列出该批次下的 inspection_task。
 *
 * 路由入口：
 * /batches/:id，其中 id 是 production_batch.id。
 *
 * 调用的前端 API：
 * getBatchDetail、getInspectionTasksByBatch。
 *
 * 对应后端接口：
 * GET /api/batches/{id} -> ProductionBatchController#getBatchDetail；
 * GET /api/inspection-tasks/by-batch/{batchId} -> InspectionTaskController#listTasksByBatch。
 *
 * 主要交互流程：
 * 读取 route.params.id -> 同时查询批次详情和任务列表；
 * 点击任务“查看检测结果” -> /detections?taskId={taskId}。
 */
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBatchDetail } from '../api/batchApi'
import { getInspectionTasksByBatch } from '../api/taskApi'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const tasksLoading = ref(false)
const detail = ref({})
const tasks = ref([])

// 批次状态颜色映射，仅用于展示。
function statusTagType(status) {
  const typeMap = {
    CREATED: 'info',
    INSPECTING: 'primary',
    NCR_OPEN: 'warning',
    CAPA_OPEN: 'warning',
    CLOSED: 'success'
  }
  return typeMap[status] || 'info'
}

// 任务状态颜色映射，与后端 inspection_task.status 保持同名。
function taskStatusTagType(status) {
  const typeMap = {
    CREATED: 'info',
    WAIT_REVIEW: 'warning',
    REVIEWED: 'primary',
    CLOSED: 'success',
    CANCELLED: 'danger'
  }
  return typeMap[status] || 'info'
}

// 按 route.params.id 获取 production_batch 详情。
async function fetchDetail() {
  loading.value = true
  try {
    detail.value = await getBatchDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

// 按当前 batchId 查询 inspection_task 列表，支撑从批次追溯到检测结果。
async function fetchTasks() {
  tasksLoading.value = true
  try {
    tasks.value = await getInspectionTasksByBatch(route.params.id)
  } finally {
    tasksLoading.value = false
  }
}

// 返回批次列表路由。
function goBack() {
  router.push('/batches')
}

// 将任务 ID 放入 query，检测结果列表会读取 route.query.taskId 自动筛选。
function goDetections(taskId) {
  router.push({
    path: '/detections',
    query: { taskId }
  })
}

onMounted(() => {
  fetchDetail()
  fetchTasks()
})
</script>

<style scoped>
/* 批次详情描述区域：限制最大宽度，让字段阅读更稳定。 */
.batch-descriptions {
  max-width: 980px;
}

/* 任务列表面板：与上方批次详情形成清晰间隔。 */
.task-panel {
  margin-top: 16px;
}

/* 小节标题：用于说明从批次追溯到检测任务的关系。 */
.section-header {
  margin-bottom: 14px;
}

.section-header h3 {
  margin: 0;
  color: #172033;
  font-size: 17px;
  font-weight: 650;
}

.section-header p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

/* 小屏处理：Element Plus 描述表格改为块级流式展示。 */
@media (max-width: 760px) {
  :deep(.el-descriptions__body .el-descriptions__table) {
    display: block;
  }
}
</style>

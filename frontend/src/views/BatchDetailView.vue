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

async function fetchDetail() {
  loading.value = true
  try {
    detail.value = await getBatchDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

async function fetchTasks() {
  tasksLoading.value = true
  try {
    tasks.value = await getInspectionTasksByBatch(route.params.id)
  } finally {
    tasksLoading.value = false
  }
}

function goBack() {
  router.push('/batches')
}

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
.batch-descriptions {
  max-width: 980px;
}

.task-panel {
  margin-top: 16px;
}

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

@media (max-width: 760px) {
  :deep(.el-descriptions__body .el-descriptions__table) {
    display: block;
  }
}
</style>

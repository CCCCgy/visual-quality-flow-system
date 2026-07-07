<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">检测结果</h2>
        <p class="page-subtitle">查看 YOLO JSON 导入后的检测框与复核状态</p>
      </div>
    </div>

    <div class="content-panel">
      <div class="toolbar">
        <el-input
          v-model="query.taskId"
          clearable
          placeholder="任务 ID"
          style="width: 130px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-model="query.imageId"
          clearable
          placeholder="图片 ID"
          style="width: 130px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-model="query.className"
          clearable
          placeholder="类别名称"
          style="width: 150px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="query.status"
          clearable
          placeholder="状态筛选"
          style="width: 190px"
          @change="handleSearch"
          @clear="handleSearch"
        >
          <el-option
            v-for="status in statusOptions"
            :key="status"
            :label="status"
            :value="status"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="86" />
        <el-table-column prop="taskId" label="任务 ID" width="110" />
        <el-table-column prop="imageId" label="图片 ID" width="110" />
        <el-table-column prop="classId" label="类别 ID" width="100" />
        <el-table-column prop="className" label="类别名称" width="120" />
        <el-table-column label="置信度" width="110" align="right">
          <template #default="{ row }">
            {{ formatConfidence(row.confidence) }}
          </template>
        </el-table-column>
        <el-table-column label="bboxX1" width="100" align="right">
          <template #default="{ row }">{{ formatNumber(row.bboxX1) }}</template>
        </el-table-column>
        <el-table-column label="bboxY1" width="100" align="right">
          <template #default="{ row }">{{ formatNumber(row.bboxY1) }}</template>
        </el-table-column>
        <el-table-column label="bboxX2" width="100" align="right">
          <template #default="{ row }">{{ formatNumber(row.bboxX2) }}</template>
        </el-table-column>
        <el-table-column label="bboxY2" width="100" align="right">
          <template #default="{ row }">{{ formatNumber(row.bboxY2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="170">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="statusTagType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" min-width="180" />
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page.pageNo"
          v-model:page-size="page.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="fetchDetections"
          @current-change="fetchDetections"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getDetectionResultPage } from '../api/detectionApi'

const route = useRoute()
const loading = ref(false)
const tableData = ref([])

const query = reactive({
  taskId: '',
  imageId: '',
  className: '',
  status: ''
})

const page = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const statusOptions = [
  'PENDING_REVIEW',
  'CONFIRMED_DEFECT',
  'FALSE_POSITIVE',
  'NEED_RECHECK'
]

function statusTagType(status) {
  const typeMap = {
    PENDING_REVIEW: 'warning',
    CONFIRMED_DEFECT: 'danger',
    FALSE_POSITIVE: 'info',
    NEED_RECHECK: 'primary'
  }
  return typeMap[status] || 'info'
}

function formatNumber(value) {
  if (value === null || typeof value === 'undefined') {
    return '-'
  }
  return Number(value).toFixed(2)
}

function formatConfidence(value) {
  if (value === null || typeof value === 'undefined') {
    return '-'
  }
  return Number(value).toFixed(4)
}

async function fetchDetections() {
  loading.value = true
  try {
    const data = await getDetectionResultPage({
      taskId: query.taskId || undefined,
      imageId: query.imageId || undefined,
      className: query.className || undefined,
      status: query.status || undefined,
      pageNo: page.pageNo,
      pageSize: page.pageSize
    })
    tableData.value = data.records || []
    page.total = data.total || 0
  } finally {
    loading.value = false
  }
}

function applyRouteQuery() {
  query.taskId = route.query.taskId ? String(route.query.taskId) : ''
}

function handleSearch() {
  page.pageNo = 1
  fetchDetections()
}

function resetSearch() {
  query.taskId = ''
  query.imageId = ''
  query.className = ''
  query.status = ''
  handleSearch()
}

watch(
  () => route.query.taskId,
  () => {
    applyRouteQuery()
    handleSearch()
  }
)

onMounted(() => {
  applyRouteQuery()
  fetchDetections()
})
</script>

<style scoped>
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 760px) {
  .pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>

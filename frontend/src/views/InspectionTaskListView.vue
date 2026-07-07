<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">检测任务</h2>
        <p class="page-subtitle">查看批次下的模型检测任务与导入状态</p>
      </div>
    </div>

    <div class="content-panel">
      <div class="toolbar">
        <el-input
          v-model="query.taskNo"
          clearable
          placeholder="任务号模糊查询"
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-model="query.batchId"
          clearable
          placeholder="批次 ID"
          style="width: 140px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="query.status"
          clearable
          placeholder="状态筛选"
          style="width: 180px"
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
        <el-table-column prop="taskNo" label="任务号" min-width="220" />
        <el-table-column prop="batchId" label="批次 ID" width="110" />
        <el-table-column prop="modelName" label="模型名称" min-width="180" />
        <el-table-column prop="modelVersion" label="模型版本" min-width="130" />
        <el-table-column prop="sourceType" label="来源类型" width="130" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="statusTagType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="importedTime" label="导入时间" min-width="180" />
        <el-table-column prop="createdTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetections(row.id)">
              查看检测结果
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page.pageNo"
          v-model:page-size="page.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="fetchTasks"
          @current-change="fetchTasks"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getInspectionTaskPage } from '../api/taskApi'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])

const query = reactive({
  taskNo: '',
  batchId: '',
  status: ''
})

const page = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const statusOptions = ['CREATED', 'WAIT_REVIEW', 'REVIEWED', 'CLOSED', 'CANCELLED']

function statusTagType(status) {
  const typeMap = {
    CREATED: 'info',
    WAIT_REVIEW: 'warning',
    REVIEWED: 'primary',
    CLOSED: 'success',
    CANCELLED: 'danger'
  }
  return typeMap[status] || 'info'
}

async function fetchTasks() {
  loading.value = true
  try {
    const data = await getInspectionTaskPage({
      taskNo: query.taskNo || undefined,
      batchId: query.batchId || undefined,
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

function handleSearch() {
  page.pageNo = 1
  fetchTasks()
}

function resetSearch() {
  query.taskNo = ''
  query.batchId = ''
  query.status = ''
  handleSearch()
}

function goDetections(taskId) {
  router.push({
    path: '/detections',
    query: { taskId }
  })
}

onMounted(fetchTasks)
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

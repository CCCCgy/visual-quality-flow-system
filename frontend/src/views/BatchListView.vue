<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">批次管理</h2>
        <p class="page-subtitle">查看生产批次与质量闭环状态</p>
      </div>
    </div>

    <div class="content-panel">
      <div class="toolbar">
        <el-input
          v-model="query.batchNo"
          clearable
          placeholder="批次号模糊查询"
          style="width: 220px"
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
        <el-table-column prop="batchNo" label="批次号" min-width="210" />
        <el-table-column prop="productCode" label="产品编码" min-width="160" />
        <el-table-column prop="productName" label="产品名称" min-width="190" />
        <el-table-column
          prop="plannedQuantity"
          label="计划数量"
          width="110"
          align="right"
        />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="statusTagType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.id)">
              查看详情
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
          @size-change="fetchBatches"
          @current-change="fetchBatches"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getBatchPage } from '../api/batchApi'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])

const query = reactive({
  batchNo: '',
  status: ''
})

const page = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const statusOptions = ['CREATED', 'INSPECTING', 'NCR_OPEN', 'CAPA_OPEN', 'CLOSED']

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

async function fetchBatches() {
  loading.value = true
  try {
    const data = await getBatchPage({
      batchNo: query.batchNo || undefined,
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
  fetchBatches()
}

function resetSearch() {
  query.batchNo = ''
  query.status = ''
  handleSearch()
}

function goDetail(id) {
  router.push(`/batches/${id}`)
}

onMounted(fetchBatches)
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

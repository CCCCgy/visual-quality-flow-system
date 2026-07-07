<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">人工复核</h2>
        <p class="page-subtitle">查看检测结果的人工复核记录与复核结论</p>
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
          v-model="query.reviewerId"
          clearable
          placeholder="复核人 ID"
          style="width: 140px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="query.reviewResult"
          clearable
          placeholder="复核结果"
          style="width: 190px"
          @change="handleSearch"
          @clear="handleSearch"
        >
          <el-option
            v-for="result in reviewResultOptions"
            :key="result"
            :label="result"
            :value="result"
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
        <el-table-column
          prop="detectionResultId"
          label="检测结果 ID"
          width="130"
        />
        <el-table-column prop="taskId" label="任务 ID" width="110" />
        <el-table-column prop="imageId" label="图片 ID" width="110" />
        <el-table-column prop="reviewerId" label="复核人 ID" width="120" />
        <el-table-column label="复核结果" width="180">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="reviewResultTagType(row.reviewResult)">
              {{ row.reviewResult }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="reviewComment"
          label="复核备注"
          min-width="240"
          show-overflow-tooltip
        />
        <el-table-column prop="reviewedTime" label="复核时间" min-width="180" />
        <el-table-column prop="createdTime" label="创建时间" min-width="180" />
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page.pageNo"
          v-model:page-size="page.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="fetchReviews"
          @current-change="fetchReviews"
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getReviewPage } from '../api/reviewApi'

const loading = ref(false)
const tableData = ref([])

const query = reactive({
  taskId: '',
  imageId: '',
  reviewerId: '',
  reviewResult: ''
})

const page = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const reviewResultOptions = [
  'CONFIRMED_DEFECT',
  'FALSE_POSITIVE',
  'NEED_RECHECK'
]

function reviewResultTagType(result) {
  const typeMap = {
    CONFIRMED_DEFECT: 'danger',
    FALSE_POSITIVE: 'info',
    NEED_RECHECK: 'primary'
  }
  return typeMap[result] || 'info'
}

async function fetchReviews() {
  loading.value = true
  try {
    const data = await getReviewPage({
      taskId: query.taskId || undefined,
      imageId: query.imageId || undefined,
      reviewerId: query.reviewerId || undefined,
      reviewResult: query.reviewResult || undefined,
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
  fetchReviews()
}

function resetSearch() {
  query.taskId = ''
  query.imageId = ''
  query.reviewerId = ''
  query.reviewResult = ''
  handleSearch()
}

onMounted(fetchReviews)
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

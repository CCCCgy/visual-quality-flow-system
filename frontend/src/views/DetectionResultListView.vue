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
        <el-button type="warning" plain @click="showPendingOnly">
          只看待复核
        </el-button>
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_REVIEW'"
              link
              type="warning"
              @click="openReviewDialog(row)"
            >
              人工复核
            </el-button>
            <span v-else class="muted-action">已复核</span>
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
          @size-change="fetchDetections"
          @current-change="fetchDetections"
        />
      </div>
    </div>

    <el-dialog
      v-model="reviewDialogVisible"
      title="人工复核"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="reviewFormRef"
        :model="reviewForm"
        :rules="reviewRules"
        label-width="132px"
      >
        <el-form-item label="检测结果 ID">
          <el-input v-model="reviewForm.detectionResultId" readonly />
        </el-form-item>
        <el-form-item label="缺陷类别">
          <el-input v-model="reviewForm.className" readonly />
        </el-form-item>
        <el-form-item label="置信度">
          <el-input v-model="reviewForm.confidenceText" readonly />
        </el-form-item>
        <el-form-item label="复核人 ID" prop="reviewerId">
          <el-input-number
            v-model="reviewForm.reviewerId"
            :min="1"
            :step="1"
            controls-position="right"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="复核结果" prop="reviewResult">
          <el-select
            v-model="reviewForm.reviewResult"
            placeholder="选择复核结果"
            style="width: 260px"
          >
            <el-option
              v-for="result in reviewResultOptions"
              :key="result"
              :label="result"
              :value="result"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="复核备注">
          <el-input
            v-model="reviewForm.reviewComment"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="填写复核说明"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="reviewSubmitting"
          @click="submitReview"
        >
          提交复核
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDetectionResultPage } from '../api/detectionApi'
import { createReview } from '../api/reviewApi'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewFormRef = ref()

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

const reviewResultOptions = [
  'CONFIRMED_DEFECT',
  'FALSE_POSITIVE',
  'NEED_RECHECK'
]

const reviewForm = reactive({
  detectionResultId: '',
  className: '',
  confidenceText: '',
  reviewerId: 2,
  reviewResult: 'CONFIRMED_DEFECT',
  reviewComment: ''
})

const reviewRules = {
  reviewerId: [
    { required: true, message: '请输入复核人 ID', trigger: 'change' }
  ],
  reviewResult: [
    { required: true, message: '请选择复核结果', trigger: 'change' }
  ]
}

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

function showPendingOnly() {
  query.status = 'PENDING_REVIEW'
  handleSearch()
}

function resetSearch() {
  query.taskId = ''
  query.imageId = ''
  query.className = ''
  query.status = ''
  handleSearch()
}

function goDetail(row) {
  router.push(`/detections/${row.id}`)
}

function openReviewDialog(row) {
  reviewForm.detectionResultId = row.id
  reviewForm.className = row.className
  reviewForm.confidenceText = formatConfidence(row.confidence)
  reviewForm.reviewerId = 2
  reviewForm.reviewResult = 'CONFIRMED_DEFECT'
  reviewForm.reviewComment = ''
  reviewDialogVisible.value = true
}

async function submitReview() {
  await reviewFormRef.value?.validate()
  reviewSubmitting.value = true
  try {
    await createReview({
      detectionResultId: Number(reviewForm.detectionResultId),
      reviewerId: Number(reviewForm.reviewerId),
      reviewResult: reviewForm.reviewResult,
      reviewComment: reviewForm.reviewComment || undefined
    })
    reviewDialogVisible.value = false
    await fetchDetections()
  } finally {
    reviewSubmitting.value = false
  }
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

.muted-action {
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 760px) {
  .pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>

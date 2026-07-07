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
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.reviewResult === 'CONFIRMED_DEFECT'"
              link
              type="primary"
              @click="openNcrDialog(row)"
            >
              创建 NCR
            </el-button>
            <span v-else class="muted-action">无需 NCR</span>
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
          @size-change="fetchReviews"
          @current-change="fetchReviews"
        />
      </div>
    </div>

    <el-dialog
      v-model="ncrDialogVisible"
      title="创建 NCR"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="ncrFormRef"
        :model="ncrForm"
        :rules="ncrRules"
        label-width="120px"
      >
        <el-form-item label="复核 ID">
          <el-input v-model="ncrForm.reviewId" readonly />
        </el-form-item>
        <el-form-item label="NCR 编号" prop="ncrNo">
          <el-input v-model="ncrForm.ncrNo" />
        </el-form-item>
        <el-form-item label="严重度" prop="severity">
          <el-select v-model="ncrForm.severity" style="width: 180px">
            <el-option
              v-for="severity in severityOptions"
              :key="severity"
              :label="severity"
              :value="severity"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="ncrForm.description"
            type="textarea"
            :rows="4"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="创建人 ID" prop="createdBy">
          <el-input-number
            v-model="ncrForm.createdBy"
            :min="1"
            :step="1"
            controls-position="right"
            style="width: 180px"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="ncrDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ncrSubmitting" @click="submitNcr">
          创建 NCR
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createNcr } from '../api/ncrApi'
import { getReviewPage } from '../api/reviewApi'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const ncrDialogVisible = ref(false)
const ncrSubmitting = ref(false)
const ncrFormRef = ref()

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

const severityOptions = ['LOW', 'MEDIUM', 'HIGH']

const ncrForm = reactive({
  reviewId: '',
  ncrNo: '',
  severity: 'MEDIUM',
  description: '',
  createdBy: 3
})

const ncrRules = {
  ncrNo: [{ required: true, message: '请输入 NCR 编号', trigger: 'blur' }],
  severity: [{ required: true, message: '请选择严重度', trigger: 'change' }],
  description: [
    { required: true, message: '请输入问题描述', trigger: 'blur' }
  ],
  createdBy: [{ required: true, message: '请输入创建人 ID', trigger: 'change' }]
}

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

function openNcrDialog(row) {
  ncrForm.reviewId = row.id
  ncrForm.ncrNo = `NCR-FE-${Date.now()}`
  ncrForm.severity = 'MEDIUM'
  ncrForm.description =
    row.reviewComment || 'Confirmed defect requires NCR tracking.'
  ncrForm.createdBy = 3
  ncrDialogVisible.value = true
}

async function submitNcr() {
  await ncrFormRef.value?.validate()
  ncrSubmitting.value = true
  try {
    await createNcr({
      ncrNo: ncrForm.ncrNo,
      reviewId: Number(ncrForm.reviewId),
      severity: ncrForm.severity,
      description: ncrForm.description,
      createdBy: Number(ncrForm.createdBy)
    })
    ElMessage.success('NCR 创建成功')
    ncrDialogVisible.value = false
    router.push('/ncrs')
  } finally {
    ncrSubmitting.value = false
  }
}

onMounted(fetchReviews)
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

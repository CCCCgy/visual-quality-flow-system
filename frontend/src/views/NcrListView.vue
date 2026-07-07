<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">NCR 不合格记录</h2>
        <p class="page-subtitle">跟踪确认缺陷形成的不合格记录与 CAPA 创建状态</p>
      </div>
    </div>

    <div class="content-panel">
      <div class="toolbar">
        <el-input
          v-model="query.ncrNo"
          clearable
          placeholder="NCR 编号"
          style="width: 210px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-model="query.batchId"
          clearable
          placeholder="批次 ID"
          style="width: 120px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-model="query.taskId"
          clearable
          placeholder="任务 ID"
          style="width: 120px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="query.severity"
          clearable
          placeholder="严重度"
          style="width: 130px"
          @change="handleSearch"
          @clear="handleSearch"
        >
          <el-option
            v-for="severity in severityOptions"
            :key="severity"
            :label="severity"
            :value="severity"
          />
        </el-select>
        <el-select
          v-model="query.status"
          clearable
          placeholder="状态"
          style="width: 160px"
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
        <el-table-column prop="ncrNo" label="NCR 编号" min-width="220" />
        <el-table-column prop="batchId" label="批次 ID" width="110" />
        <el-table-column prop="taskId" label="任务 ID" width="110" />
        <el-table-column
          prop="detectionResultId"
          label="检测结果 ID"
          width="130"
        />
        <el-table-column prop="reviewId" label="复核 ID" width="110" />
        <el-table-column prop="severity" label="严重度" width="110" />
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="ncrStatusTagType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="description"
          label="问题描述"
          min-width="240"
          show-overflow-tooltip
        />
        <el-table-column prop="createdBy" label="创建人" width="100" />
        <el-table-column prop="createdTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'OPEN'">
              <el-button link type="primary" @click="openCapaDialog(row)">
                创建 CAPA
              </el-button>
              <el-button link type="success" @click="changeNcrStatus(row, 'CLOSED')">
                关闭 NCR
              </el-button>
              <el-button link type="danger" @click="changeNcrStatus(row, 'CANCELLED')">
                取消 NCR
              </el-button>
            </template>
            <span v-else class="muted-action">无可用操作</span>
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
          @size-change="fetchNcrs"
          @current-change="fetchNcrs"
        />
      </div>
    </div>

    <el-dialog
      v-model="capaDialogVisible"
      title="创建 CAPA"
      width="620px"
      destroy-on-close
    >
      <el-form
        ref="capaFormRef"
        :model="capaForm"
        :rules="capaRules"
        label-width="132px"
      >
        <el-form-item label="NCR ID">
          <el-input v-model="capaForm.ncrId" readonly />
        </el-form-item>
        <el-form-item label="CAPA 编号" prop="capaNo">
          <el-input v-model="capaForm.capaNo" />
        </el-form-item>
        <el-form-item label="负责人 ID" prop="ownerId">
          <el-input-number
            v-model="capaForm.ownerId"
            :min="1"
            :step="1"
            controls-position="right"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="根因分析" prop="rootCause">
          <el-input v-model="capaForm.rootCause" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="纠正措施" prop="correctiveAction">
          <el-input
            v-model="capaForm.correctiveAction"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="预防措施" prop="preventiveAction">
          <el-input
            v-model="capaForm.preventiveAction"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="计划完成日期">
          <el-date-picker
            v-model="capaForm.dueDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="capaDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="capaSubmitting" @click="submitCapa">
          创建 CAPA
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCapa } from '../api/capaApi'
import { getNcrPage, updateNcrStatus } from '../api/ncrApi'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const capaDialogVisible = ref(false)
const capaSubmitting = ref(false)
const capaFormRef = ref()

const query = reactive({
  ncrNo: '',
  batchId: '',
  taskId: '',
  severity: '',
  status: ''
})

const page = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const capaForm = reactive({
  ncrId: '',
  capaNo: '',
  ownerId: 3,
  rootCause: '',
  correctiveAction: '',
  preventiveAction: '',
  dueDate: ''
})

const severityOptions = ['LOW', 'MEDIUM', 'HIGH']
const statusOptions = ['OPEN', 'CAPA_CREATED', 'CLOSED', 'CANCELLED']

const capaRules = {
  capaNo: [{ required: true, message: '请输入 CAPA 编号', trigger: 'blur' }],
  ownerId: [{ required: true, message: '请输入负责人 ID', trigger: 'change' }],
  rootCause: [{ required: true, message: '请输入根因分析', trigger: 'blur' }],
  correctiveAction: [
    { required: true, message: '请输入纠正措施', trigger: 'blur' }
  ],
  preventiveAction: [
    { required: true, message: '请输入预防措施', trigger: 'blur' }
  ]
}

function ncrStatusTagType(status) {
  const typeMap = {
    OPEN: 'warning',
    CAPA_CREATED: 'primary',
    CLOSED: 'success',
    CANCELLED: 'danger'
  }
  return typeMap[status] || 'info'
}

async function fetchNcrs() {
  loading.value = true
  try {
    const data = await getNcrPage({
      ncrNo: query.ncrNo || undefined,
      batchId: query.batchId || undefined,
      taskId: query.taskId || undefined,
      severity: query.severity || undefined,
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
  fetchNcrs()
}

function resetSearch() {
  query.ncrNo = ''
  query.batchId = ''
  query.taskId = ''
  query.severity = ''
  query.status = ''
  handleSearch()
}

function openCapaDialog(row) {
  capaForm.ncrId = row.id
  capaForm.capaNo = `CAPA-FE-${Date.now()}`
  capaForm.ownerId = 3
  capaForm.rootCause = ''
  capaForm.correctiveAction = ''
  capaForm.preventiveAction = ''
  capaForm.dueDate = ''
  capaDialogVisible.value = true
}

async function submitCapa() {
  await capaFormRef.value?.validate()
  capaSubmitting.value = true
  try {
    await createCapa({
      capaNo: capaForm.capaNo,
      ncrId: Number(capaForm.ncrId),
      ownerId: Number(capaForm.ownerId),
      rootCause: capaForm.rootCause,
      correctiveAction: capaForm.correctiveAction,
      preventiveAction: capaForm.preventiveAction,
      dueDate: capaForm.dueDate || undefined
    })
    ElMessage.success('CAPA 创建成功')
    capaDialogVisible.value = false
    await fetchNcrs()
    router.push('/capas')
  } finally {
    capaSubmitting.value = false
  }
}

async function changeNcrStatus(row, status) {
  const actionText = status === 'CLOSED' ? '关闭' : '取消'
  await ElMessageBox.confirm(`确认${actionText}该 NCR？`, '确认操作', {
    type: 'warning'
  })
  await updateNcrStatus(row.id, status)
  ElMessage.success(`NCR 已${actionText}`)
  fetchNcrs()
}

onMounted(fetchNcrs)
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

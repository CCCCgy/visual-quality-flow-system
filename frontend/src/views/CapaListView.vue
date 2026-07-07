<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">CAPA 整改闭环</h2>
        <p class="page-subtitle">跟踪纠正预防措施、验证结果与关闭状态</p>
      </div>
    </div>

    <div class="content-panel">
      <div class="toolbar">
        <el-input
          v-model="query.capaNo"
          clearable
          placeholder="CAPA 编号"
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-model="query.ncrId"
          clearable
          placeholder="NCR ID"
          style="width: 120px"
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
          v-model="query.ownerId"
          clearable
          placeholder="负责人 ID"
          style="width: 130px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="query.status"
          clearable
          placeholder="状态"
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
        <el-table-column prop="capaNo" label="CAPA 编号" min-width="220" />
        <el-table-column prop="ncrId" label="NCR ID" width="110" />
        <el-table-column prop="batchId" label="批次 ID" width="110" />
        <el-table-column prop="ownerId" label="负责人 ID" width="120" />
        <el-table-column
          prop="rootCause"
          label="根因分析"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          prop="correctiveAction"
          label="纠正措施"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          prop="preventiveAction"
          label="预防措施"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          prop="verifyResult"
          label="验证结果"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column label="状态" width="170">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="capaStatusTagType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="计划完成" width="130" />
        <el-table-column prop="closedTime" label="关闭时间" min-width="180" />
        <el-table-column prop="createdTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <template v-if="!isTerminal(row.status)">
              <el-button link type="primary" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button
                v-if="row.status !== 'PENDING_VERIFY'"
                link
                type="warning"
                @click="changeCapaStatus(row, 'PENDING_VERIFY')"
              >
                待验证
              </el-button>
              <el-button link type="success" @click="changeCapaStatus(row, 'CLOSED')">
                关闭
              </el-button>
              <el-button link type="danger" @click="changeCapaStatus(row, 'CANCELLED')">
                取消
              </el-button>
            </template>
            <span v-else class="muted-action">已终态</span>
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
          @size-change="fetchCapas"
          @current-change="fetchCapas"
        />
      </div>
    </div>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑 CAPA"
      width="620px"
      destroy-on-close
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="132px"
      >
        <el-form-item label="根因分析" prop="rootCause">
          <el-input v-model="editForm.rootCause" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="纠正措施">
          <el-input
            v-model="editForm.correctiveAction"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="预防措施" prop="preventiveAction">
          <el-input
            v-model="editForm.preventiveAction"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="验证结果">
          <el-input v-model="editForm.verifyResult" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="计划完成日期">
          <el-date-picker
            v-model="editForm.dueDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCapaPage, updateCapa, updateCapaStatus } from '../api/capaApi'

const loading = ref(false)
const tableData = ref([])
const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref()

const query = reactive({
  capaNo: '',
  ncrId: '',
  batchId: '',
  ownerId: '',
  status: ''
})

const page = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const editForm = reactive({
  id: '',
  rootCause: '',
  correctiveAction: '',
  preventiveAction: '',
  verifyResult: '',
  dueDate: ''
})

const statusOptions = [
  'PENDING_ANALYSIS',
  'IN_PROGRESS',
  'PENDING_VERIFY',
  'CLOSED',
  'CANCELLED'
]

const editRules = {
  rootCause: [{ required: true, message: '请输入根因分析', trigger: 'blur' }],
  preventiveAction: [
    { required: true, message: '请输入预防措施', trigger: 'blur' }
  ]
}

function capaStatusTagType(status) {
  const typeMap = {
    PENDING_ANALYSIS: 'info',
    IN_PROGRESS: 'warning',
    PENDING_VERIFY: 'primary',
    CLOSED: 'success',
    CANCELLED: 'danger'
  }
  return typeMap[status] || 'info'
}

function isTerminal(status) {
  return status === 'CLOSED' || status === 'CANCELLED'
}

async function fetchCapas() {
  loading.value = true
  try {
    const data = await getCapaPage({
      capaNo: query.capaNo || undefined,
      ncrId: query.ncrId || undefined,
      batchId: query.batchId || undefined,
      ownerId: query.ownerId || undefined,
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
  fetchCapas()
}

function resetSearch() {
  query.capaNo = ''
  query.ncrId = ''
  query.batchId = ''
  query.ownerId = ''
  query.status = ''
  handleSearch()
}

function openEditDialog(row) {
  editForm.id = row.id
  editForm.rootCause = row.rootCause || ''
  editForm.correctiveAction = row.correctiveAction || ''
  editForm.preventiveAction = row.preventiveAction || ''
  editForm.verifyResult = row.verifyResult || ''
  editForm.dueDate = row.dueDate || ''
  editDialogVisible.value = true
}

async function submitEdit() {
  await editFormRef.value?.validate()
  editSubmitting.value = true
  try {
    await updateCapa(editForm.id, {
      rootCause: editForm.rootCause,
      correctiveAction: editForm.correctiveAction || undefined,
      preventiveAction: editForm.preventiveAction,
      verifyResult: editForm.verifyResult || undefined,
      dueDate: editForm.dueDate || undefined
    })
    ElMessage.success('CAPA 已更新')
    editDialogVisible.value = false
    fetchCapas()
  } finally {
    editSubmitting.value = false
  }
}

async function changeCapaStatus(row, status) {
  const actionTextMap = {
    PENDING_VERIFY: '更新为待验证',
    CLOSED: '关闭',
    CANCELLED: '取消'
  }
  const actionText = actionTextMap[status]
  await ElMessageBox.confirm(`确认${actionText}该 CAPA？`, '确认操作', {
    type: 'warning'
  })
  await updateCapaStatus(row.id, status)
  if (status === 'CLOSED') {
    ElMessage.success('CAPA 已关闭，NCR 与批次状态已同步关闭。')
  } else {
    ElMessage.success(`CAPA 已${actionText}`)
  }
  fetchCapas()
}

onMounted(fetchCapas)
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

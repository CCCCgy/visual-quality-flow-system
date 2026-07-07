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
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBatchDetail } from '../api/batchApi'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref({})

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

async function fetchDetail() {
  loading.value = true
  try {
    detail.value = await getBatchDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/batches')
}

onMounted(fetchDetail)
</script>

<style scoped>
.batch-descriptions {
  max-width: 980px;
}

@media (max-width: 760px) {
  :deep(.el-descriptions__body .el-descriptions__table) {
    display: block;
  }
}
</style>

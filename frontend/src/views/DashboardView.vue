<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">工业视觉质量看板</h2>
        <p class="page-subtitle">
          基于检测结果、人工复核、NCR 与 CAPA 的质量闭环状态统计
        </p>
      </div>
    </div>

    <div v-loading="loading" class="dashboard-shell">
      <div class="summary-grid">
        <div v-for="card in summaryCards" :key="card.key" class="metric-card">
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value">{{ card.value }}</div>
          <div class="metric-note">{{ card.note }}</div>
        </div>
      </div>

      <div class="chart-grid">
        <div class="content-panel chart-panel">
          <div class="panel-title">批次状态分布</div>
          <div ref="batchStatusChartRef" class="chart"></div>
        </div>
        <div class="content-panel chart-panel">
          <div class="panel-title">检测结果状态分布</div>
          <div ref="detectionStatusChartRef" class="chart"></div>
        </div>
        <div class="content-panel chart-panel chart-panel-wide">
          <div class="panel-title">缺陷类别分布</div>
          <div ref="defectClassChartRef" class="chart"></div>
        </div>
        <div class="content-panel chart-panel small-chart-panel">
          <div class="panel-title">NCR 状态分布</div>
          <div ref="ncrStatusChartRef" class="chart small-chart"></div>
        </div>
        <div class="content-panel chart-panel small-chart-panel">
          <div class="panel-title">CAPA 状态分布</div>
          <div ref="capaStatusChartRef" class="chart small-chart"></div>
        </div>
      </div>

      <div class="content-panel action-panel">
        <div class="panel-title">待处理提示</div>
        <div class="action-grid">
          <div v-for="item in actionItems" :key="item.label" class="action-item">
            <span class="action-label">{{ item.label }}</span>
            <span :class="['action-value', item.level]">{{ item.value }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  getBatchStatusStats,
  getCapaStatusStats,
  getDashboardSummary,
  getDefectClassStats,
  getDetectionStatusStats,
  getNcrStatusStats
} from '../api/dashboardApi'

const loading = ref(false)
const summary = ref({
  batchCount: 0,
  taskCount: 0,
  detectionCount: 0,
  pendingReviewCount: 0,
  confirmedDefectCount: 0,
  openNcrCount: 0,
  inProgressCapaCount: 0,
  closedCapaCount: 0
})
const batchStatusStats = ref([])
const detectionStatusStats = ref([])
const defectClassStats = ref([])
const ncrStatusStats = ref([])
const capaStatusStats = ref([])

const batchStatusChartRef = ref()
const detectionStatusChartRef = ref()
const defectClassChartRef = ref()
const ncrStatusChartRef = ref()
const capaStatusChartRef = ref()

const charts = []

const summaryCards = computed(() => [
  {
    key: 'batchCount',
    label: '生产批次',
    value: summary.value.batchCount,
    note: 'production_batch 总数'
  },
  {
    key: 'taskCount',
    label: '检测任务',
    value: summary.value.taskCount,
    note: 'inspection_task 总数'
  },
  {
    key: 'detectionCount',
    label: '检测结果',
    value: summary.value.detectionCount,
    note: 'detection_result 总数'
  },
  {
    key: 'pendingReviewCount',
    label: '待复核',
    value: summary.value.pendingReviewCount,
    note: 'PENDING_REVIEW'
  },
  {
    key: 'confirmedDefectCount',
    label: '确认缺陷',
    value: summary.value.confirmedDefectCount,
    note: 'CONFIRMED_DEFECT'
  },
  {
    key: 'openNcrCount',
    label: 'OPEN NCR',
    value: summary.value.openNcrCount,
    note: '待处置不合格'
  },
  {
    key: 'inProgressCapaCount',
    label: '进行中 CAPA',
    value: summary.value.inProgressCapaCount,
    note: 'IN_PROGRESS'
  },
  {
    key: 'closedCapaCount',
    label: '已关闭 CAPA',
    value: summary.value.closedCapaCount,
    note: 'CLOSED'
  }
])

const actionItems = computed(() => [
  {
    label: '待复核数量',
    value: summary.value.pendingReviewCount,
    level: summary.value.pendingReviewCount > 0 ? 'warning' : 'ok'
  },
  {
    label: 'OPEN NCR 数量',
    value: summary.value.openNcrCount,
    level: summary.value.openNcrCount > 0 ? 'danger' : 'ok'
  },
  {
    label: '进行中 CAPA 数量',
    value: summary.value.inProgressCapaCount,
    level: summary.value.inProgressCapaCount > 0 ? 'primary' : 'ok'
  }
])

async function fetchDashboardData() {
  loading.value = true
  try {
    const [
      summaryData,
      batchStatusData,
      detectionStatusData,
      defectClassData,
      ncrStatusData,
      capaStatusData
    ] = await Promise.all([
      getDashboardSummary(),
      getBatchStatusStats(),
      getDetectionStatusStats(),
      getDefectClassStats(),
      getNcrStatusStats(),
      getCapaStatusStats()
    ])

    summary.value = { ...summary.value, ...(summaryData || {}) }
    batchStatusStats.value = batchStatusData || []
    detectionStatusStats.value = detectionStatusData || []
    defectClassStats.value = defectClassData || []
    ncrStatusStats.value = ncrStatusData || []
    capaStatusStats.value = capaStatusData || []

    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error('Dashboard 数据加载失败')
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  disposeCharts()
  charts.push(
    renderPieChart(batchStatusChartRef.value, batchStatusStats.value, '暂无批次状态数据'),
    renderBarChart(
      detectionStatusChartRef.value,
      detectionStatusStats.value.map((item) => ({
        name: item.status || '未设置',
        value: item.count || 0
      })),
      '暂无检测状态数据'
    ),
    renderBarChart(
      defectClassChartRef.value,
      defectClassStats.value.map((item) => ({
        name: item.className || '未分类',
        value: item.count || 0
      })),
      '暂无缺陷类别数据'
    ),
    renderPieChart(ncrStatusChartRef.value, ncrStatusStats.value, '暂无 NCR 状态数据'),
    renderPieChart(capaStatusChartRef.value, capaStatusStats.value, '暂无 CAPA 状态数据')
  )
}

function renderPieChart(el, rows, emptyText) {
  if (!el) {
    return null
  }

  const data = normalizeStatusRows(rows)
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: {
      bottom: 0,
      type: 'scroll'
    },
    color: ['#2f8f83', '#5470c6', '#e6a23c', '#f56c6c', '#909399', '#67c23a'],
    graphic: emptyGraphic(data.length === 0, emptyText),
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '43%'],
        avoidLabelOverlap: true,
        data
      }
    ]
  })
  return chart
}

function renderBarChart(el, rows, emptyText) {
  if (!el) {
    return null
  }

  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: {
      top: 24,
      left: 12,
      right: 16,
      bottom: 16,
      containLabel: true
    },
    color: ['#2f8f83'],
    graphic: emptyGraphic(rows.length === 0, emptyText),
    xAxis: {
      type: 'category',
      data: rows.map((item) => item.name),
      axisLabel: {
        interval: 0,
        rotate: rows.length > 4 ? 28 : 0
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 42,
        data: rows.map((item) => item.value)
      }
    ]
  })
  return chart
}

function normalizeStatusRows(rows) {
  return (rows || []).map((item) => ({
    name: item.status || '未设置',
    value: item.count || 0
  }))
}

function emptyGraphic(show, text) {
  return show
    ? {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text,
          fill: '#94a3b8',
          fontSize: 14
        }
      }
    : null
}

function resizeCharts() {
  charts.forEach((chart) => chart?.resize())
}

function disposeCharts() {
  while (charts.length) {
    charts.pop()?.dispose()
  }
}

onMounted(() => {
  fetchDashboardData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.dashboard-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 12px;
}

.metric-card {
  min-height: 112px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.metric-label {
  color: #64748b;
  font-size: 13px;
  line-height: 1.4;
}

.metric-value {
  margin-top: 10px;
  color: #172033;
  font-size: 30px;
  font-weight: 720;
  line-height: 1.1;
}

.metric-note {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
  word-break: break-word;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-panel {
  min-width: 0;
}

.chart-panel-wide {
  grid-column: span 2;
}

.small-chart-panel {
  min-height: 320px;
}

.panel-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 16px;
  font-weight: 650;
  line-height: 1.4;
}

.chart {
  width: 100%;
  height: 320px;
}

.small-chart {
  height: 260px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 12px;
}

.action-item {
  display: flex;
  min-height: 64px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.action-label {
  color: #334155;
  font-size: 14px;
  font-weight: 600;
}

.action-value {
  min-width: 40px;
  text-align: right;
  font-size: 24px;
  font-weight: 720;
}

.action-value.ok {
  color: #2f8f83;
}

.action-value.warning {
  color: #e6a23c;
}

.action-value.danger {
  color: #f56c6c;
}

.action-value.primary {
  color: #5470c6;
}

@media (max-width: 1100px) {
  .summary-grid,
  .chart-grid,
  .action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-panel-wide {
    grid-column: span 2;
  }
}

@media (max-width: 640px) {
  .summary-grid,
  .chart-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .chart-panel-wide {
    grid-column: auto;
  }

  .metric-card {
    min-height: 96px;
  }

  .chart {
    height: 300px;
  }
}
</style>

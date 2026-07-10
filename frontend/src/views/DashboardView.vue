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
/**
 * 页面或模块职责：
 * Dashboard 首页，看板化展示批次、任务、检测结果、NCR 和 CAPA 的统计状态。
 *
 * 路由入口：
 * /。
 *
 * 调用的前端 API：
 * getDashboardSummary、getBatchStatusStats、getDetectionStatusStats、getDefectClassStats、getNcrStatusStats、getCapaStatusStats。
 *
 * 对应后端接口：
 * GET /api/dashboard/* -> DashboardController -> DashboardServiceImpl。
 *
 * 主要交互流程：
 * 页面 mounted -> 并发请求 6 个统计接口 -> summaryCards/actionItems 更新卡片 -> renderCharts 创建 ECharts；
 * 浏览器 resize 时调用 chart.resize；组件销毁时 disposeCharts 防止图表实例泄漏。
 */
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

// 顶部指标卡片从 DashboardSummaryVO 映射而来，note 标明后端统计来源或状态含义。
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

// 待处理提示突出待复核、OPEN NCR 和进行中 CAPA，用于快速识别闭环堵点。
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

// 并发加载 Dashboard 所有统计数据；nextTick 后再渲染图表，确保容器 DOM 尺寸可用。
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

// 重新创建图表实例，确保数据刷新后不会保留旧 series。
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

// 渲染状态分布饼图，rows 来自 StatusCountVO[]。
function renderPieChart(el, rows, emptyText) {
  if (!el) {
    return null
  }

  const data = normalizeStatusRows(rows)
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'item' },
    // legend 展示状态名称，series.data 的 name/value 由后端 status/count 转换。
    legend: {
      bottom: 0,
      type: 'scroll'
    },
    color: ['#2f8f83', '#5470c6', '#e6a23c', '#f56c6c', '#909399', '#67c23a'],
    graphic: emptyGraphic(data.length === 0, emptyText),
    series: [
      {
        // series 是 ECharts 的核心数据系列，此处使用环形饼图表达状态占比。
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

// 渲染柱状图，用于检测状态和缺陷类别数量对比。
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
      // xAxis 使用状态名或缺陷类别名，来自 rows.name。
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
        // series.data 使用 rows.value，即后端 group by count。
        type: 'bar',
        barMaxWidth: 42,
        data: rows.map((item) => item.value)
      }
    ]
  })
  return chart
}

// 将 StatusCountVO 转为 ECharts 所需的 name/value 结构。
function normalizeStatusRows(rows) {
  return (rows || []).map((item) => ({
    name: item.status || '未设置',
    value: item.count || 0
  }))
}

// 无数据时在图表中央展示提示，而不是渲染空坐标系。
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

// 视口变化时同步调整所有图表尺寸。
function resizeCharts() {
  charts.forEach((chart) => chart?.resize())
}

// 销毁 ECharts 实例，避免组件反复进入时占用内存。
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
/* Dashboard 主体：卡片、图表和待处理提示纵向排列。 */
.dashboard-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 汇总指标区：展示 DashboardSummaryVO 转换出的 8 个指标。 */
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

/* 图表网格：承载 ECharts 容器，宽屏两列、窄屏一列。 */
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

/* 图表容器：必须有稳定高度，否则 ECharts 初始化时会拿不到尺寸。 */
.chart {
  width: 100%;
  height: 320px;
}

.small-chart {
  height: 260px;
}

/* 待处理提示区：突出待复核、OPEN NCR 和进行中 CAPA。 */
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

/* 响应式处理：中等屏幕减少列数，保持图表和卡片可读。 */
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

/* 小屏处理：所有统计块单列堆叠。 */
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

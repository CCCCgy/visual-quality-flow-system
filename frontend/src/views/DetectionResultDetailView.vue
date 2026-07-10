<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">检测结果详情</h2>
        <p class="page-subtitle">查看检测图片、bbox 可视化和人工复核入口</p>
      </div>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <div v-loading="loading" class="detail-layout">
      <div class="content-panel">
        <div class="panel-title">基础信息</div>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="检测结果 ID">
            {{ detail.id || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="任务 ID">
            {{ detail.taskId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="图片 ID">
            {{ detail.imageId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="图片名称">
            {{ detail.imageName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="类别">
            {{ detail.className || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="置信度">
            {{ formatConfidence(detail.confidence) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag class="status-tag" :type="statusTagType(detail.status)">
              {{ detail.status || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="bbox 坐标" :span="2">
            {{ bboxText }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ detail.createdTime || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="content-panel visual-panel">
        <div class="visual-header">
          <div>
            <div class="panel-title">图片可视化</div>
            <p class="visual-subtitle">逻辑画布尺寸：1280 x 960</p>
          </div>
          <el-button
            v-if="detail.status === 'PENDING_REVIEW'"
            type="primary"
            @click="openReviewDialog"
          >
            人工复核
          </el-button>
        </div>

        <div class="image-stage">
          <div class="image-canvas">
            <img
              class="demo-image"
              src="/demo-images/demo-ceramic.svg"
              alt="demo ceramic surface"
            />
            <template v-if="hasValidBbox">
              <div class="bbox" :style="bboxStyle"></div>
              <div class="bbox-label" :style="bboxLabelStyle">
                {{ detail.className || 'UNKNOWN' }}
                {{ formatConfidence(detail.confidence) }}
              </div>
            </template>
          </div>
          <el-empty
            v-if="!hasValidBbox"
            class="bbox-empty"
            description="暂无可绘制 bbox 坐标"
          />
        </div>
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
/**
 * 页面或模块职责：
 * 检测结果视觉详情页，展示图片、bbox 红框和人工复核入口。
 *
 * 路由入口：
 * /detections/:id，其中 id 是 detection_result.id。
 *
 * 调用的前端 API：
 * getDetectionVisualDetail、createReview。
 *
 * 对应后端接口：
 * GET /api/detections/{id}/visual-detail -> DetectionController#getDetectionVisualDetail；
 * POST /api/reviews -> ReviewRecordController#createReview。
 *
 * 主要交互流程：
 * route.params.id -> visual-detail 接口 -> detail；
 * bboxX1/bboxY1/bboxX2/bboxY2 按 1280x960 逻辑画布换算为百分比；
 * 复核成功后重新 fetchDetail，让状态和按钮可见性同步更新。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDetectionVisualDetail } from '../api/detectionApi'
import { createReview } from '../api/reviewApi'

const LOGICAL_WIDTH = 1280
const LOGICAL_HEIGHT = 960

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewFormRef = ref()

const detail = ref({})

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

// 将后端 bbox 坐标转换为前端可绘制的矩形，并裁剪到逻辑画布范围内。
const bbox = computed(() => {
  const x1 = toNumber(detail.value.bboxX1)
  const y1 = toNumber(detail.value.bboxY1)
  const x2 = toNumber(detail.value.bboxX2)
  const y2 = toNumber(detail.value.bboxY2)
  if ([x1, y1, x2, y2].some((value) => value === null)) {
    return null
  }

  const left = clamp(Math.min(x1, x2), 0, LOGICAL_WIDTH)
  const top = clamp(Math.min(y1, y2), 0, LOGICAL_HEIGHT)
  const right = clamp(Math.max(x1, x2), 0, LOGICAL_WIDTH)
  const bottom = clamp(Math.max(y1, y2), 0, LOGICAL_HEIGHT)

  if (right <= left || bottom <= top) {
    return null
  }

  return { left, top, width: right - left, height: bottom - top }
})

const hasValidBbox = computed(() => Boolean(bbox.value))

// 用于详情区域展示原始 bbox 坐标。
const bboxText = computed(() => {
  if (!hasValidBbox.value) {
    return '-'
  }
  return [
    formatNumber(detail.value.bboxX1),
    formatNumber(detail.value.bboxY1),
    formatNumber(detail.value.bboxX2),
    formatNumber(detail.value.bboxY2)
  ].join(', ')
})

// 将逻辑坐标转换为百分比，适配响应式图片容器缩放。
const bboxStyle = computed(() => ({
  left: `${(bbox.value.left / LOGICAL_WIDTH) * 100}%`,
  top: `${(bbox.value.top / LOGICAL_HEIGHT) * 100}%`,
  width: `${(bbox.value.width / LOGICAL_WIDTH) * 100}%`,
  height: `${(bbox.value.height / LOGICAL_HEIGHT) * 100}%`
}))

// 标签放在 bbox 上方，并限制在画布可视范围内。
const bboxLabelStyle = computed(() => {
  const left = (bbox.value.left / LOGICAL_WIDTH) * 100
  const top = (bbox.value.top / LOGICAL_HEIGHT) * 100
  return {
    left: `${Math.min(left, 82)}%`,
    top: `${Math.max(top - 5, 1)}%`
  }
})

// 查询 detection_result 与 inspection_image 合并后的视觉详情。
async function fetchDetail() {
  loading.value = true
  try {
    detail.value = await getDetectionVisualDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

// 打开复核弹窗，只允许当前状态仍为 PENDING_REVIEW 时由模板显示入口。
function openReviewDialog() {
  reviewForm.detectionResultId = detail.value.id
  reviewForm.className = detail.value.className
  reviewForm.confidenceText = formatConfidence(detail.value.confidence)
  reviewForm.reviewerId = 2
  reviewForm.reviewResult = 'CONFIRMED_DEFECT'
  reviewForm.reviewComment = ''
  reviewDialogVisible.value = true
}

// 提交复核后刷新详情，使 detection_result.status 与按钮状态同步。
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
    ElMessage.success('复核提交成功')
    reviewDialogVisible.value = false
    await fetchDetail()
  } finally {
    reviewSubmitting.value = false
  }
}

// 返回检测结果列表。
function goBack() {
  router.push('/detections')
}

// 检测结果状态展示色。
function statusTagType(status) {
  const typeMap = {
    PENDING_REVIEW: 'warning',
    CONFIRMED_DEFECT: 'danger',
    FALSE_POSITIVE: 'info',
    NEED_RECHECK: 'primary'
  }
  return typeMap[status] || 'info'
}

// 数字格式化，避免空值在页面上显示为 NaN。
function formatNumber(value) {
  if (value === null || typeof value === 'undefined') {
    return '-'
  }
  return Number(value).toFixed(2)
}

// 置信度格式化。
function formatConfidence(value) {
  if (value === null || typeof value === 'undefined') {
    return '-'
  }
  return Number(value).toFixed(4)
}

// 把后端可能返回的字符串数字转换为 Number。
function toNumber(value) {
  if (value === null || typeof value === 'undefined' || value === '') {
    return null
  }
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

// 将 bbox 坐标限制在逻辑画布内，避免框绘制到图片外。
function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

onMounted(fetchDetail)
</script>

<style scoped>
/* 详情页面布局：基础信息和图片可视化上下排列。 */
.detail-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 16px;
  font-weight: 650;
}

/* 视觉区域头部：左侧标题说明，右侧在待复核时显示人工复核按钮。 */
.visual-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.visual-subtitle {
  margin: -6px 0 0;
  color: #64748b;
  font-size: 13px;
}

/* 图片舞台：提供边框、背景和横向滚动，容纳 bbox 叠加画布。 */
.image-stage {
  position: relative;
  display: flex;
  justify-content: center;
  padding: 16px;
  overflow-x: auto;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

/* bbox 画布：保持 4:3 比例，与 1280x960 逻辑坐标一致。 */
.image-canvas {
  position: relative;
  width: min(100%, 960px);
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border: 1px solid #94a3b8;
  border-radius: 8px;
  background: #ffffff;
}

/* 演示图片：当前使用 public/demo-images 下的 SVG 作为可视化底图。 */
.demo-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* bbox 红框：位置和尺寸由后端 bbox 坐标换算出的百分比控制。 */
.bbox {
  position: absolute;
  border: 3px solid #f56c6c;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.85);
  pointer-events: none;
}

/* bbox 标签：显示 className 和 confidence，跟随 bbox 左上方定位。 */
.bbox-label {
  position: absolute;
  max-width: 180px;
  padding: 4px 8px;
  border-radius: 4px;
  background: rgba(245, 108, 108, 0.95);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.4;
  pointer-events: none;
  word-break: break-word;
}

/* 无有效 bbox 时的覆盖提示。 */
.bbox-empty {
  position: absolute;
  inset: 0;
  background: rgba(248, 250, 252, 0.78);
}

/* 小屏处理：视觉区按钮和标题上下排列，减少画布外边距。 */
@media (max-width: 760px) {
  .visual-header {
    flex-direction: column;
  }

  .image-stage {
    padding: 10px;
  }
}
</style>

<template>
  <section>
    <div class="page-header">
      <div>
        <h2 class="page-title">Dashboard</h2>
        <p class="page-subtitle">工业视觉检测结果进入质量流程后的主链路总览</p>
      </div>
    </div>

    <div class="content-panel">
      <div class="system-title">工业视觉检测结果复核与质量闭环管理系统</div>
      <div class="flow">
        <div v-for="(item, index) in flowItems" :key="item" class="flow-step">
          <div class="step-index">{{ index + 1 }}</div>
          <div class="step-name">{{ item }}</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
const flowItems = [
  '生产批次',
  '检测任务',
  'YOLO JSON 导入',
  '检测结果',
  '人工复核',
  'NCR',
  'CAPA'
]
</script>

<style scoped>
.system-title {
  margin-bottom: 18px;
  color: #172033;
  font-size: 18px;
  font-weight: 650;
}

.flow {
  display: grid;
  grid-template-columns: repeat(7, minmax(96px, 1fr));
  gap: 10px;
}

.flow-step {
  min-height: 94px;
  padding: 14px 10px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.step-index {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: #2f8f83;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.step-name {
  margin-top: 12px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
}

@media (max-width: 1100px) {
  .flow {
    grid-template-columns: repeat(3, minmax(120px, 1fr));
  }
}

@media (max-width: 640px) {
  .flow {
    grid-template-columns: 1fr;
  }
}
</style>

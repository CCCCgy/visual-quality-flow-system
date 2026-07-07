<template>
  <el-container class="app-shell">
    <el-aside class="sidebar" width="232px">
      <div class="brand">
        <div class="brand-mark">VQ</div>
        <div>
          <div class="brand-name">Visual QMS</div>
          <div class="brand-subtitle">质量闭环原型</div>
        </div>
      </div>

      <el-menu
        class="side-menu"
        router
        :default-active="activePath"
        background-color="#172033"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/">Dashboard</el-menu-item>
        <el-menu-item index="/batches">批次管理</el-menu-item>
        <el-menu-item index="/inspection-tasks">检测任务</el-menu-item>
        <el-menu-item index="/detections">检测结果</el-menu-item>
        <el-menu-item index="/reviews" disabled>人工复核</el-menu-item>
        <el-menu-item index="/ncrs" disabled>NCR</el-menu-item>
        <el-menu-item index="/capas" disabled>CAPA</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <h1>工业视觉检测结果复核与质量闭环管理系统</h1>
          <p>YOLO 检测结果进入质量管理流程后的复核、NCR 与 CAPA 闭环</p>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const activePath = computed(() => {
  if (route.path.startsWith('/batches')) {
    return '/batches'
  }
  if (route.path.startsWith('/inspection-tasks')) {
    return '/inspection-tasks'
  }
  if (route.path.startsWith('/detections')) {
    return '/detections'
  }
  return route.path
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f5f7fb;
}

.sidebar {
  min-height: 100vh;
  background: #172033;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 72px;
  padding: 0 18px;
  color: #ffffff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 8px;
  background: #2f8f83;
  color: #ffffff;
  font-weight: 700;
}

.brand-name {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
}

.brand-subtitle {
  margin-top: 3px;
  color: #94a3b8;
  font-size: 12px;
}

.side-menu {
  border-right: 0;
}

.topbar {
  display: flex;
  align-items: center;
  height: 72px;
  padding: 0 28px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.topbar h1 {
  margin: 0;
  color: #172033;
  font-size: 18px;
  font-weight: 650;
  line-height: 1.35;
}

.topbar p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.main-content {
  padding: 24px;
}

@media (max-width: 760px) {
  .app-shell {
    display: block;
  }

  .sidebar {
    width: 100% !important;
    min-height: auto;
  }

  .brand {
    height: 64px;
  }

  .topbar {
    height: auto;
    padding: 16px;
  }

  .main-content {
    padding: 16px;
  }
}
</style>

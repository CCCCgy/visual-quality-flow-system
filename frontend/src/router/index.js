import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import BatchListView from '../views/BatchListView.vue'
import BatchDetailView from '../views/BatchDetailView.vue'
import InspectionTaskListView from '../views/InspectionTaskListView.vue'
import DetectionResultListView from '../views/DetectionResultListView.vue'
import DetectionResultDetailView from '../views/DetectionResultDetailView.vue'
import ReviewListView from '../views/ReviewListView.vue'
import NcrListView from '../views/NcrListView.vue'
import CapaListView from '../views/CapaListView.vue'

/**
 * 页面或模块职责：
 * 定义前端路由与页面组件的对应关系。
 *
 * 路由入口：
 * main.js 注册 router 后，App.vue 的顶层 router-view 渲染本文件匹配到的组件。
 *
 * 调用的前端 API：
 * 本文件不直接调用 API，各页面组件在加载或用户操作时调用 api/*.js。
 *
 * 对应后端接口：
 * 页面组件再通过 batchApi、taskApi、detectionApi、reviewApi、ncrApi、capaApi、dashboardApi 调用后端。
 *
 * 主要交互流程：
 * 根路径进入 MainLayout，MainLayout 内部的 router-view 渲染 Dashboard、批次、任务、检测结果、复核、NCR 和 CAPA 页面。
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          // URL: /；首页看板，展示 Dashboard 统计卡片和 ECharts 图表。
          path: '',
          name: 'Dashboard',
          component: DashboardView
        },
        {
          // URL: /batches；批次列表页，用于查询 production_batch。
          path: 'batches',
          name: 'BatchList',
          component: BatchListView
        },
        {
          // URL: /batches/:id；批次详情页，id 是 production_batch.id，并通过 props 传给页面。
          path: 'batches/:id',
          name: 'BatchDetail',
          component: BatchDetailView,
          props: true
        },
        {
          // URL: /inspection-tasks；检测任务列表页，可按 batchId 过滤 inspection_task。
          path: 'inspection-tasks',
          name: 'InspectionTaskList',
          component: InspectionTaskListView
        },
        {
          // URL: /detections；检测结果列表页，可接收 query.taskId 从批次详情跳入。
          path: 'detections',
          name: 'DetectionResultList',
          component: DetectionResultListView
        },
        {
          // URL: /detections/:id；检测结果视觉详情页，id 是 detection_result.id。
          path: 'detections/:id',
          name: 'DetectionResultDetail',
          component: DetectionResultDetailView,
          props: true
        },
        {
          // URL: /reviews；人工复核列表页，CONFIRMED_DEFECT 记录可继续创建 NCR。
          path: 'reviews',
          name: 'ReviewList',
          component: ReviewListView
        },
        {
          // URL: /ncrs；NCR 列表页，OPEN NCR 可继续创建 CAPA。
          path: 'ncrs',
          name: 'NcrList',
          component: NcrListView
        },
        {
          // URL: /capas；CAPA 列表页，可编辑措施并关闭 CAPA。
          path: 'capas',
          name: 'CapaList',
          component: CapaListView
        }
      ]
    }
  ]
})

export default router

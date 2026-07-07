import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import BatchListView from '../views/BatchListView.vue'
import BatchDetailView from '../views/BatchDetailView.vue'
import InspectionTaskListView from '../views/InspectionTaskListView.vue'
import DetectionResultListView from '../views/DetectionResultListView.vue'
import ReviewListView from '../views/ReviewListView.vue'
import NcrListView from '../views/NcrListView.vue'
import CapaListView from '../views/CapaListView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'Dashboard',
          component: DashboardView
        },
        {
          path: 'batches',
          name: 'BatchList',
          component: BatchListView
        },
        {
          path: 'batches/:id',
          name: 'BatchDetail',
          component: BatchDetailView,
          props: true
        },
        {
          path: 'inspection-tasks',
          name: 'InspectionTaskList',
          component: InspectionTaskListView
        },
        {
          path: 'detections',
          name: 'DetectionResultList',
          component: DetectionResultListView
        },
        {
          path: 'reviews',
          name: 'ReviewList',
          component: ReviewListView
        },
        {
          path: 'ncrs',
          name: 'NcrList',
          component: NcrListView
        },
        {
          path: 'capas',
          name: 'CapaList',
          component: CapaListView
        }
      ]
    }
  ]
})

export default router

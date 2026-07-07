import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layout/MainLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import BatchListView from '../views/BatchListView.vue'
import BatchDetailView from '../views/BatchDetailView.vue'

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
        }
      ]
    }
  ]
})

export default router

import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/global.css'
import App from './App.vue'
import router from './router'

/**
 * 文件职责：
 * Vue 前端应用入口。
 *
 * 系统位置：
 * index.html 通过 <script type="module" src="/src/main.js"> 加载本文件。
 *
 * 注册内容：
 * App 是根组件，router 提供页面路由，ElementPlus 提供表格、表单、弹窗等 UI 组件，
 * global.css 提供跨页面的基础样式。
 *
 * 输出结果：
 * 最终挂载到 index.html 的 #app 节点，由 App.vue 继续渲染 router-view。
 */
createApp(App).use(router).use(ElementPlus).mount('#app')

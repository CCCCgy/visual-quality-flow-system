import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 文件职责：
 * Vite 前端开发与构建配置。
 *
 * 系统位置：
 * npm run dev / npm run build 会读取本文件。
 *
 * 调用关系：
 * 前端 request.js 使用 baseURL=/api；开发服务器收到 /api 请求后，
 * 由 proxy 转发到后端 http://localhost:8081。
 *
 * 注意事项：
 * 代理只作用于 Vite 开发环境；生产部署需要由真实网关或后端服务处理 /api 转发。
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        // 仅开发期代理目标；不改变前端代码中的请求路径。
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})

import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 页面或模块职责：
 * 创建全站复用的 Axios 实例，并统一解析后端 Result 响应结构。
 *
 * 路由入口：
 * 无页面路由；所有 api/*.js 都通过本模块发送请求。
 *
 * 调用的前端 API：
 * batchApi、taskApi、detectionApi、reviewApi、ncrApi、capaApi、dashboardApi 均依赖 request。
 *
 * 对应后端接口：
 * baseURL=/api，开发环境由 vite.config.js 将 /api 代理到 http://localhost:8081。
 *
 * 主要交互流程：
 * 页面组件 -> api/*.js -> request.js -> 后端 Controller -> Result(code/message/data)
 * -> 响应拦截器 code=200 时只返回 data，非 200 时展示 message 并抛错。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 当前项目没有统一鉴权请求头，请求拦截器暂未设置；公共响应处理集中放在这里，避免各 api.js 重复解析 Result。
request.interceptors.response.use(
  (response) => {
    const result = response.data

    // 兼容非 Result 包装的响应；当前业务接口正常都会返回 { code, message, data }。
    if (!result || typeof result.code === 'undefined') {
      return result
    }

    // 后端业务失败时保持 Promise reject，让页面的 finally 仍能关闭 loading。
    if (result.code !== 200) {
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(new Error(result.message || '请求失败'))
    }

    // 成功时剥离统一外壳，页面直接拿到 VO、PageResult 或统计数组。
    return result.data
  },
  (error) => {
    // 网络错误或非 2xx 响应统一展示，具体页面不需要重复写提示逻辑。
    const message =
      error.response?.data?.message || error.message || '网络请求异常'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request

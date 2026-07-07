import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  (response) => {
    const result = response.data

    if (!result || typeof result.code === 'undefined') {
      return result
    }

    if (result.code !== 200) {
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(new Error(result.message || '请求失败'))
    }

    return result.data
  },
  (error) => {
    const message =
      error.response?.data?.message || error.message || '网络请求异常'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request

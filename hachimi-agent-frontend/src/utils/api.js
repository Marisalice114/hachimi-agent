// src/utils/api.js
import axios from 'axios'
import { handleResponse } from './responseHandler'
 
// 从环境变量获取API基础URL，如果没有则使用默认值
const getBaseURL = () => {
  // 开发环境下，如果没有设置环境变量，使用本地地址
  if (import.meta.env.DEV) {
    return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'
  }
  // 生产环境下使用相对路径（通过nginx代理）
  return import.meta.env.VITE_API_BASE_URL || '/api'
}
 
const baseURL = getBaseURL()
console.log('API Base URL:', baseURL)
 
const api = axios.create({
  baseURL: baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})
 
// 请求拦截器
api.interceptors.request.use(
  config => {
    console.log('API请求:', config.method?.toUpperCase(), config.url)
    return config
  },
  error => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)
 
// 响应拦截器 - 添加BaseResponse处理
api.interceptors.response.use(
  response => {
    console.log('API响应:', response.status, response.config.url)
    try {
      // 自动处理BaseResponse结构
      response.data = handleResponse(response.data)
      return response
    } catch (error) {
      console.error('响应处理错误:', error)
      return Promise.reject(error)
    }
  },
  error => {
    console.error('API响应错误:', error.response?.status, error.config?.url, error.message)
    return Promise.reject(error)
  }
)
 
export default api
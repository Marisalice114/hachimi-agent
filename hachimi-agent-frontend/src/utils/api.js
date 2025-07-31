// src/utils/api.js
import axios from 'axios'
import { handleResponse } from './responseHandler'
 
// 从环境变量获取API基础URL，如果没有则使用默认值
const getBaseURL = () => {
  // 开发环境下，如果没有设置环境变量，使用相对路径（通过Vite代理）
  if (import.meta.env.DEV) {
    const configuredUrl = import.meta.env.VITE_API_BASE_URL
    if (configuredUrl) {
      // 如果已经包含/api，直接返回，否则添加
      return configuredUrl.endsWith('/api') ? configuredUrl : `${configuredUrl}/api`
    }
    // 开发环境使用相对路径，Vite会代理到云端
    return '/api'
  }
  // 生产环境下修复路径重复问题
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL
  if (apiBaseUrl) {
    // 如果环境变量已经包含/api，直接返回
    if (apiBaseUrl.endsWith('/api')) {
      return apiBaseUrl
    }
    // 否则添加/api
    return `${apiBaseUrl}/api`
  }
  // 默认使用相对路径
  return '/api'
}
 
const baseURL = getBaseURL()
// console.log('API Base URL:', baseURL) // 已注释：减少生产环境日志
 
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
    // console.log('API请求:', config.method?.toUpperCase(), config.url) // 已注释：减少生产环境日志
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
    // console.log('API响应:', response.status, response.config.url) // 已注释：减少生产环境日志
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
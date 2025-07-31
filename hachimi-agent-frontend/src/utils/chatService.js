// src/utils/chatHistoryService.js
import Cookies from 'js-cookie'
import api from './api'
 
// 获取API基础URL（与api.js保持一致）
const getApiBaseURL = () => {
  if (import.meta.env.DEV) {
    const configuredUrl = import.meta.env.VITE_API_BASE_URL
    if (configuredUrl) {
      return `${configuredUrl}/api`
    }
    // 开发环境使用相对路径，Vite会代理到云端
    return '/api'
  }
  return import.meta.env.VITE_API_BASE_URL ? `${import.meta.env.VITE_API_BASE_URL}/api` : '/api'
}
 
export const chatHistoryService = {
  // 获取所有会话
  async getAllSessions() {
    try {
      const response = await api.get('/chat/sessions')
      // console.log('获取到的会话数据:', response.data) // 已注释：减少生产环境日志
      return response.data || []
    } catch (error) {
      console.error('获取会话列表失败:', error)
      return []
    }
  },
 
  // 获取指定会话的消息
  async getSessionMessages(sessionId) {
    try {
      if (!sessionId) {
        throw new Error('会话ID不能为空')
      }
      const response = await api.get(`/chat/sessions/${sessionId}/messages`)
      // console.log('获取到的消息数据:', response.data) // 已注释：减少生产环境日志
      return response.data || []
    } catch (error) {
      console.error('获取会话消息失败:', error)
      return []
    }
  },
 
  // 获取会话信息
  async getSession(sessionId) {
    try {
      if (!sessionId) {
        throw new Error('会话ID不能为空')
      }
      const response = await api.get(`/chat/sessions/${sessionId}`)
      return response.data || null
    } catch (error) {
      console.error('获取会话信息失败:', error)
      return null
    }
  },
 
  // 删除会话
  async deleteSession(sessionId) {
    try {
      if (!sessionId) {
        throw new Error('会话ID不能为空')
      }
      const response = await api.delete(`/chat/sessions/${sessionId}`)
      // 同时删除自定义名称
      const customNames = this.getCustomSessionNames()
      delete customNames[sessionId]
      Cookies.set('custom_session_names', JSON.stringify(customNames), { expires: 365 })
      return response.data
    } catch (error) {
      console.error('删除会话失败:', error)
      throw error
    }
  },
 
  // 设置自定义会话名称（仅保存在前端Cookie中）
  setCustomSessionName(sessionId, name) {
    const customNames = this.getCustomSessionNames()
    customNames[sessionId] = name
    Cookies.set('custom_session_names', JSON.stringify(customNames), { expires: 365 })
  },
 
  // 获取自定义会话名称
  getCustomSessionNames() {
    const names = Cookies.get('custom_session_names')
    return names ? JSON.parse(names) : {}
  },
 
    // 获取会话的显示名称
  getSessionDisplayName(session) {
    if (!session) return '未知会话'
    
    // 调试：打印会话数据结构（生产环境已注释）
    // console.log('调试会话数据结构:', session)
    
    const customNames = this.getCustomSessionNames()
    
    // 支持多种会话ID字段名（根据新的后端结构）
    const sessionId = session.sessionId || session.conversationId || session.id
    if (!sessionId) return '未知会话'
    
    // 优先使用自定义名称
    const customName = customNames[sessionId]
    if (customName) return customName
    
    // 优先使用首条用户消息作为会话名（如有）
    // 兼容不同结构：session.messages 或 session.lastMessage 或 session.question
    let firstMsg = ''
    
    // 1. 检查是否有消息数组
    if (Array.isArray(session.messages) && session.messages.length > 0) {
      // console.log('会话包含消息:', session.messages.length, '条') // 已注释：减少生产环境日志
      // 查找首条用户消息
      const userMsg = session.messages.find(m => 
        (m.type === 'user' || m.role === 'user' || m.messageType === 'user') && 
        (m.text || m.content || m.message)
      )
      if (userMsg) {
        firstMsg = userMsg.text || userMsg.content || userMsg.message
        // console.log('找到用户消息:', firstMsg) // 已注释：减少生产环境日志
      } else if (session.messages[0]) {
        // 如果没找到用户消息，取第一条消息
        const firstMessage = session.messages[0]
        firstMsg = firstMessage.text || firstMessage.content || firstMessage.message
        // console.log('使用第一条消息:', firstMsg) // 已注释：减少生产环境日志
      }
    }
    
    // 2. 检查其他可能的消息字段
    if (!firstMsg) {
      firstMsg = session.lastMessage || session.question || session.firstUserMessage || session.lastUserMessage
      // if (firstMsg) console.log('使用其他字段:', firstMsg) // 已注释：减少生产环境日志
    }
    
    // 3. 检查会话摘要字段
    if (!firstMsg && session.summary) {
      firstMsg = session.summary
      // console.log('使用摘要字段:', firstMsg) // 已注释：减少生产环境日志
    }
    
    if (firstMsg && typeof firstMsg === 'string') {
      // 截取前20字符，去除换行和空格
      const displayName = firstMsg.replace(/\s+/g, ' ').trim().slice(0, 20) + (firstMsg.length > 20 ? '...' : '')
      // console.log('生成显示名称:', displayName) // 已注释：减少生产环境日志
      return displayName
    }
    
    // 使用会话的默认名称
    if (session.sessionName || session.name || session.title) {
      const defaultName = session.sessionName || session.name || session.title
      // console.log('使用默认名称:', defaultName) // 已注释：减少生产环境日志
      return defaultName
    }
    
    // 生成默认名称
    const generatedName = `会话 ${sessionId.toString().slice(-6)}`
    // console.log('生成默认名称:', generatedName) // 已注释：减少生产环境日志
    return generatedName
  }
}
 
export const generateChatId = () => {
  return Date.now().toString() + Math.random().toString(36).substr(2, 9)
}
 
export const aiChatService = {
  // 获取SSE专用的基础URL - 修复生产环境路径重复问题
  getSseBaseUrl() {
    // 🔧 调试信息（生产环境已注释）
    // console.log('🔍 环境变量调试信息:')
    // console.log('NODE_ENV:', import.meta.env.NODE_ENV)
    // console.log('DEV:', import.meta.env.DEV)
    // console.log('PROD:', import.meta.env.PROD)
    // console.log('VITE_API_BASE_URL:', import.meta.env.VITE_API_BASE_URL)
    // console.log('VITE_SSE_BASE_URL:', import.meta.env.VITE_SSE_BASE_URL)
    
    if (import.meta.env.DEV) {
      // 开发环境：优先使用环境变量中的URL
      const sseUrl = import.meta.env.VITE_SSE_BASE_URL
      if (sseUrl) {
        // console.log('🔗 开发环境使用SSE URL:', `${sseUrl}/api`)
        return `${sseUrl}/api`
      }
      
      const apiUrl = import.meta.env.VITE_API_BASE_URL
      if (apiUrl) {
        // console.log('🔗 开发环境使用API URL:', `${apiUrl}/api`)
        return `${apiUrl}/api`
      }
      
      // 🔧 修复：开发环境默认使用相对路径，通过Vite代理
      // console.log('🔗 开发环境使用Vite代理路径: /api')
      return '/api'
    }
    
    // 🔧 生产环境：修复路径重复问题
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL
    // console.log('🔍 生产环境VITE_API_BASE_URL:', `"${apiBaseUrl}"`)
    
    if (apiBaseUrl) {
      // 如果环境变量已经包含/api，直接返回
      if (apiBaseUrl.endsWith('/api')) {
        // console.log('🔗 生产环境URL已包含/api:', apiBaseUrl)
        return apiBaseUrl
      }
      // 否则添加/api
      // console.log('🔗 生产环境添加/api:', `${apiBaseUrl}/api`)
      return `${apiBaseUrl}/api`
    }
    
    // 默认使用相对路径
    // console.log('🔗 生产环境使用默认相对路径: /api')
    return '/api'
  },

  // 获取API基础URL（用于普通请求）
  getApiBaseUrl() {
    return this.getSseBaseUrl()
  },

  // 启动可中断的恋爱大师聊天 - 返回SSE URL
  getLoveAppSseUrl(message, chatId) {
    const baseUrl = this.getSseBaseUrl()
    
    // 🔧 强制修正：防止路径重复
    let finalUrl
    if (baseUrl === '/api') {
      // 相对路径情况
      finalUrl = `/api/ai/love_app/chat/sse/emitter?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`
    } else if (baseUrl.endsWith('/api')) {
      // 绝对路径且已包含/api
      finalUrl = `${baseUrl}/ai/love_app/chat/sse/emitter?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`
    } else {
      // 绝对路径但不包含/api
      finalUrl = `${baseUrl}/api/ai/love_app/chat/sse/emitter?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`
    }
    
    // 🔧 二次检查：移除任何路径重复
    finalUrl = finalUrl.replace(/\/api\/api\//g, '/api/')
    
    // 生产环境已注释调试信息
    // console.log('🔗 SSE URL构建过程:')
    // console.log('  Base URL:', baseUrl)
    // console.log('  Final URL:', finalUrl)
    
    return finalUrl
  },

  // 启动可中断的Manus聊天
  async getManusSseConnection(message) {
    try {
      const baseUrl = this.getSseBaseUrl()
      console.log('🔗 Manus base URL:', baseUrl)
      
      const response = await fetch(`${baseUrl}/ai/manus/chat/interruptible?message=${encodeURIComponent(message)}`, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const result = await response.json();
      
      if (result.code !== 0) {
        throw new Error(result.message || '启动聊天失败');
      }
      
      return `${baseUrl}/ai/manus/chat/sse/stream?message=${encodeURIComponent(message)}`;
      
    } catch (error) {
      console.error('获取Manus SSE连接失败:', error);
      throw error;
    }
  },

  // 直接获取SSE URL（备用方案）
  getManusSseUrl(message) {
    const baseUrl = this.getSseBaseUrl()
    
    // 🔧 强制修正：防止路径重复
    let finalUrl
    if (baseUrl === '/api') {
      finalUrl = `/api/ai/manus/chat/sse/stream?message=${encodeURIComponent(message)}`
    } else if (baseUrl.endsWith('/api')) {
      finalUrl = `${baseUrl}/ai/manus/chat/sse/stream?message=${encodeURIComponent(message)}`
    } else {
      finalUrl = `${baseUrl}/api/ai/manus/chat/sse/stream?message=${encodeURIComponent(message)}`
    }
    
    // 🔧 二次检查：移除任何路径重复
    finalUrl = finalUrl.replace(/\/api\/api\//g, '/api/')
    
    console.log('🔗 Manus SSE URL:', finalUrl)
    return finalUrl
  },

  // 测试方法：验证代理是否工作
  async testProxy() {
    try {
      console.log('🧪 测试Vite代理连接...')
      const response = await fetch('/api/health', {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        }
      })
      
      if (response.ok) {
        const data = await response.text()
        console.log('✅ 代理连接成功:', data)
        return true
      } else {
        console.error('❌ 代理连接失败:', response.status, response.statusText)
        return false
      }
    } catch (error) {
      console.error('❌ 代理连接测试失败:', error)
      return false
    }
  },

  // 测试SSE连接
  testSseConnection(message = 'test', chatId = 'test123') {
    console.log('🧪 测试SSE连接...')
    const url = this.getLoveAppSseUrl(message, chatId)
    
    console.log('🔗 创建SSE连接:', url)
    
    const eventSource = new EventSource(url)
    
    eventSource.onopen = () => {
      console.log('✅ SSE连接已打开')
      eventSource.close()
    }
    
    eventSource.onerror = (error) => {
      console.error('❌ SSE连接失败:', error)
      console.error('❌ EventSource readyState:', eventSource.readyState)
      console.error('❌ EventSource URL:', eventSource.url)
      eventSource.close()
    }
    
    eventSource.onmessage = (event) => {
      console.log('📨 收到SSE消息:', event.data)
      eventSource.close()
    }
    
    // 10秒后自动关闭测试连接
    setTimeout(() => {
      if (eventSource.readyState !== EventSource.CLOSED) {
        console.log('⏰ 测试超时，关闭连接')
        eventSource.close()
      }
    }, 10000)
    
    return eventSource
  },

  // 其他方法保持不变...
  async stopChatStream(streamId) {
    try {
      if (!streamId) {
        throw new Error('Stream ID不能为空')
      }
      const response = await api.post(`/ai/chat/stop/${streamId}`)
      return response.data
    } catch (error) {
      console.error('停止AI聊天流失败:', error)
      throw error
    }
  },

  async startLoveAppStream(message, chatId) {
    try {
      if (!message || !chatId) {
        throw new Error('消息和会话ID都不能为空')
      }
      const response = await api.get('/ai/love_app/chat/sse/interruptible', {
        params: { message, chatId }
      })
      return response.data
    } catch (error) {
      console.error('启动恋爱大师聊天失败:', error)
      throw error
    }
  },

  async startManusStream(message) {
    try {
      if (!message) {
        throw new Error('消息不能为空')
      }
      const response = await api.get('/ai/manus/chat/interruptible', {
        params: { message }
      })
      return response.data
    } catch (error) {
      console.error('启动Manus聊天失败:', error)
      throw error
    }
  }
}
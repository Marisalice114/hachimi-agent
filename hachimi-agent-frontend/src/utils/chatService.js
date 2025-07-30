// src/utils/chatHistoryService.js
import Cookies from 'js-cookie'
import api from './api'
 
// 获取API基础URL（与api.js保持一致）
const getApiBaseURL = () => {
  if (import.meta.env.DEV) {
    return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'
  }
  return import.meta.env.VITE_API_BASE_URL || '/api'
}
 
export const chatHistoryService = {
  // 获取所有会话
  async getAllSessions() {
    try {
      const response = await api.get('/chat/sessions')
      console.log('获取到的会话数据:', response.data)
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
      console.log('获取到的消息数据:', response.data)
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
    
    // 调试：打印会话数据结构
    console.log('调试会话数据结构:', session)
    
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
      console.log('会话包含消息:', session.messages.length, '条')
      // 查找首条用户消息
      const userMsg = session.messages.find(m => 
        (m.type === 'user' || m.role === 'user' || m.messageType === 'user') && 
        (m.text || m.content || m.message)
      )
      if (userMsg) {
        firstMsg = userMsg.text || userMsg.content || userMsg.message
        console.log('找到用户消息:', firstMsg)
      } else if (session.messages[0]) {
        // 如果没找到用户消息，取第一条消息
        const firstMessage = session.messages[0]
        firstMsg = firstMessage.text || firstMessage.content || firstMessage.message
        console.log('使用第一条消息:', firstMsg)
      }
    }
    
    // 2. 检查其他可能的消息字段
    if (!firstMsg) {
      firstMsg = session.lastMessage || session.question || session.firstUserMessage || session.lastUserMessage
      if (firstMsg) console.log('使用其他字段:', firstMsg)
    }
    
    // 3. 检查会话摘要字段
    if (!firstMsg && session.summary) {
      firstMsg = session.summary
      console.log('使用摘要字段:', firstMsg)
    }
    
    if (firstMsg && typeof firstMsg === 'string') {
      // 截取前20字符，去除换行和空格
      const displayName = firstMsg.replace(/\s+/g, ' ').trim().slice(0, 20) + (firstMsg.length > 20 ? '...' : '')
      console.log('生成显示名称:', displayName)
      return displayName
    }
    
    // 使用会话的默认名称
    if (session.sessionName || session.name || session.title) {
      const defaultName = session.sessionName || session.name || session.title
      console.log('使用默认名称:', defaultName)
      return defaultName
    }
    
    // 生成默认名称
    const generatedName = `会话 ${sessionId.toString().slice(-6)}`
    console.log('生成默认名称:', generatedName)
    return generatedName
  }
}
 
export const generateChatId = () => {
  return Date.now().toString() + Math.random().toString(36).substr(2, 9)
}
 
// 新增：AI聊天服务
export const aiChatService = {
  // 获取API基础URL
  getApiBaseUrl() {
    if (import.meta.env.DEV) {
      // 在开发环境中，如果设置了VITE_API_BASE_URL，直接使用
      // 否则使用默认的后端地址
      const configuredUrl = import.meta.env.VITE_API_BASE_URL
      if (configuredUrl && configuredUrl.startsWith('/')) {
        // 相对路径，使用当前域名
        return window.location.origin
      }
      return configuredUrl || 'http://localhost:3000'
    }
    return import.meta.env.VITE_API_BASE_URL || ''
  },

  // 启动可中断的恋爱大师聊天 - 直接返回SSE URL
  getLoveAppSseUrl(message, chatId) {
    const baseUrl = this.getApiBaseUrl()
    return `${baseUrl}/api/ai/love_app/chat/sse/emitter?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`
  },

  // 启动可中断的Manus聊天 - 修改为先获取SSE连接
  async getManusSseConnection(message) {
    try {
      const baseUrl = this.getApiBaseUrl()
      const response = await fetch(`${baseUrl}/api/ai/manus/chat/interruptible?message=${encodeURIComponent(message)}`, {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        }
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      const result = await response.json();
      
      if (result.code !== 0) {
        throw new Error(result.message || '启动聊天失败');
      }
      
      // 返回SSE URL，这里我们需要从后端获取真正的SSE流地址
      // 由于Spring Boot的SseEmitter在包装后不能直接使用，我们需要另一种方式
      return `${baseUrl}/api/ai/manus/chat/sse/stream?message=${encodeURIComponent(message)}`;
      
    } catch (error) {
      console.error('获取Manus SSE连接失败:', error);
      throw error;
    }
  },

  // 直接获取SSE URL（备用方案）
  getManusSseUrl(message) {
    const baseUrl = this.getApiBaseUrl()
    return `${baseUrl}/api/ai/manus/chat/sse/stream?message=${encodeURIComponent(message)}`
  },

  // 停止AI聊天流
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

  // 启动可中断的恋爱大师聊天
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

  // 启动可中断的Manus聊天
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
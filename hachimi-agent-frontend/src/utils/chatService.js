// src/utils/chatHistoryService.js
import Cookies from 'js-cookie'

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
      const apiUrl = `${getApiBaseURL()}/chat/sessions`
      console.log('准备请求会话列表:', apiUrl)
      
      const response = await fetch(apiUrl)
      console.log('会话列表响应状态:', response.status, response.statusText)
      
      if (response.ok) {
        const data = await response.json()
        console.log('获取到的会话数据:', data)
        return data
      } else {
        // 尝试读取错误响应
        const errorText = await response.text()
        console.warn('获取会话列表失败:', response.status, response.statusText, errorText)
        return []
      }
    } catch (error) {
      console.error('获取会话列表失败:', error)
      return []
    }
  },

  // 获取指定会话的消息
  async getSessionMessages(sessionId) {
    try {
      const apiUrl = `${getApiBaseURL()}/chat/sessions/${sessionId}/messages`
      console.log('准备请求会话消息:', apiUrl)
      
      const response = await fetch(apiUrl)
      if (response.ok) {
        const data = await response.json()
        console.log('获取到的消息数据:', data)
        return data
      } else {
        console.warn('获取会话消息失败:', response.status)
        return []
      }
    } catch (error) {
      console.error('获取会话消息失败:', error)
      return []
    }
  },

  // 获取会话信息
  async getSession(sessionId) {
    try {
      const apiUrl = `${getApiBaseURL()}/chat/sessions/${sessionId}`
      const response = await fetch(apiUrl)
      return await response.json()
    } catch (error) {
      console.error('获取会话信息失败:', error)
      return null
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
    const customNames = this.getCustomSessionNames()
    
    // 支持多种会话ID字段名（根据新的后端结构）
    const sessionId = session.sessionId || session.conversationId || session.id
    if (!sessionId) return '未知会话'
    
    // 优先使用自定义名称
    const customName = customNames[sessionId]
    if (customName) return customName
    
    // 使用会话的默认名称
    if (session.sessionName) return session.sessionName
    
    // 生成默认名称
    return `会话 ${sessionId.slice(-6)}`
  },

  // 删除会话
  async deleteSession(sessionId) {
    try {
      const apiUrl = `${getApiBaseURL()}/chat/sessions/${sessionId}`
      await fetch(apiUrl, {
        method: 'DELETE'
      })
      // 同时删除自定义名称
      const customNames = this.getCustomSessionNames()
      delete customNames[sessionId]
      Cookies.set('custom_session_names', JSON.stringify(customNames), { expires: 365 })
    } catch (error) {
      console.error('删除会话失败:', error)
    }
  }
}

export const generateChatId = () => {
  return Date.now().toString() + Math.random().toString(36).substr(2, 9)
}
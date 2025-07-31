<template>
  <div class="container">
    <!-- 动态背景 -->
    <div class="animated-background">
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
        <div class="shape shape-5"></div>
        <div class="shape shape-6"></div>
      </div>
      <div class="particles">
        <div class="particle" v-for="n in 50" :key="n"></div>
      </div>
    </div>
    
    <!-- 移动端菜单按钮 -->
    <button class="mobile-menu-btn" @click="toggleSidebar" aria-label="切换菜单">
      <span class="hamburger"></span>
      <span class="hamburger"></span>
      <span class="hamburger"></span>
    </button>
    
    <!-- 移动端遮罩层 -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="toggleSidebar"></div>
    
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-header">
        <button @click="createNewChat" class="new-chat-btn">
          + 新建对话
        </button>
      </div>
      
      <div class="chat-history">
        <div 
          v-for="session in sessions" 
          :key="session.sessionId || session.conversationId || session.id"
          @click="selectSession(session.sessionId || session.conversationId || session.id)"
          :class="['chat-item', { active: currentChatId === (session.sessionId || session.conversationId || session.id) }]"
        >
          <div v-if="editingSessionId === (session.sessionId || session.conversationId || session.id)" @click.stop="">
            <input 
              v-model="editingSessionName"
              @blur="saveSessionName"
              @keyup.enter="saveSessionName"
              @keyup.esc="cancelEdit"
              class="session-name-input"
              ref="sessionNameInput"
            />
          </div>
          <div v-else @dblclick="startEditSessionName(session)" class="session-content">
            <span class="session-name">{{ getSessionDisplayName(session) }}</span>
            <button @click.stop="deleteSession(session)" class="delete-btn" title="删除会话">×</button>
          </div>
        </div>
      </div>
      
      <div style="padding: 12px; border-top: 1px solid #343541;">
        <router-link to="/" class="new-chat-btn" style="text-decoration: none; display: block; text-align: center;">
          返回主页
        </router-link>
      </div>
    </div>
 
    <!-- 主要内容区域 -->
    <div class="main-content">
      <div class="chat-container">
        <!-- 聊天消息区域 -->
        <div class="chat-messages" ref="messagesContainer">
          <div 
            v-for="message in messages" 
            :key="message.id"
            :class="['message', message.messageType]"
          >
            <div class="message-avatar">
              {{ message.messageType === 'user' ? 'U' : 'AI' }}
            </div>
            <div class="message-content" v-html="parseMarkdown(message.content)"></div>
          </div>
          
          <!-- 正在输入的AI回复 -->
          <div v-if="isTyping" class="message ai">
            <div class="message-avatar">AI</div>
            <div class="message-content" v-html="parseMarkdown(currentAiMessage)"></div>
          </div>
        </div>
 
        <!-- 输入区域 -->
        <div class="chat-input-container">
          <div class="chat-input-wrapper">
            <!-- 停止按钮 -->
            <button 
              v-if="isLoading && currentStreamId"
              @click="stopChatStream"
              class="stop-button"
              title="停止写作指导"
            >
              ⏹
            </button>
            
            <textarea
              v-model="inputMessage"
              @keyup.enter="handleKeyUp"
              :disabled="isLoading"
              placeholder="请描述您的写作问题或分享您的创作困惑..."
              class="chat-input"
              rows="1"
              ref="inputTextarea"
            ></textarea>
            <button 
              @click="sendMessage"
              :disabled="!inputMessage.trim() || isLoading"
              class="send-button"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { chatHistoryService, generateChatId, aiChatService } from '@/utils/chatService'
import { marked } from 'marked'

export default {
  name: 'WritingApp',
  data() {
    return {
      currentChatId: null,
      inputMessage: '',
      messages: [],
      sessions: [],
      isLoading: false,
      isTyping: false,
      currentAiMessage: '',
      editingSessionId: null,
      editingSessionName: '',
      sidebarOpen: false,
      currentStreamId: null,
      eventSource: null,
      sseTimeout: null
    }
  },
  
  async mounted() {
    // 配置marked选项
    marked.setOptions({
      breaks: true, // 支持换行
      gfm: true, // 支持GitHub风格Markdown
      sanitize: false, // 允许HTML（注意：生产环境可能需要sanitize）
      headerIds: false, // 禁用标题ID生成
      mangle: false, // 禁用邮箱地址混淆
      // 启用链接解析
      renderer: new marked.Renderer()
    })
    
    // 自定义链接渲染器，确保链接在新窗口打开
    const renderer = new marked.Renderer()
    renderer.link = function(href, title, text) {
      const titleAttr = title ? ` title="${title}"` : ''
      return `<a href="${href}"${titleAttr} target="_blank" rel="noopener noreferrer">${text}</a>`
    }
    marked.setOptions({ renderer })
    
    this.updateMetaTags()
    await this.loadSessions()
    this.createNewChat()
  },
  
  beforeUnmount() {
    this.closeEventSource()
  },
  
  methods: {
    // Markdown解析方法
    parseMarkdown(content) {
      if (!content) return ''
      try {
        return marked(content)
      } catch (error) {
        console.error('Markdown解析错误:', error)
        return content // 如果解析失败，返回原文本
      }
    },
    
    updateMetaTags() {
      document.title = 'AI写作助手 - Hachimi Agent'
      this.updateMetaTag('description', 'AI写作助手提供专业的写作指导和创作建议，帮助您解决写作中的各种问题，获得个性化的写作指导')
      this.updateMetaTag('keywords', 'AI写作助手,写作指导,创作建议,文学创作,写作培训,文案写作,小说写作,散文写作')
      this.updateMetaProperty('og:title', 'AI写作助手 - 专业写作指导服务')
      this.updateMetaProperty('og:description', '获得AI写作助手的专业创作建议，解决您的写作困惑')
    },
    
    updateMetaTag(name, content) {
      let meta = document.querySelector(`meta[name="${name}"]`)
      if (!meta) {
        meta = document.createElement('meta')
        meta.name = name
        document.head.appendChild(meta)
      }
      meta.content = content
    },
    
    updateMetaProperty(property, content) {
      let meta = document.querySelector(`meta[property="${property}"]`)
      if (!meta) {
        meta = document.createElement('meta')
        meta.setAttribute('property', property)
        document.head.appendChild(meta)
      }
      meta.content = content
    },
    
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    },
    
    createNewChat() {
      this.currentChatId = generateChatId()
      this.messages = []
      this.inputMessage = ''
    },
    
    async loadSessions() {
      try {
        this.sessions = await chatHistoryService.getAllSessions()
        console.log('写作助手 加载的会话数量:', this.sessions.length)
      } catch (error) {
        console.error('加载会话列表失败:', error)
        this.sessions = []
      }
    },
    
    async selectSession(sessionId) {
      console.log('选择会话:', sessionId)
      this.currentChatId = sessionId
      try {
        const rawMessages = await chatHistoryService.getSessionMessages(sessionId)
        console.log('原始消息数据:', rawMessages)
        
        this.messages = rawMessages.map(msg => {
          const processedMsg = {
            id: msg.id || Date.now() + Math.random(),
            content: msg.content || msg.message || '',
            timestamp: msg.timestamp || msg.createTime || new Date(),
            messageType: this.normalizeMessageType(msg)
          }
          
          console.log('处理后的消息:', processedMsg)
          return processedMsg
        })
        
        console.log('加载的消息数量:', this.messages.length)
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      } catch (error) {
        console.error('加载会话消息失败:', error)
        this.messages = []
      }
    },
    
    normalizeMessageType(msg) {
      const type = msg.messageType || msg.type || msg.role || msg.sender
      
      if (!type) {
        console.warn('消息缺少类型信息，默认为ai:', msg)
        return 'ai'
      }
      
      if (type.toLowerCase().includes('user') || 
          type.toLowerCase().includes('human') ||
          type === 'USER' ||
          type === 'user') {
        return 'user'
      }
      
      if (type.toLowerCase().includes('ai') || 
          type.toLowerCase().includes('assistant') ||
          type.toLowerCase().includes('system') ||
          type === 'AI' ||
          type === 'ai' ||
          type === 'assistant') {
        return 'ai'
      }
      
      console.warn('未识别的消息类型:', type, '默认为ai')
      return 'ai'
    },
    
    getSessionDisplayName(session) {
      return chatHistoryService.getSessionDisplayName(session)
    },
    
    startEditSessionName(session) {
      this.editingSessionId = session.sessionId || session.conversationId || session.id
      this.editingSessionName = this.getSessionDisplayName(session)
      this.$nextTick(() => {
        const input = this.$refs.sessionNameInput
        if (input && input.length > 0) {
          input[0].focus()
          input[0].select()
        }
      })
    },
    
    saveSessionName() {
      if (this.editingSessionName.trim()) {
        chatHistoryService.setCustomSessionName(this.editingSessionId, this.editingSessionName.trim())
      }
      this.editingSessionId = null
      this.editingSessionName = ''
    },
    
    cancelEdit() {
      this.editingSessionId = null
      this.editingSessionName = ''
    },
    
    async deleteSession(session) {
      try {
        const sessionId = session.sessionId || session.conversationId || session.id;
        await chatHistoryService.deleteSession(sessionId);
        
        this.sessions = this.sessions.filter(s => {
          const currentSessionId = s.sessionId || s.conversationId || s.id;
          return currentSessionId !== sessionId;
        });
        
        if (this.currentChatId === sessionId) {
          this.createNewChat();
        }
        
        console.log('会话删除成功');
      } catch (error) {
        console.error('删除会话失败:', error);
      }
    },
    
    handleKeyUp(event) {
      if (!event.shiftKey) {
        event.preventDefault()
        this.sendMessage()
      }
    },
    
    async sendMessage() {
      if (!this.inputMessage.trim() || this.isLoading) return
      
      const isNewChat = !this.sessions.some(
        s => (s.sessionId || s.conversationId || s.id) === this.currentChatId
      );

      const message = this.inputMessage.trim()
      this.inputMessage = ''
      
      this.messages.push({
        id: Date.now(),
        content: message,
        messageType: 'user',
        timestamp: new Date()
      })
      
      if (isNewChat) {
        const snippet = message.length > 20 ? message.substring(0, 20) + '...' : message;
        chatHistoryService.setCustomSessionName(this.currentChatId, snippet);
        
        this.sessions.unshift({
          id: this.currentChatId,
          sessionId: this.currentChatId,
          conversationId: this.currentChatId,
        });
      }

      this.scrollToBottom()
      this.isLoading = true
      this.isTyping = true
      this.currentAiMessage = ''
      this.currentStreamId = null
      
      try {
        const sseUrl = aiChatService.getLoveAppSseUrl(message, this.currentChatId)
        console.log('准备建立SSE连接，URL:', sseUrl)
        this.establishSSEConnection(sseUrl)
        
      } catch (error) {
        console.error('发送消息失败:', error)
        this.handleSseError('发送消息失败: ' + (error.message || '未知错误'))
      }
    },
    
    establishSSEConnection(sseUrl) {
      this.closeEventSource()
      
      console.log('建立SSE连接:', sseUrl)
      this.eventSource = new EventSource(sseUrl)
      
      this.currentStreamId = 'connecting-' + Date.now()
      this.resetSseTimeout()
      
      this.eventSource.onopen = () => {
        console.log('SSE connection opened')
      }
      
      this.eventSource.onmessage = (event) => {
        console.log('收到默认SSE消息:', event.data)
        if (event.data && event.data.trim()) {
          this.currentAiMessage += event.data
          this.scrollToBottom()
        }
      }
      
      this.eventSource.onerror = (event) => {
        console.error('SSE connection error:', event)
        if (this.eventSource && this.eventSource.readyState === EventSource.CLOSED) {
          if (this.currentAiMessage && this.currentAiMessage.trim().length > 0) {
            console.log('检测到有AI回复内容，判断为正常完成')
            this.handleSseComplete()
          } else {
            this.handleSseError('连接意外关闭，请重试')
          }
        }
      }
      
      this.eventSource.addEventListener('stream_info', (event) => {
        try {
          const info = JSON.parse(event.data)
          this.currentStreamId = info.streamId
          console.log('收到stream_info，streamId:', info.streamId)
        } catch (e) {
          console.error('解析stream_info失败:', e, 'data:', event.data)
        }
      })
      
      this.eventSource.addEventListener('data', (event) => {
        console.log('收到data事件:', event.data)
        this.currentAiMessage += event.data
        this.scrollToBottom()
        this.resetSseTimeout()
      })
      
      this.eventSource.addEventListener('complete', (event) => {
        console.log('收到complete事件:', event.data)
        this.clearSseTimeout()
        this.handleSseComplete()
      })
      
      this.eventSource.addEventListener('error', (event) => {
        console.error('收到error事件:', event.data)
        this.handleSseError(event.data || '写作指导处理失败')
      })
    },
    
    async stopChatStream() {
      if (!this.currentStreamId) {
        console.warn('没有活动的写作指导流可以停止')
        return
      }
      
      try {
        await aiChatService.stopChatStream(this.currentStreamId)
        console.log('成功停止写作指导流:', this.currentStreamId)
        this.handleSseComplete()
      } catch (error) {
        console.error('停止写作指导流失败:', error)
        this.handleSseError('停止写作指导失败: ' + (error.message || '未知错误'))
      }
    },
    
    closeEventSource() {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }
      this.clearSseTimeout()
    },
    
    resetSseTimeout() {
      this.clearSseTimeout()
      this.sseTimeout = setTimeout(() => {
        console.log('SSE数据接收超时，自动完成')
        if (this.currentAiMessage && this.currentAiMessage.trim().length > 0) {
          this.handleSseComplete()
        } else {
          this.handleSseError('数据接收超时')
        }
      }, 60000) // 增加到60秒，适应AI生成时间
    },
    
    clearSseTimeout() {
      if (this.sseTimeout) {
        clearTimeout(this.sseTimeout)
        this.sseTimeout = null
      }
    },
    
    handleSseError(errorMessage) {
      console.log('处理SSE错误，当前写作指导消息长度:', this.currentAiMessage.length)
      
      const hasAiContent = this.currentAiMessage && this.currentAiMessage.trim().length > 0
      
      if (hasAiContent) {
        console.log('检测到写作指导内容，保存并显示')
        this.messages.push({
          id: Date.now(),
          content: this.currentAiMessage.trim(),
          messageType: 'ai',
          timestamp: new Date()
        })
      }
      
      this.isLoading = false
      this.isTyping = false
      this.currentAiMessage = ''
      this.currentStreamId = null
      this.closeEventSource()
      
      if (!hasAiContent && errorMessage) {
        this.messages.push({
          id: Date.now(),
          content: `❌ 错误: ${errorMessage}`,
          messageType: 'ai',
          timestamp: new Date()
        })
      } else if (hasAiContent) {
        console.log('已保存写作指导，不显示错误消息')
        setTimeout(() => {
          this.loadSessions()
        }, 1000)
      }
      
      this.scrollToBottom()
    },
    
    handleSseComplete() {
      console.log('处理SSE完成，当前写作指导消息长度:', this.currentAiMessage.length)
      
      this.clearSseTimeout()
      
      if (this.currentAiMessage && this.currentAiMessage.trim()) {
        this.messages.push({
          id: Date.now(),
          content: this.currentAiMessage.trim(),
          messageType: 'ai',
          timestamp: new Date()
        })
        console.log('写作指导消息已添加到消息列表')
      } else {
        console.warn('没有写作指导内容，可能是连接异常结束')
      }
      
      this.isLoading = false
      this.isTyping = false
      this.currentAiMessage = ''
      this.currentStreamId = null
      this.closeEventSource()
      this.scrollToBottom()
      
      setTimeout(() => {
        this.loadSessions()
      }, 1000)
    },
    
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    }
  }
}
</script>

<style scoped>
/* 容器样式 */
.container {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #0c0c0c 0%, #1a1a2e 50%, #16213e 100%);
  overflow: hidden;
  display: flex;
  height: 100vh;
}

/* 动态背景样式 */
.animated-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

/* 浮动形状 */
.floating-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(45deg, transparent, rgba(120, 119, 198, 0.3));
  animation: float 20s infinite linear;
}

.shape-1 {
  width: 80px;
  height: 80px;
  top: 20%;
  left: 10%;
  animation-delay: 0s;
  background: linear-gradient(45deg, rgba(255, 107, 107, 0.2), transparent);
}

.shape-2 {
  width: 120px;
  height: 120px;
  top: 60%;
  left: 80%;
  animation-delay: -5s;
  background: linear-gradient(45deg, rgba(74, 144, 226, 0.2), transparent);
}

.shape-3 {
  width: 60px;
  height: 60px;
  top: 80%;
  left: 20%;
  animation-delay: -10s;
  background: linear-gradient(45deg, rgba(129, 236, 236, 0.2), transparent);
}

.shape-4 {
  width: 100px;
  height: 100px;
  top: 10%;
  left: 70%;
  animation-delay: -15s;
  background: linear-gradient(45deg, rgba(250, 177, 160, 0.2), transparent);
}

.shape-5 {
  width: 140px;
  height: 140px;
  top: 40%;
  left: 5%;
  animation-delay: -7s;
  background: linear-gradient(45deg, rgba(168, 85, 247, 0.2), transparent);
}

.shape-6 {
  width: 90px;
  height: 90px;
  top: 70%;
  left: 60%;
  animation-delay: -12s;
  background: linear-gradient(45deg, rgba(34, 197, 94, 0.2), transparent);
}

@keyframes float {
  0% {
    transform: translateY(0px) rotate(0deg);
    opacity: 0.7;
  }
  33% {
    transform: translateY(-30px) rotate(120deg);
    opacity: 1;
  }
  66% {
    transform: translateY(-60px) rotate(240deg);
    opacity: 0.7;
  }
  100% {
    transform: translateY(0px) rotate(360deg);
    opacity: 0.7;
  }
}

/* 粒子效果 - 增强版本，更亮更明显 */
.particles {
  position: absolute;
  width: 100%;
  height: 100%;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: radial-gradient(circle, rgba(255, 255, 255, 1) 0%, rgba(255, 255, 255, 0.3) 70%);
  border-radius: 50%;
  animation: particleFloat 15s infinite linear;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.8);
}

.particle:nth-child(odd) {
  animation-delay: -7.5s;
  background: radial-gradient(circle, rgba(120, 119, 198, 1) 0%, rgba(120, 119, 198, 0.3) 70%);
  box-shadow: 0 0 8px rgba(120, 119, 198, 0.8);
}

.particle:nth-child(3n) {
  width: 3px;
  height: 3px;
  animation-delay: -5s;
  background: radial-gradient(circle, rgba(74, 144, 226, 1) 0%, rgba(74, 144, 226, 0.3) 70%);
  box-shadow: 0 0 8px rgba(74, 144, 226, 0.8);
}

.particle:nth-child(4n) {
  width: 5px;
  height: 5px;
  animation-delay: -2.5s;
  background: radial-gradient(circle, rgba(168, 85, 247, 1) 0%, rgba(168, 85, 247, 0.3) 70%);
  box-shadow: 0 0 8px rgba(168, 85, 247, 0.8);
}

.particle:nth-child(5n) {
  animation-delay: -10s;
  background: radial-gradient(circle, rgba(34, 197, 94, 1) 0%, rgba(34, 197, 94, 0.3) 70%);
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.8);
}

@keyframes particleFloat {
  0% {
    transform: translateY(100vh) translateX(0px);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100px) translateX(100px);
    opacity: 0;
  }
}

/* 为每个粒子随机位置 */
.particle:nth-child(1) { left: 10%; animation-delay: -1s; }
.particle:nth-child(2) { left: 20%; animation-delay: -2s; }
.particle:nth-child(3) { left: 30%; animation-delay: -3s; }
.particle:nth-child(4) { left: 40%; animation-delay: -4s; }
.particle:nth-child(5) { left: 50%; animation-delay: -5s; }
.particle:nth-child(6) { left: 60%; animation-delay: -6s; }
.particle:nth-child(7) { left: 70%; animation-delay: -7s; }
.particle:nth-child(8) { left: 80%; animation-delay: -8s; }
.particle:nth-child(9) { left: 90%; animation-delay: -9s; }
.particle:nth-child(10) { left: 15%; animation-delay: -10s; }
.particle:nth-child(11) { left: 25%; animation-delay: -11s; }
.particle:nth-child(12) { left: 35%; animation-delay: -12s; }
.particle:nth-child(13) { left: 45%; animation-delay: -13s; }
.particle:nth-child(14) { left: 55%; animation-delay: -14s; }
.particle:nth-child(15) { left: 65%; animation-delay: -15s; }
.particle:nth-child(16) { left: 75%; animation-delay: -16s; }
.particle:nth-child(17) { left: 85%; animation-delay: -17s; }
.particle:nth-child(18) { left: 95%; animation-delay: -18s; }
.particle:nth-child(19) { left: 5%; animation-delay: -19s; }
.particle:nth-child(20) { left: 12%; animation-delay: -20s; }

/* 确保其他元素在背景之上 */
.sidebar, .main-content, .mobile-menu-btn {
  position: relative;
  z-index: 10;
}

/* 基础布局 */
.sidebar {
  width: clamp(240px, 25vw, 320px);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(10px);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(5px);
}

/* 输入容器样式 */
.chat-input-container {
  padding: 20px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

/* 页面布局样式 */
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

/* 聊天输入区域 - 新的Flex布局 */
.chat-input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  padding: 8px 12px;
  position: relative;
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: #e2e8f0;
  resize: none;
  font-size: 16px;
  line-height: 1.5;
  min-height: 24px;
  max-height: 120px;
  padding: 8px 0;
}

.chat-input:focus {
  outline: none;
}

.send-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2563eb;
  border: none;
  color: white;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-button:hover {
  background: #1d4ed8;
  transform: scale(1.05);
}

.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 停止按钮样式 */
.stop-button {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  background-color: #ff4444;
  color: white;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 12px;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stop-button:hover {
  background-color: #cc3333;
  transform: translateY(-50%) scale(1.05);
}

/* 移动端样式 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    z-index: 20;
    transform: translateX(-100%);
    width: 280px;
  }
  
  .sidebar.open {
    transform: translateX(0);
  }
  
  .sidebar-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.5);
    z-index: 15;
  }
  
  .mobile-menu-btn {
    position: fixed;
    top: 20px;
    left: 20px;
    z-index: 25;
    background: rgba(0, 0, 0, 0.7);
    border: none;
    padding: 10px;
    border-radius: 8px;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    gap: 3px;
  }
  
  .hamburger {
    width: 20px;
    height: 2px;
    background: #fff;
    transition: 0.3s;
  }
}

@media (min-width: 769px) {
  .mobile-menu-btn {
    display: none;
  }
  
  .sidebar-overlay {
    display: none;
  }
}

/* 侧边栏样式 */
.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.chat-item {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #d1d5db;
  border: 1px solid transparent;
}

.chat-item:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
}

.chat-item.active {
  background: rgba(74, 144, 226, 0.2);
  border-color: #4a90e2;
  color: #ffffff;
}

/* 侧边栏按钮 - 使用纯色而非渐变色 */
.new-chat-btn {
  width: 100%;
  padding: 12px;
  background: #2d3748;
  color: #ffffff;
  border: 1px solid #4a5568;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  text-decoration: none;
  display: block;
  text-align: center;
}

.new-chat-btn:hover {
  background: #4a5568;
  border-color: #718096;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(74, 85, 104, 0.4);
}

/* 会话管理 */
.session-name-input {
  width: 100%;
  background: #40414f;
  border: 1px solid #565869;
  border-radius: 4px;
  color: #ffffff;
  padding: 4px 8px;
  font-size: 14px;
  outline: none;
}

.session-name-input:focus {
  border-color: #19c37d;
}

.session-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.session-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-btn {
  background: none;
  border: none;
  color: #8e8ea0;
  cursor: pointer;
  font-size: 18px;
  font-weight: bold;
  padding: 0;
  margin-left: 8px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  opacity: 0;
  transition: all 0.2s ease;
}

.chat-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #ff4444;
  color: #ffffff;
}

/* 消息样式 - 深色主题 */
.message {
  display: flex;
  margin-bottom: 16px;
  padding: 12px 16px;
  border-radius: 18px;
  max-width: 75%;
  word-wrap: break-word;
  animation: fadeIn 0.3s ease-in;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}

.message.user {
  background: #2563eb;
  color: white;
  margin-left: auto;
  flex-direction: row-reverse;
  border-bottom-right-radius: 6px;
}

.message.ai {
  background: linear-gradient(135deg, #2d3748 0%, #4a5568 100%);
  color: #e2e8f0;
  margin-right: auto;
  border-bottom-left-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.message.user .message-avatar {
  background: #1d4ed8;
  color: white;
  margin-left: 12px;
}

.message.ai .message-avatar {
  background: linear-gradient(135deg, #00d2ff, #3a7bd5);
  color: white;
  margin-right: 12px;
}

.message-content {
  flex: 1;
  line-height: 1.6;
  white-space: pre-wrap;
  font-size: 15px;
}

/* Markdown样式 */
.message-content h1,
.message-content h2,
.message-content h3,
.message-content h4,
.message-content h5,
.message-content h6 {
  margin: 16px 0 8px 0;
  font-weight: 600;
  line-height: 1.4;
}

.message-content h1 { font-size: 1.5em; }
.message-content h2 { font-size: 1.3em; }
.message-content h3 { font-size: 1.2em; }
.message-content h4 { font-size: 1.1em; }

.message-content p {
  margin: 8px 0;
  line-height: 1.6;
}

.message-content strong {
  font-weight: 600;
  color: #1e40af;
}

.message-content em {
  font-style: italic;
  color: #6b7280;
}

/* 链接样式 */
.message-content a {
  color: #3b82f6;
  text-decoration: underline;
  transition: all 0.2s ease;
}

.message-content a:hover {
  color: #1d4ed8;
  text-decoration-thickness: 2px;
}

.message-content a:visited {
  color: #7c3aed;
}

.message-content ul,
.message-content ol {
  margin: 12px 0;
  padding-left: 24px;
}

.message-content li {
  margin: 4px 0;
  line-height: 1.5;
}

.message-content blockquote {
  margin: 16px 0;
  padding: 12px 16px;
  border-left: 4px solid #3b82f6;
  background: rgba(59, 130, 246, 0.05);
  border-radius: 0 6px 6px 0;
}

.message-content code {
  background: rgba(100, 116, 139, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
  color: #e11d48;
}

.message-content pre {
  background: #1f2937;
  color: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
  border: 1px solid #374151;
}

.message-content pre code {
  background: none;
  padding: 0;
  color: inherit;
  font-size: 0.9em;
}

.message-content hr {
  margin: 20px 0;
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent, #e5e7eb, transparent);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: transparent;
  position: relative;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
<template>
  <div class="container">
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
            <div class="message-content">{{ message.content }}</div>
          </div>
          
          <!-- 正在输入的AI回复 -->
          <div v-if="isTyping" class="message ai">
            <div class="message-avatar">AI</div>
            <div class="message-content">{{ currentAiMessage }}</div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input-container">
          <div class="chat-input-wrapper">
            <textarea
              v-model="inputMessage"
              @keyup.enter="handleKeyUp"
              :disabled="isLoading"
              placeholder="输入您的消息..."
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
import { chatHistoryService, generateChatId } from '@/utils/chatService'

export default {
  name: 'LoveApp',
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
      sidebarOpen: false
    }
  },
  
  async mounted() {
    this.updateMetaTags()
    await this.loadSessions()
    this.createNewChat()
  },
  
  methods: {
    // 更新SEO meta标签
    updateMetaTags() {
      document.title = 'AI恋爱大师 - Hachimi Agent'
      this.updateMetaTag('description', 'AI恋爱大师提供专业的恋爱咨询和情感建议，帮助您解决恋爱中的各种问题，获得个性化的情感指导')
      this.updateMetaTag('keywords', 'AI恋爱大师,恋爱咨询,情感建议,恋爱指导,情感AI,恋爱问题')
      this.updateMetaProperty('og:title', 'AI恋爱大师 - 专业恋爱咨询服务')
      this.updateMetaProperty('og:description', '获得AI恋爱大师的专业情感建议，解决您的恋爱困惑')
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
    
    // 切换侧边栏
    toggleSidebar() {
      this.sidebarOpen = !this.sidebarOpen
    },
    
    // 创建新对话
    createNewChat() {
      this.currentChatId = generateChatId()
      this.messages = []
      this.inputMessage = ''
    },
    
    // 加载会话列表
    async loadSessions() {
      try {
        this.sessions = await chatHistoryService.getAllSessions()
        console.log('LoveApp 加载的会话数量:', this.sessions.length)
      } catch (error) {
        console.error('加载会话列表失败:', error)
        this.sessions = []
      }
    },
    
    // 选择会话
    async selectSession(sessionId) {
      console.log('选择会话:', sessionId)
      this.currentChatId = sessionId
      try {
        this.messages = await chatHistoryService.getSessionMessages(sessionId)
        console.log('加载的消息数量:', this.messages.length)
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      } catch (error) {
        console.error('加载会话消息失败:', error)
        this.messages = []
      }
    },
    
    // 获取会话显示名称
    getSessionDisplayName(session) {
      return chatHistoryService.getSessionDisplayName(session)
    },
    
    // 开始编辑会话名称
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
    
    // 保存会话名称
    saveSessionName() {
      if (this.editingSessionName.trim()) {
        chatHistoryService.setCustomSessionName(this.editingSessionId, this.editingSessionName.trim())
      }
      this.editingSessionId = null
      this.editingSessionName = ''
    },
    
    // 取消编辑
    cancelEdit() {
      this.editingSessionId = null
      this.editingSessionName = ''
    },
    
    // 删除会话
    async deleteSession(session) {
      try {
        const sessionId = session.sessionId || session.conversationId || session.id;
        await chatHistoryService.deleteSession(sessionId);
        
        // 移除已删除的会话
        this.sessions = this.sessions.filter(s => {
          const currentSessionId = s.sessionId || s.conversationId || s.id;
          return currentSessionId !== sessionId;
        });
        
        // 如果删除的是当前活跃会话，创建新会话
        if (this.currentChatId === sessionId) {
          this.createNewChat();
        }
        
        console.log('会话删除成功');
      } catch (error) {
        console.error('删除会话失败:', error);
      }
    },
    
    // 处理键盘事件
    handleKeyUp(event) {
      if (!event.shiftKey) {
        event.preventDefault()
        this.sendMessage()
      }
    },
    
    // 发送消息
    async sendMessage() {
      if (!this.inputMessage.trim() || this.isLoading) return
      
      const isNewChat = !this.sessions.some(
        s => (s.sessionId || s.conversationId || s.id) === this.currentChatId
      );

      const message = this.inputMessage.trim()
      this.inputMessage = ''
      
      // 添加用户消息到界面
      this.messages.push({
        id: Date.now(),
        content: message,
        messageType: 'user',
        timestamp: new Date()
      })
      
      if (isNewChat) {
        const snippet = message.length > 20 ? message.substring(0, 20) + '...' : message;
        chatHistoryService.setCustomSessionName(this.currentChatId, snippet);
        
        // Add the new session to the list for immediate UI update
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
      
      try {
        // 使用SSE调用接口
        const eventSource = new EventSource(
          `/api/ai/love_app/chat/sse/emitter?message=${encodeURIComponent(message)}&chatId=${this.currentChatId}`
        )
        
        eventSource.onmessage = (event) => {
          this.currentAiMessage += event.data
          this.scrollToBottom()
        }
        
        eventSource.onopen = () => {
          console.log('SSE connection opened')
        }
        
        eventSource.onerror = (event) => {
          console.error('SSE error:', event)
          eventSource.close()
          this.handleSseComplete()
        }
        
        eventSource.addEventListener('close', () => {
          eventSource.close()
          this.handleSseComplete()
        })
        
        // 监听连接关闭
        const checkClosed = setInterval(() => {
          if (eventSource.readyState === EventSource.CLOSED) {
            clearInterval(checkClosed)
            this.handleSseComplete()
          }
        }, 100)
        
      } catch (error) {
        console.error('发送消息失败:', error)
        this.isLoading = false
        this.isTyping = false
        this.currentAiMessage = ''
      }
    },
    
    // 处理SSE完成
    handleSseComplete() {
      if (this.currentAiMessage) {
        this.messages.push({
          id: Date.now(),
          content: this.currentAiMessage,
          messageType: 'ai',
          timestamp: new Date()
        })
      }
      
      this.isLoading = false
      this.isTyping = false
      this.currentAiMessage = ''
      this.scrollToBottom()
      
      // 延迟一秒后重新加载会话列表，确保后端数据已保存
      setTimeout(() => {
        this.loadSessions()
      }, 1000)
    },
    
    // 滚动到底部
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
</style>

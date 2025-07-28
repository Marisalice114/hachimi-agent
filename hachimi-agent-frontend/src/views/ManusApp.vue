<template>
  <div class="manus-container">
    <!-- 主要内容区域 - 全宽，没有侧边栏 -->
    <div class="main-content">
      <!-- 返回主页按钮 -->
      <div class="header">
        <router-link to="/" class="back-btn">
          ← 返回主页
        </router-link>
        <h2 class="page-title">AI 超级智能体</h2>
      </div>

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
            <div class="message-content">
              <div v-if="message.messageType === 'ai' && message.steps">
                <!-- 步骤分离显示 -->
                <div v-for="(step, index) in message.steps" :key="index" class="step-item">
                  <div class="step-header">Step {{ index + 1 }}: {{ step.tool }}</div>
                  <div class="step-result">{{ step.result }}</div>
                </div>
              </div>
              <div v-else>
                {{ message.content }}
              </div>
            </div>
          </div>
          
          <!-- 正在输入的AI回复 -->
          <div v-if="isTyping" class="message ai">
            <div class="message-avatar">AI</div>
            <div class="message-content">
              <!-- 显示已完成的步骤 -->
              <div v-for="(step, index) in stepMessages" :key="index" class="step-item">
                <div class="step-header">Step {{ index + 1 }}: {{ step.tool }}</div>
                <div class="step-result">{{ step.result }}</div>
              </div>
              <!-- 显示当前正在处理的步骤 -->
              <div v-if="currentAiMessage" class="step-item current-step">
                <div class="step-header">Step {{ stepMessages.length + 1 }}: Processing...</div>
                <div class="step-result">{{ currentAiMessage }}</div>
              </div>
            </div>
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
import { generateChatId } from '@/utils/chatService'

export default {
  name: 'ManusApp',
  data() {
    return {
      currentChatId: null,
      inputMessage: '',
      messages: [],
      isLoading: false,
      isTyping: false,
      currentAiMessage: '',
      currentStepIndex: 0,  // 当前步骤索引
      stepMessages: []      // 存储每个步骤的消息
    }
  },
  
  mounted() {
    this.updateMetaTags()
    this.createNewChat()
  },
  
  methods: {
    // 更新SEO meta标签
    updateMetaTags() {
      document.title = 'AI超级智能体 - Hachimi Agent'
      this.updateMetaTag('description', 'AI超级智能体是强大的多功能AI助手，为您提供全方位的智能服务，包括任务处理、问题解决和智能分析')
      this.updateMetaTag('keywords', 'AI超级智能体,AI助手,智能任务处理,人工智能,智能分析,问题解决')
      this.updateMetaProperty('og:title', 'AI超级智能体 - 多功能AI助手服务')
      this.updateMetaProperty('og:description', '体验AI超级智能体的强大功能，获得全方位的智能服务支持')
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
    
    // 创建新对话
    createNewChat() {
      this.currentChatId = generateChatId()
      this.messages = []
      this.inputMessage = ''
      this.stepMessages = []
      this.currentStepIndex = 0
      this.currentAiMessage = ''
      this.isLoading = false
      this.isTyping = false
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
      
      const message = this.inputMessage.trim()
      this.inputMessage = ''
      
      // 添加用户消息到界面
      this.messages.push({
        id: Date.now(),
        content: message,
        messageType: 'user',
        timestamp: new Date()
      })
      
      this.scrollToBottom()
      this.isLoading = true
      this.isTyping = true
      this.currentAiMessage = ''
      this.currentStepIndex = 0
      this.stepMessages = []
      
      try {
        // 使用SSE调用Manus接口（注意：Manus不使用chatId）
        const eventSource = new EventSource(
          `/api/ai/manus/chat?message=${encodeURIComponent(message)}`
        )
        
        eventSource.onmessage = (event) => {
          const data = event.data
          
          // 尝试解析步骤信息 - 更宽泛的匹配
          if (data.includes('Step') && (data.includes('Tool:') || data.includes('tool:')) && (data.includes('Result:') || data.includes('result:'))) {
            // 多种格式匹配
            let stepMatch = data.match(/Step (\d+): Tool: (\w+), Result: "(.*?)"/) || 
                           data.match(/Step (\d+): tool: (\w+), result: "(.*?)"/) ||
                           data.match(/Step (\d+): Tool: (\w+), Result: (.*?)(?:Step|$)/) ||
                           data.match(/Step (\d+): (\w+), (.*?)(?:Step|$)/)
            
            if (stepMatch) {
              const [, stepNum, tool, result] = stepMatch
              const stepNumber = parseInt(stepNum)
              
              // 确保步骤数组有足够的空间
              while (this.stepMessages.length < stepNumber) {
                this.stepMessages.push({
                  tool: 'Processing',
                  result: 'Loading...'
                })
              }
              
              // 更新对应步骤
              if (stepNumber > 0 && stepNumber <= this.stepMessages.length) {
                this.stepMessages[stepNumber - 1] = {
                  tool: tool,
                  result: result.replace(/"/g, '') // 移除引号
                }
              } else {
                // 新增步骤
                this.stepMessages.push({
                  tool: tool,
                  result: result.replace(/"/g, '')
                })
              }
              
              // 清空当前消息缓冲区
              this.currentAiMessage = ''
            } else {
              // 如果无法解析为步骤，但包含Step关键词，可能是新格式
              this.currentAiMessage += data + '\n'
            }
          } else {
            // 普通文本，添加到当前消息
            this.currentAiMessage += data
          }
          
          this.scrollToBottom()
        }
        
        eventSource.onopen = () => {
          console.log('Manus SSE connection opened')
        }
        
        eventSource.onerror = (event) => {
          console.error('Manus SSE error:', event)
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
      // 保存最后的消息内容（如果有未处理的内容）
      if (this.currentAiMessage && !this.stepMessages.some(step => step.result === this.currentAiMessage)) {
        this.stepMessages.push({
          tool: 'Final',
          result: this.currentAiMessage
        })
      }
      
      // 构建包含步骤的AI消息
      const aiMessage = {
        id: Date.now(),
        messageType: 'ai',
        timestamp: new Date()
      }
      
      if (this.stepMessages.length > 0) {
        aiMessage.steps = [...this.stepMessages]
        aiMessage.content = `完成了 ${this.stepMessages.length} 个步骤`
      } else {
        aiMessage.content = this.currentAiMessage || '处理完成'
      }
      
      this.messages.push(aiMessage)
      
      this.isLoading = false
      this.isTyping = false
      this.currentAiMessage = ''
      this.stepMessages = []
      this.currentStepIndex = 0
      this.scrollToBottom()
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
.manus-container {
  display: flex;
  height: 100vh;
  background-color: #0f1011;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #343541;
  background-color: #202123;
}

.back-btn {
  color: #ffffff;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid #565869;
  margin-right: 20px;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background-color: #40414f;
}

.page-title {
  color: #ffffff;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  gap: 16px;
  padding: 16px 0;
}

.message.user {
  flex-direction: row-reverse;
  background-color: #343541;
  margin: 0 -20px;
  padding: 16px 20px;
}

.message.ai {
  background-color: transparent;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background-color: #19c37d;
  color: #ffffff;
}

.message.ai .message-avatar {
  background-color: #da7756;
  color: #ffffff;
}

.message-content {
  flex: 1;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  color: #ffffff;
}

.chat-input-container {
  padding: 20px;
  border-top: 1px solid #343541;
}

.chat-input-wrapper {
  max-width: 800px;
  margin: 0 auto;
  position: relative;
}

.chat-input {
  width: 100%;
  min-height: 52px;
  max-height: 200px;
  padding: 16px 52px 16px 16px;
  background-color: #40414f;
  border: 1px solid #565869;
  border-radius: 12px;
  color: #ffffff;
  font-size: 16px;
  resize: none;
  outline: none;
  font-family: inherit;
}

.chat-input:focus {
  border-color: #19c37d;
}

.send-button {
  position: absolute;
  right: 12px;
  bottom: 12px;
  width: 32px;
  height: 32px;
  background-color: #19c37d;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.send-button:hover {
  background-color: #16a069;
}

.send-button:disabled {
  background-color: #565869;
  cursor: not-allowed;
}

/* 步骤显示样式 */
.step-item {
  margin-bottom: 12px;
  padding: 12px;
  background-color: #2a2b32;
  border-radius: 8px;
  border-left: 3px solid #19c37d;
}

.step-item.current-step {
  border-left-color: #da7756;
  background-color: #3a3b42;
}

.step-header {
  font-weight: bold;
  color: #19c37d;
  margin-bottom: 6px;
  font-size: 14px;
}

.current-step .step-header {
  color: #da7756;
}

.step-result {
  color: #e1e1e1;
  line-height: 1.4;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }
  
  .chat-messages {
    padding: 16px;
  }
  
  .chat-input-container {
    padding: 16px;
  }
}
</style>

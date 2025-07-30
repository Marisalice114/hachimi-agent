<template>
  <div class="manus-container">
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
                  <div v-if="step.detailResult" class="step-detail-result">
                    <strong>result:</strong> {{ step.detailResult }}
                  </div>
                </div>
              </div>
              <div v-else-if="message.messageType === 'ai' && (message.thinkingProcess || message.content)">
                <!-- AI回复带思考过程 -->
                
                <!-- 思考过程框（如果有） -->
                <div v-if="message.thinkingProcess" class="thinking-box completed">
                  <div class="thinking-header" @click="toggleThinkingExpansion(message.id)">
                    <span class="thinking-icon">🤔</span>
                    <span class="thinking-title">AI 思考过程</span>
                    <span class="expand-icon" :class="{ 'expanded': message.expanded }">▼</span>
                  </div>
                  <div v-show="message.expanded !== false" class="thinking-content">
                    <!-- 显示思考内容 -->
                    <div v-if="message.thinkingProcess.content" class="thinking-text">
                      <div class="thinking-label">思考：</div>
                      <div class="thinking-value">{{ message.thinkingProcess.content }}</div>
                    </div>
                    <!-- 显示工具调用日志 -->
                    <div v-for="(log, index) in message.thinkingProcess.logs" :key="index" class="thinking-log">
                      <div class="log-type" :class="log.type">{{ log.label }}</div>
                      <div class="log-content">{{ log.content }}</div>
                    </div>
                  </div>
                </div>
                
                <!-- 最终回复内容 -->
                <div v-if="message.content" class="final-response completed">
                  <div class="final-response-header">
                    <span class="response-icon">💬</span>
                    <span class="response-title">回复</span>
                  </div>
                  <div class="final-response-content">
                    <div class="ai-content" v-html="formatAiContent(message.content)"></div>
                  </div>
                </div>
                
                <!-- 如果没有思考过程，直接显示内容 -->
                <div v-if="!message.thinkingProcess && message.content" class="ai-content" v-html="formatAiContent(message.content)"></div>
              </div>
              <div v-else>
                <!-- 普通消息内容 -->
                {{ message.content }}
              </div>
            </div>
          </div>
          
          <!-- 正在输入的AI回复 -->
          <div v-if="isTyping" class="message ai">
            <div class="message-avatar">AI</div>
            <div class="message-content">
              <!-- 思考过程框 -->
              <div v-if="showThinkingBox" class="thinking-box">
                <div class="thinking-header">
                  <span class="thinking-icon">🤔</span>
                  <span class="thinking-title">AI 思考过程</span>
                </div>
                <div class="thinking-content" ref="thinkingContainer">
                  <!-- 显示思考内容 -->
                  <div v-if="thinkingContent" class="thinking-text">
                    <div class="thinking-label">思考：</div>
                    <div class="thinking-value">{{ thinkingContent }}</div>
                  </div>
                  <!-- 显示工具调用日志 -->
                  <div v-for="(log, index) in thinkingLogs" :key="index" class="thinking-log">
                    <div class="log-type" :class="log.type">{{ log.label }}</div>
                    <div class="log-content">{{ log.content }}</div>
                  </div>
                </div>
              </div>
              
              <!-- 最终回复内容 -->
              <div v-if="finalResponse" class="final-response">
                <div class="final-response-header">
                  <span class="response-icon">💬</span>
                  <span class="response-title">回复</span>
                </div>
                <div class="final-response-content">
                  <div class="ai-content" v-html="formatAiContent(finalResponse)"></div>
                </div>
              </div>
              
              <!-- 如果有步骤，显示步骤格式（兼容旧版本） -->
              <div v-if="stepMessages.length > 0 && !showThinkingBox">
                <!-- 显示已完成的步骤 -->
                <div v-for="(step, index) in stepMessages" :key="index" class="step-item">
                  <div class="step-header">Step {{ index + 1 }}: {{ step.tool }}</div>
                  <div class="step-result">{{ step.result }}</div>
                  <div v-if="step.detailResult" class="step-detail-result">
                    <strong>result:</strong> {{ step.detailResult }}
                  </div>
                </div>
                <!-- 显示当前正在处理的步骤 -->
                <div v-if="currentAiMessage" class="step-item current-step">
                  <div class="step-header">Step {{ stepMessages.length + 1 }}: Processing...</div>
                  <div class="step-result">{{ currentAiMessage }}</div>
                </div>
              </div>
              
              <!-- 如果没有步骤且没有思考框，直接显示AI回复内容（兼容旧版本） -->
              <div v-else-if="currentAiMessage && !showThinkingBox && !finalResponse" class="ai-direct-response">
                <div class="ai-content" v-html="formatAiContent(currentAiMessage)"></div>
              </div>
            </div>
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
              title="停止AI回复"
            >
              ⏹ 停止回复
            </button>
            
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
import { generateChatId, aiChatService } from '@/utils/chatService'
 
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
      stepMessages: [],     // 存储每个步骤的消息
      currentStreamId: null, // 新增：当前流ID
      eventSource: null, // 新增：当前EventSource连接
      // 新增：思考过程相关数据
      thinkingContent: '',    // AI思考内容
      thinkingLogs: [],       // 思考过程日志
      showThinkingBox: false, // 是否显示思考框
      finalResponse: ''       // 最终回复内容
    }
  },
  
  mounted() {
    this.updateMetaTags()
    this.createNewChat()
  },
  
  // 新增：组件销毁前清理连接
  beforeUnmount() {
    this.closeEventSource()
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
      this.currentStreamId = null
      // 初始化思考相关数据
      this.thinkingContent = ''
      this.thinkingLogs = []
      this.showThinkingBox = false
      this.finalResponse = ''
    },
    
    // 处理键盘事件
    handleKeyUp(event) {
      if (!event.shiftKey) {
        event.preventDefault()
        this.sendMessage()
      }
    },
    
    // 发送消息 - 修改为直接建立SSE连接
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
      this.currentStreamId = null
      
      try {
        // 直接建立SSE连接
        const sseUrl = aiChatService.getManusSseUrl(message)
        this.establishSSEConnection(sseUrl)
        
      } catch (error) {
        console.error('发送消息失败:', error)
        this.handleSseError('发送消息失败: ' + (error.message || '未知错误'))
      }
    },
    
    // 建立SSE连接 - 修改实现
    establishSSEConnection(sseUrl) {
      // 关闭之前的连接
      this.closeEventSource()
      
      // 建立新的SSE连接
      console.log('建立SSE连接:', sseUrl)
      this.eventSource = new EventSource(sseUrl)
      
      this.eventSource.onmessage = (event) => {
        // 处理默认消息事件
        this.processMessageData(event.data)
      }
      
      this.eventSource.onopen = () => {
        console.log('Manus SSE connection opened')
      }
      
      this.eventSource.onerror = (event) => {
        console.error('Manus SSE error:', event)
        console.log('SSE readyState:', event.target.readyState)
        console.log('EventSource.CONNECTING:', EventSource.CONNECTING)
        console.log('EventSource.OPEN:', EventSource.OPEN) 
        console.log('EventSource.CLOSED:', EventSource.CLOSED)
        
        // 检查连接状态
        if (event.target.readyState === EventSource.CLOSED) {
          console.log('SSE连接已关闭，准备完成处理')
          // 连接关闭时，检查是否有步骤数据或AI消息内容需要保存
          if (this.stepMessages.length > 0 || (this.currentAiMessage && this.currentAiMessage.trim().length > 0)) {
            // 如果有步骤数据或AI消息内容，正常完成处理
            console.log('检测到有步骤数据或AI内容，判断为正常完成')
            this.handleSseComplete()
          } else {
            // 如果没有步骤数据，可能是真的错误
            console.log('没有检测到步骤数据，判断为连接错误')
            this.handleSseError('连接已断开')
          }
        } else if (event.target.readyState === EventSource.CONNECTING) {
          // CONNECTING状态，如果有步骤数据说明处理已完成，这是正常的连接关闭
          console.log('SSE连接状态为CONNECTING')
          if (this.stepMessages.length > 0) {
            console.log('检测到有步骤数据，判断为正常完成（连接重置）')
            this.handleSseComplete()
          } else {
            console.log('没有步骤数据，可能是连接问题')
            // 先不处理，等待可能的重连或其他事件
          }
        } else {
          console.error('SSE连接出现其他错误，readyState:', event.target.readyState)
        }
      }
      
      // 监听自定义事件
      this.eventSource.addEventListener('stream_info', (event) => {
        try {
          const info = JSON.parse(event.data)
          this.currentStreamId = info.streamId
          console.log('收到stream_info:', info)
        } catch (e) {
          console.error('解析stream_info失败:', e)
        }
      })
      
      // 监听数据事件
      this.eventSource.addEventListener('data', (event) => {
        this.processMessageData(event.data)
      })
      
      // 监听完成事件
      this.eventSource.addEventListener('complete', () => {
        console.log('收到complete事件')
        this.handleSseComplete()
      })
      
      // 监听错误事件
      this.eventSource.addEventListener('error', (event) => {
        console.error('SSE error event:', event.data)
        this.handleSseError(event.data || 'AI处理失败')
      })
    },
    
    // 处理消息数据 - 为Manus专门优化
    processMessageData(data) {
      console.log('收到消息数据:', data) // 添加调试日志
      
      // 检查是否是完成信号
      if (data === '[DONE]') {
        console.log('收到[DONE]信号，处理完成')
        this.handleSseComplete()
        return
      }
      
      // 检查新的SSE消息格式
      if (data.startsWith('THINK:')) {
        // 思考过程内容
        const thinkContent = data.substring(6).trim()
        this.thinkingContent = thinkContent
        this.showThinkingBox = true
        console.log('收到思考内容:', thinkContent)
        this.scrollToThinkingBottom()
        return
      }
      
      if (data.startsWith('TOOL_START:')) {
        // 工具开始执行
        const toolName = data.substring(11).trim()
        this.thinkingLogs.push({
          type: 'tool-start',
          label: '工具开始',
          content: toolName
        })
        this.showThinkingBox = true
        console.log('工具开始:', toolName)
        this.scrollToThinkingBottom()
        return
      }
      
      if (data.startsWith('TOOL_ARGS:')) {
        // 工具参数
        const toolArgs = data.substring(10).trim()
        this.thinkingLogs.push({
          type: 'tool-args',
          label: '工具参数',
          content: toolArgs
        })
        console.log('工具参数:', toolArgs)
        this.scrollToThinkingBottom()
        return
      }
      
      if (data.startsWith('TOOL_RESULT:')) {
        // 工具结果
        const toolResult = data.substring(12).trim()
        this.thinkingLogs.push({
          type: 'tool-result',
          label: '工具结果',
          content: toolResult
        })
        console.log('工具结果:', toolResult)
        this.scrollToThinkingBottom()
        return
      }
      
      if (data.startsWith('FINAL_RESPONSE:')) {
        // 最终回复
        const finalResponse = data.substring(15).trim()
        
        // 如果已经有最终回复内容且新内容是"任务处理完成"，则忽略
        if (this.finalResponse && this.finalResponse.length > 50 && finalResponse === '任务处理完成') {
          console.log('忽略重复的任务完成消息，保持原有内容')
          return
        }
        
        // 如果新内容比现有内容更详细，则使用新内容
        if (!this.finalResponse || finalResponse.length > this.finalResponse.length) {
          this.finalResponse = finalResponse
          console.log('更新最终回复:', finalResponse.substring(0, 100) + '...')
        }
        
        this.scrollToBottom()
        return
      }
      
      // 检查是否是步骤信息 - 匹配多种格式（保持向后兼容）
      const stepMatch = data.match(/Step (\d+): (\w+)/)
      const toolMatch = data.match(/Tool: (\w+), (Arguments|Result): (.*)/)
      
      if (stepMatch) {
        const [, stepNum, tool] = stepMatch
        const stepNumber = parseInt(stepNum)
        
        console.log(`解析到步骤 ${stepNumber}: ${tool}`) // 添加调试日志
        
        // 当开始新步骤时，如果有累积的AI消息内容，保存到前一个步骤的详细结果中
        if (stepNumber > 1 && this.currentAiMessage.trim() && this.stepMessages.length >= stepNumber - 1) {
          const prevStepIndex = stepNumber - 2
          if (this.stepMessages[prevStepIndex]) {
            // 设置完成状态并添加详细结果
            const prevTool = this.stepMessages[prevStepIndex].tool
            if (prevTool === '网络搜索') {
              this.stepMessages[prevStepIndex].result = '✅ 搜索完成，找到相关信息'
            } else if (prevTool === '文件写入') {
              this.stepMessages[prevStepIndex].result = '✅ 文件保存成功'
            } else if (prevTool === '任务完成') {
              this.stepMessages[prevStepIndex].result = '✅ 任务已完成'
            } else {
              this.stepMessages[prevStepIndex].result = '✅ 处理完成'
            }
            this.stepMessages[prevStepIndex].detailResult = this.currentAiMessage.trim()
          }
        }
        
        // 确保步骤数组有足够的空间
        while (this.stepMessages.length < stepNumber) {
          this.stepMessages.push({
            tool: 'Processing',
            result: 'Loading...',
            detailResult: ''
          })
        }
        
        // 为不同的工具添加友好的显示名称和描述
        const toolDisplayInfo = {
          'searchWeb': { name: '网络搜索', desc: '正在搜索相关信息...' },
          'scrapeWebPage': { name: '网页抓取', desc: '正在抓取网页内容...' },
          'generatePDF': { name: '生成PDF', desc: '正在生成PDF文档...' },
          'fileWrite': { name: '文件写入', desc: '正在保存文件...' },
          'fileRead': { name: '文件读取', desc: '正在读取文件...' },
          'doTerminate': { name: '任务完成', desc: '正在完成任务...' }
        }
        
        const toolInfo = toolDisplayInfo[tool] || { name: tool, desc: '正在处理...' }
        
        // 更新对应步骤的工具名称
        if (stepNumber > 0) {
          if (stepNumber <= this.stepMessages.length) {
            this.stepMessages[stepNumber - 1] = {
              tool: toolInfo.name,
              result: toolInfo.desc,
              detailResult: ''
            }
          } else {
            // 新增步骤
            this.stepMessages.push({
              tool: toolInfo.name,
              result: toolInfo.desc,
              detailResult: ''
            })
          }
        }
        
        // 清空当前消息缓冲区
        this.currentAiMessage = ''
      } else if (toolMatch) {
        // 处理工具调用信息格式：Tool: toolName, Arguments/Result: ...
        const [, tool, type, content] = toolMatch
        
        console.log(`解析到工具调用: ${tool}, ${type}: ${content}`)
        
        // 检查是否是重复的Result数据（包含 "Tool: toolName, Result:"）
        const isNestedResult = content.includes(`Tool: ${tool}, Result:`)
        let actualContent = content
        
        if (isNestedResult) {
          // 提取实际的结果内容，跳过重复的前缀
          const nestedMatch = content.match(`Tool: ${tool}, Result: (.*)`)
          if (nestedMatch) {
            actualContent = nestedMatch[1]
            console.log(`检测到嵌套Result，提取实际内容: ${actualContent}`)
          }
        }
        
        const toolDisplayInfo = {
          'searchWeb': { name: '网络搜索' },
          'scrapeWebPage': { name: '网页抓取' },
          'generatePDF': { name: '生成PDF' },
          'fileWrite': { name: '文件写入' },
          'fileRead': { name: '文件读取' },
          'doTerminate': { name: '任务完成' }
        }
        
        const toolInfo = toolDisplayInfo[tool] || { name: tool }
        
        if (type === 'Arguments') {
          // 开始新的工具调用时，如果有累积的AI消息内容，保存到当前步骤的详细结果中
          if (this.currentAiMessage.trim() && this.stepMessages.length > 0) {
            const lastStepIndex = this.stepMessages.length - 1
            if (this.stepMessages[lastStepIndex].result.includes('正在') ||
                this.stepMessages[lastStepIndex].result === 'Loading...' ||
                this.stepMessages[lastStepIndex].result === 'Processing...') {
              const lastTool = this.stepMessages[lastStepIndex].tool
              if (lastTool === '网络搜索') {
                this.stepMessages[lastStepIndex].result = '✅ 搜索完成，找到相关信息'
              } else if (lastTool === '文件写入') {
                this.stepMessages[lastStepIndex].result = '✅ 文件保存成功'
              } else if (lastTool === '任务完成') {
                this.stepMessages[lastStepIndex].result = '✅ 任务已完成'
              } else {
                this.stepMessages[lastStepIndex].result = '✅ 处理完成'
              }
              this.stepMessages[lastStepIndex].detailResult = this.currentAiMessage.trim()
            }
          }
          
          // 新增一个步骤
          this.stepMessages.push({
            tool: toolInfo.name,
            result: '正在处理...',
            detailResult: ''
          })
        } else if (type === 'Result') {
          // 更新最后一个步骤的结果
          if (this.stepMessages.length > 0) {
            const lastStepIndex = this.stepMessages.length - 1
            this.stepMessages[lastStepIndex].result = this.formatResult(actualContent, tool)
          }
        }
        
        this.currentAiMessage = ''
      } else {
        // 普通文本，累积到当前AI消息中
        this.currentAiMessage += data
      }
      
      this.scrollToBottom()
    },
    
    // 格式化结果显示
    formatResult(data, tool = '') {
      if (data.includes('File written successfully')) {
        return '✅ 文件保存成功'
      } else if (data === 'Terminated') {
        return '✅ 任务已完成'
      } else if (data.startsWith('{')) {
        // JSON数据
        try {
          const jsonData = JSON.parse(data)
          if (jsonData.title && jsonData.link) {
            return `找到: ${jsonData.title}`
          } else {
            return '✅ 搜索完成，找到相关信息'
          }
        } catch (e) {
          return '✅ 获取到JSON数据'
        }
      } else if (data.includes('<!doctype html>') || data.includes('<html>')) {
        // HTML数据
        return '✅ 成功获取网页内容'
      } else if (data.includes('Type of font') && data.includes('not recognized')) {
        return '⚠️ PDF生成遇到字体问题，尝试其他方式'
      } else if (data.length > 100) {
        // 长文本内容，显示摘要
        return `✅ 处理完成: ${data.substring(0, 100)}...`
      } else {
        // 短文本内容
        return data || '✅ 处理完成'
      }
    },
    
    // 停止AI聊天流 - 新增方法
    async stopChatStream() {
      if (!this.currentStreamId) {
        console.warn('没有活动的聊天流可以停止')
        return
      }
      
      try {
        await aiChatService.stopChatStream(this.currentStreamId)
        console.log('成功停止AI聊天流:', this.currentStreamId)
        this.handleSseComplete()
      } catch (error) {
        console.error('停止AI聊天流失败:', error)
        this.handleSseError('停止AI回复失败: ' + (error.message || '未知错误'))
      }
    },
    
    // 关闭EventSource连接 - 新增方法
    closeEventSource() {
      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }
    },
    
    // 处理SSE错误 - 新增方法
    handleSseError(errorMessage) {
      this.isLoading = false
      this.isTyping = false
      this.currentAiMessage = ''
      this.currentStreamId = null
      this.closeEventSource()
      this.stepMessages = []
      this.currentStepIndex = 0
      
      // 显示错误消息
      if (errorMessage) {
        // 构建包含步骤的AI消息
        const aiMessage = {
          id: Date.now(),
          messageType: 'ai',
          timestamp: new Date(),
          content: `❌ 错误: ${errorMessage}`
        }
        
        this.messages.push(aiMessage)
        this.scrollToBottom()
      }
    },
    
    // 处理SSE完成
    handleSseComplete() {
      console.log('处理SSE完成，当前步骤数量:', this.stepMessages.length)
      console.log('当前AI消息内容长度:', this.currentAiMessage ? this.currentAiMessage.length : 0)
      console.log('思考框状态:', this.showThinkingBox)
      console.log('最终回复内容:', this.finalResponse)
      
      // 构建AI消息
      const aiMessage = {
        id: Date.now(),
        messageType: 'ai',
        timestamp: new Date(),
        expanded: true // 默认展开思考过程
      }
      
      // 保存思考过程（如果有）
      if (this.showThinkingBox && (this.thinkingContent || this.thinkingLogs.length > 0)) {
        aiMessage.thinkingProcess = {
          content: this.thinkingContent,
          logs: [...this.thinkingLogs]
        }
      }
      
      // 优先检查是否有最终回复（新格式）
      if (this.finalResponse && this.finalResponse.trim()) {
        aiMessage.content = this.finalResponse.trim()
        console.log('使用最终回复内容，长度:', aiMessage.content.length)
      }
      // 检查是否有思考过程但没有最终回复
      else if (this.showThinkingBox && this.thinkingContent) {
        aiMessage.content = this.thinkingContent
        console.log('使用思考内容作为回复，长度:', aiMessage.content.length)
      }
      // 如果有累积的AI消息内容，保存到最后一个步骤的详细结果中
      else if (this.currentAiMessage && this.currentAiMessage.trim() && this.stepMessages.length > 0) {
        const lastStepIndex = this.stepMessages.length - 1
        // 如果最后一个步骤还在处理状态，先设置完成状态
        if (this.stepMessages[lastStepIndex].result.includes('正在') ||
            this.stepMessages[lastStepIndex].result === 'Loading...' ||
            this.stepMessages[lastStepIndex].result === 'Processing...') {
          const lastTool = this.stepMessages[lastStepIndex].tool
          if (lastTool === '网络搜索') {
            this.stepMessages[lastStepIndex].result = '✅ 搜索完成，找到相关信息'
          } else if (lastTool === '文件写入') {
            this.stepMessages[lastStepIndex].result = '✅ 文件保存成功'
          } else if (lastTool === '任务完成') {
            this.stepMessages[lastStepIndex].result = '✅ 任务已完成'
          } else {
            this.stepMessages[lastStepIndex].result = '✅ 处理完成'
          }
        }
        // 添加详细结果
        this.stepMessages[lastStepIndex].detailResult = this.currentAiMessage.trim()
        
        // 使用步骤格式显示
        const hasRealToolSteps = this.stepMessages.some(step => 
          step.tool !== 'AI回复' && step.tool !== 'Final' && step.tool !== 'Processing')
        
        if (hasRealToolSteps) {
          aiMessage.steps = [...this.stepMessages]
          aiMessage.content = `✅ 完成了 ${this.stepMessages.length} 个步骤的处理`
          console.log('使用步骤格式，步骤数量:', aiMessage.steps.length)
        } else {
          // 如果步骤都是通用步骤，显示第一个步骤的结果
          aiMessage.content = this.stepMessages[0]?.result || '处理完成'
          console.log('使用第一个步骤结果作为内容')
        }
      }
      // 优先检查是否有AI文本内容（没有使用工具的情况）
      else if (this.currentAiMessage && this.currentAiMessage.trim() && this.stepMessages.length === 0) {
        // 如果有AI文本内容且没有步骤，直接显示文本（AI直接回复的情况）
        aiMessage.content = this.currentAiMessage.trim()
        console.log('使用AI直接回复内容，长度:', aiMessage.content.length)
      } else if (this.stepMessages.length > 0) {
        // 确保所有正在处理的步骤都标记为完成
        this.stepMessages.forEach((step, index) => {
          if (step.result.includes('正在') || 
              step.result === 'Loading...' || 
              step.result === 'Processing...') {
            const tool = step.tool
            if (tool === '网络搜索') {
              this.stepMessages[index].result = '✅ 搜索完成，找到相关信息'
            } else if (tool === '文件写入') {
              this.stepMessages[index].result = '✅ 文件保存成功'
            } else if (tool === '任务完成') {
              this.stepMessages[index].result = '✅ 任务已完成'
            } else {
              this.stepMessages[index].result = '✅ 处理完成'
            }
          }
        })
        
        // 如果有步骤，使用步骤格式显示
        const hasRealToolSteps = this.stepMessages.some(step => 
          step.tool !== 'AI回复' && step.tool !== 'Final' && step.tool !== 'Processing')
        
        if (hasRealToolSteps) {
          aiMessage.steps = [...this.stepMessages]
          aiMessage.content = `✅ 完成了 ${this.stepMessages.length} 个步骤的处理`
          console.log('使用步骤格式，步骤数量:', aiMessage.steps.length)
        } else {
          // 如果步骤都是通用步骤，显示第一个步骤的结果
          aiMessage.content = this.stepMessages[0]?.result || '处理完成'
          console.log('使用第一个步骤结果作为内容')
        }
      } else {
        aiMessage.content = '处理完成'
        console.log('使用默认完成内容')
      }
      
      this.messages.push(aiMessage)
      
      // 清理状态
      this.isLoading = false
      this.isTyping = false
      this.currentAiMessage = ''
      this.stepMessages = []
      this.currentStepIndex = 0
      this.currentStreamId = null
      // 清理思考过程相关状态
      this.thinkingContent = ''
      this.thinkingLogs = []
      this.showThinkingBox = false
      this.finalResponse = ''
      
      this.closeEventSource()
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
    },
    
    // 滚动思考框到底部
    scrollToThinkingBottom() {
      this.$nextTick(() => {
        const container = this.$refs.thinkingContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    
    // 格式化AI内容，支持Markdown样式
    formatAiContent(content) {
      if (!content) return ''
      
      // 处理标题
      content = content.replace(/### (.*)/g, '<h3 class="ai-h3">$1</h3>')
      content = content.replace(/#### (.*)/g, '<h4 class="ai-h4">$1</h4>')
      
      // 处理粗体
      content = content.replace(/\*\*(.*?)\*\*/g, '<strong class="ai-bold">$1</strong>')
      
      // 处理列表项
      content = content.replace(/- (.*)/g, '<div class="ai-list-item">• $1</div>')
      
      // 处理换行
      content = content.replace(/\n/g, '<br>')
      
      return content
    },
    
    // 切换思考过程展开/收起
    toggleThinkingExpansion(messageId) {
      const message = this.messages.find(m => m.id === messageId)
      if (message) {
        message.expanded = !message.expanded
      }
    }
  }
}
</script>
 
<style scoped>
/* 容器样式 */
.manus-container {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(135deg, #0c0c0c 0%, #1a1a2e 50%, #16213e 100%);
  overflow: hidden;
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

/* 粒子效果 */
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
  box-shadow: 0 0 6px rgba(255, 255, 255, 0.8);
}

.particle:nth-child(odd) {
  animation-delay: -7.5s;
  background: radial-gradient(circle, rgba(120, 119, 198, 1) 0%, rgba(120, 119, 198, 0.3) 70%);
  box-shadow: 0 0 6px rgba(120, 119, 198, 0.8);
}

.particle:nth-child(3n) {
  width: 3px;
  height: 3px;
  animation-delay: -5s;
  background: radial-gradient(circle, rgba(74, 144, 226, 1) 0%, rgba(74, 144, 226, 0.3) 70%);
  box-shadow: 0 0 6px rgba(74, 144, 226, 0.8);
}

.particle:nth-child(4n) {
  width: 5px;
  height: 5px;
  animation-delay: -2.5s;
  background: radial-gradient(circle, rgba(168, 85, 247, 1) 0%, rgba(168, 85, 247, 0.3) 70%);
  box-shadow: 0 0 6px rgba(168, 85, 247, 0.8);
}

.particle:nth-child(5n) {
  animation-delay: -10s;
  background: radial-gradient(circle, rgba(34, 197, 94, 1) 0%, rgba(34, 197, 94, 0.3) 70%);
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.8);
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
.main-content, .header {
  position: relative;
  z-index: 10;
}

/* 半透明背景 */
.main-content {
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(5px);
}

.header {
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
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
  background: linear-gradient(135deg, #374151 0%, #4b5563 100%);
  color: #e2e8f0;
  margin-left: auto;
  flex-direction: row-reverse;
  border-bottom-right-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
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
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
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

/* 原有样式保持不变，但增加深色主题适配 */
.step-item {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 8px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.step-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(96, 165, 250, 0.3);
}

.step-header {
  color: #60a5fa;
  font-weight: bold;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-header::before {
  content: "⚙️";
  font-size: 14px;
}

.step-result {
  color: #e2e8f0;
  line-height: 1.4;
  font-size: 14px;
}

.step-detail-result {
  margin-top: 8px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.03);
  border-left: 3px solid #60a5fa;
  border-radius: 4px;
  font-size: 13px;
  color: #cbd5e1;
  line-height: 1.5;
}

.step-detail-result strong {
  color: #60a5fa;
  font-weight: 600;
}

/* AI直接回复样式 */
.ai-direct-response {
  padding: 8px 0;
}

.ai-direct-response .ai-content {
  color: #e2e8f0;
  line-height: 1.6;
}

/* 当前正在处理的步骤样式 */
.current-step {
  border-color: rgba(96, 165, 250, 0.5);
  background: rgba(96, 165, 250, 0.1);
}

.current-step .step-header {
  color: #93c5fd;
}

.current-step .step-header::before {
  content: "🔄";
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.page-title {
  color: #e2e8f0;
}

.back-btn {
  color: #60a5fa;
  text-decoration: none;
  transition: color 0.2s;
}

.back-btn:hover {
  color: #93c5fd;
}

/* 停止按钮样式 */
.stop-button {
  position: absolute;
  left: clamp(8px, 1.5vw, 12px);
  bottom: clamp(8px, 1.5vw, 12px);
  background-color: #ff4444;
  color: white;
  border: none;
  border-radius: clamp(4px, 1vw, 6px);
  padding: clamp(6px, 1.5vw, 8px) clamp(10px, 2vw, 12px);
  font-size: clamp(10px, 2vw, 12px);
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s ease;
  white-space: nowrap;
}
 
.stop-button:hover {
  background-color: #cc3333;
  transform: scale(1.02);
}
 
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
  padding: 10px 12px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 18px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 40px;
  flex-shrink: 0;
}

.send-button:hover {
  background: #2563eb;
  transform: translateY(-1px);
}

.chat-input-container {
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 20px;
  flex-shrink: 0;
}

/* 页面布局样式 */
.main-content {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  flex-shrink: 0;
}

.chat-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.send-button:disabled {
  background: #64748b;
  cursor: not-allowed;
  transform: none;
}

/* AI内容格式化样式 */
.ai-content {
  line-height: 1.8;
}

.ai-content .ai-h3 {
  color: #60a5fa;
  font-size: 18px;
  font-weight: bold;
  margin: 16px 0 8px 0;
  padding-bottom: 4px;
  border-bottom: 2px solid rgba(96, 165, 250, 0.3);
}

.ai-content .ai-h4 {
  color: #93c5fd;
  font-size: 16px;
  font-weight: bold;
  margin: 12px 0 6px 0;
}

.ai-content .ai-bold {
  color: #fbbf24;
  font-weight: bold;
}

.ai-content .ai-list-item {
  margin: 4px 0;
  padding-left: 8px;
  color: #e2e8f0;
}

.ai-content .ai-list-item:hover {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
  padding: 4px 8px;
}

/* 思考过程框样式 */
.thinking-box {
  margin-bottom: 16px;
  border: 2px solid #4a5568;
  border-radius: 12px;
  background: rgba(45, 55, 72, 0.6);
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.thinking-box.completed {
  border-color: #6b7280;
  background: rgba(55, 65, 81, 0.5);
}

.thinking-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #4a5568;
  color: white;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  transition: background 0.3s ease;
  user-select: none;
}

.thinking-header:hover {
  background: #5a6478;
}

.thinking-icon {
  margin-right: 8px;
  font-size: 1.1em;
}

.thinking-title {
  font-size: 0.95em;
  letter-spacing: 0.5px;
  flex: 1;
}

.expand-icon {
  margin-left: 8px;
  font-size: 0.8em;
  transition: transform 0.3s ease;
  transform: rotate(-90deg);
}

.expand-icon.expanded {
  transform: rotate(0deg);
}

.thinking-content {
  max-height: 300px;
  overflow-y: auto;
  padding: 16px;
  font-size: 0.9em;
  line-height: 1.6;
  transition: max-height 0.3s ease;
}

.thinking-content::-webkit-scrollbar {
  width: 6px;
}

.thinking-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
}

.thinking-content::-webkit-scrollbar-thumb {
  background: #4a5568;
  border-radius: 3px;
}

.thinking-content::-webkit-scrollbar-thumb:hover {
  background: #5a6478;
}

.thinking-text {
  margin-bottom: 12px;
}

.thinking-label {
  font-weight: 600;
  color: #cbd5e1;
  margin-bottom: 6px;
  font-size: 0.85em;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.thinking-value {
  color: #e2e8f0;
  line-height: 1.7;
  padding: 8px 12px;
  background: rgba(55, 65, 81, 0.6);
  border-radius: 6px;
  border-left: 3px solid #6b7280;
}

.thinking-log {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  border-left: 3px solid transparent;
}

.log-type {
  font-weight: 600;
  font-size: 0.8em;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  color: white;
}

.log-type.tool-start {
  background: #00b894;
}

.log-type.tool-args {
  background: #0984e3;
}

.log-type.tool-result {
  background: #e17055;
}

.thinking-log.tool-start {
  background: rgba(0, 184, 148, 0.15);
  border-left-color: #00b894;
}

.thinking-log.tool-args {
  background: rgba(9, 132, 227, 0.15);
  border-left-color: #0984e3;
}

.thinking-log.tool-result {
  background: rgba(225, 112, 85, 0.15);
  border-left-color: #e17055;
}

.log-content {
  color: #cbd5e1;
  line-height: 1.6;
  margin-top: 6px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.85em;
  background: rgba(255, 255, 255, 0.05);
  padding: 8px 10px;
  border-radius: 4px;
  word-break: break-all;
  white-space: pre-wrap;
}

/* 最终回复区域样式 */
.final-response {
  border: 2px solid #374151;
  border-radius: 12px;
  background: rgba(31, 41, 55, 0.6);
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.final-response.completed {
  border-color: #4b5563;
  background: rgba(45, 55, 72, 0.5);
}

.final-response-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #374151;
  color: white;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.response-icon {
  margin-right: 8px;
  font-size: 1.1em;
}

.response-title {
  font-size: 0.95em;
  letter-spacing: 0.5px;
}

.final-response-content {
  padding: 16px;
}

.final-response-content .ai-content {
  color: #e2e8f0;
}
</style>

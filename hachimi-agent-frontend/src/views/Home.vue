<template>
  <div class="home-wrapper">
    <!-- 动态背景效果 -->
    <div class="background-effects">
      <!-- 浮动粒子 -->
      <div class="particles-container">
        <div v-for="i in 40" :key="i" class="particle" :style="getParticleStyle(i)"></div>
      </div>
      
      <!-- 渐变光晕 -->
      <div class="gradient-orbs">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
        <div class="orb orb-3"></div>
        <div class="orb orb-4"></div>
      </div>
      
      <!-- 网格背景 -->
      <div class="grid-background"></div>
      
      <!-- 流星效果 -->
      <div class="meteors-container">
        <div v-for="i in 3" :key="i" class="meteor" :style="getMeteorStyle(i)"></div>
      </div>
      
      <!-- 脉冲波纹 -->
      <div class="pulse-waves">
        <div class="pulse-wave pulse-wave-1"></div>
        <div class="pulse-wave pulse-wave-2"></div>
        <div class="pulse-wave pulse-wave-3"></div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="home-container">
      <!-- Hero区域 -->
      <div class="hero-section">
        <div class="hero-badge">
          <span class="badge-text">🚀 AI-Powered Platform</span>
        </div>
        
        <h1 class="hero-title">
          <span class="title-highlight">Hachimi</span> Agent
        </h1>
        
        <p class="hero-subtitle">
          下一代智能AI助手平台
        </p>
        
        <p class="hero-description">
          集成先进AI技术，提供专业写作指导与智能任务处理服务。
          <br>让AI成为您生活和工作的最佳伙伴。
        </p>
        
        <!-- 统计数据 -->
        <div class="stats-section">
          <div class="stat-item">
            <div class="stat-number">10K+</div>
            <div class="stat-label">用户信赖</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">24/7</div>
            <div class="stat-label">在线服务</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">99.9%</div>
            <div class="stat-label">可用性</div>
          </div>
        </div>
      </div>

      <!-- 交互式对话示例 -->
      <div class="demo-section">
        <h2 class="section-title">体验AI对话</h2>
        <div class="chat-demo">
          <div class="demo-chat-container">
            <div class="demo-chat-header">
              <div class="demo-header-info">
                <div class="demo-avatar">🤖</div>
                <div class="demo-title">
                  <h4>Hachimi AI助手</h4>
                  <span class="demo-status">在线</span>
                </div>
              </div>
              <div class="demo-actions">
                <button @click="clearDemo" class="demo-clear-btn" title="清空对话">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                  </svg>
                </button>
              </div>
            </div>
            
            <div class="demo-chat-messages" ref="demoChatMessages">
              <div v-for="message in demoMessages" :key="message.id" 
                   :class="['demo-message', message.type]">
                <div class="demo-message-avatar">
                  {{ message.type === 'user' ? '👤' : '🤖' }}
                </div>
                <div class="demo-message-content">
                  <div class="demo-message-text">{{ message.text }}</div>
                  <div class="demo-message-time">{{ message.time }}</div>
                </div>
              </div>
              
              <!-- 正在输入提示 -->
              <div v-if="isTyping" class="demo-message ai">
                <div class="demo-message-avatar">🤖</div>
                <div class="demo-message-content">
                  <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="demo-chat-input">
              <div class="demo-input-wrapper">
                <input 
                  v-model="demoInput" 
                  @keyup.enter="sendDemoMessage"
                  :disabled="isTyping || hasUsedDemo"
                  :placeholder="hasUsedDemo ? '演示体验已结束，请使用完整版功能...' : '试试问我一些问题...'"
                  class="demo-input"
                />
                <button 
                  @click="sendDemoMessage" 
                  :disabled="!demoInput.trim() || isTyping || hasUsedDemo"
                  class="demo-send-btn"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
                  </svg>
                </button>
              </div>
              
              <!-- 快速回复建议 -->
              <div class="demo-quick-replies" v-if="demoMessages.length === 0 && !hasUsedDemo">
                <button 
                  v-for="suggestion in quickReplies" 
                  :key="suggestion"
                  @click="sendDemoMessage(suggestion)"
                  class="quick-reply-btn"
                >
                  {{ suggestion }}
                </button>
              </div>
              
              <!-- 演示结束提示 -->
              <div class="demo-end-notice" v-if="hasUsedDemo">
                <p>🎉 演示体验完成！想要体验完整功能，请选择上方的AI助手开始使用。</p>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 功能卡片区域 -->
      <div class="features-section">
        <h2 class="section-title">选择您的AI助手</h2>
        
        <div class="app-grid">
          <router-link to="/love-app" class="app-card love-card" aria-label="进入AI写作助手应用">
            <div class="card-icon">
              ✍️
            </div>
            <div class="card-content">
              <h3 class="app-card-title">AI 写作助手</h3>
              <p class="app-card-description">
                专业的写作指导和创作建议，基于文学理论，为您提供个性化的写作指导方案
              </p>
              <div class="card-features">
                <span class="feature-tag">创作指导</span>
                <span class="feature-tag">个性化建议</span>
                <span class="feature-tag">24/7咨询</span>
              </div>
            </div>
            <div class="card-arrow">→</div>
          </router-link>
          
          <router-link to="/manus-app" class="app-card manus-card" aria-label="进入AI超级智能体应用">
            <div class="card-icon">
              🤖
            </div>
            <div class="card-content">
              <h3 class="app-card-title">AI 超级智能体</h3>
              <p class="app-card-description">
                强大的多功能AI助手，支持复杂任务处理、智能分析和自动化工作流程
              </p>
              <div class="card-features">
                <span class="feature-tag">任务自动化</span>
                <span class="feature-tag">智能分析</span>
                <span class="feature-tag">多模态支持</span>
              </div>
            </div>
            <div class="card-arrow">→</div>
          </router-link>
        </div>
      </div>
      
      <!-- 特性展示区域 -->
      <div class="highlights-section">
        <h2 class="section-title">为什么选择 Hachimi Agent？</h2>
        <div class="highlights-grid">
          <div class="highlight-item">
            <div class="highlight-icon">⚡</div>
            <h4>超快响应</h4>
            <p>毫秒级AI响应，实时交互体验</p>
          </div>
          <div class="highlight-item">
            <div class="highlight-icon">🔒</div>
            <h4>隐私安全</h4>
            <p>端到端加密，保护您的隐私数据</p>
          </div>
          <div class="highlight-item">
            <div class="highlight-icon">🎯</div>
            <h4>精准智能</h4>
            <p>基于最新GPT技术，理解更准确</p>
          </div>
          <div class="highlight-item">
            <div class="highlight-icon">🔄</div>
            <h4>持续学习</h4>
            <p>AI模型不断优化，服务越来越好</p>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 底部版权信息 -->
    <footer class="home-footer">
      <div class="footer-content">
        <div class="footer-main">
          <div class="footer-brand">
            <h3>Hachimi Agent</h3>
            <p>智能AI助手平台</p>
          </div>
          
          <div class="footer-links">
            <div class="link-group">
              <h4>产品</h4>
              <a href="/love-app">AI写作助手</a>
              <a href="/manus-app">AI超级智能体</a>
            </div>
            <div class="link-group">
              <h4>支持</h4>
              <a href="/help">帮助中心</a>
              <a href="/contact">联系我们</a>
            </div>
            <div class="link-group">
              <h4>关于</h4>
              <a href="/about">关于我们</a>
              <a href="/privacy">隐私政策</a>
            </div>
          </div>
        </div>
        
        <div class="footer-bottom">
          <div class="copyright">
            <p>&copy; 2025 Hachimi Agent. All rights reserved.</p>
            <p>Powered by Advanced AI Technology</p>
          </div>
          <div class="social-links">
            <a href="#" aria-label="GitHub">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
              </svg>
            </a>
            <a href="#" aria-label="Twitter">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                <path d="M23.953 4.57a10 10 0 01-2.825.775 4.958 4.958 0 002.163-2.723c-.951.555-2.005.959-3.127 1.184a4.92 4.92 0 00-8.384 4.482C7.69 8.095 4.067 6.13 1.64 3.162a4.822 4.822 0 00-.666 2.475c0 1.71.87 3.213 2.188 4.096a4.904 4.904 0 01-2.228-.616v.06a4.923 4.923 0 003.946 4.827 4.996 4.996 0 01-2.212.085 4.936 4.936 0 004.604 3.417 9.867 9.867 0 01-6.102 2.105c-.39 0-.779-.023-1.17-.067a13.995 13.995 0 007.557 2.209c9.053 0 13.998-7.496 13.998-13.985 0-.21 0-.42-.015-.63A9.935 9.935 0 0024 4.59z"/>
              </svg>
            </a>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { generateChatId } from '@/utils/chatService'

// 对话示例相关
const demoMessages = ref([])
const demoInput = ref('')
const isTyping = ref(false)
const demoChatMessages = ref(null)
const hasUsedDemo = ref(false) // 新增：标记是否已使用过演示

// 快速回复建议
const quickReplies = [
  "你好，能介绍一下自己吗？",
  "如何使用写作助手功能？", 
  "AI超级智能体可以做什么？",
  "你能帮我分析一下问题吗？"
]

// 预设回复库
const aiResponses = {
  "你好": "你好！我是Hachimi AI助手，很高兴为您服务！我可以帮您处理各种问题，包括写作指导、任务分析等。",
  "介绍": "我是Hachimi AI助手，集成了先进的人工智能技术。我有两个主要功能：写作助手和AI超级智能体。写作助手可以提供专业的写作指导，AI超级智能体可以帮您分析和处理复杂任务。",
  "写作": "写作助手功能可以帮您：\\n• 创意写作指导\\n• 文学创作建议\\n• 提升写作技巧\\n• 解决写作困惑\\n\\n点击写作助手卡片即可开始使用！",
  "智能体": "AI超级智能体具有强大的分析能力：\\n• 分步骤分析复杂问题\\n• 提供详细解决方案\\n• 多维度思考问题\\n• 个性化建议\\n\\n点击AI超级智能体开始体验！",
  "功能": "我的主要功能包括：\\n✍️ 写作指导：提供专业的创作建议\\n🧠 智能分析：分步骤解决复杂问题\\n💬 对话交互：自然流畅的交流体验\\n📱 跨平台：支持各种设备使用",
  "帮助": "当然可以！我很乐意帮助您。请告诉我您遇到的具体问题，我会根据问题类型为您提供最合适的解决方案。您可以选择使用写作助手或AI超级智能体功能。"
}

// 生成粒子样式
const getParticleStyle = (index) => {
  const delay = Math.random() * 15
  const duration = 15 + Math.random() * 20
  const size = 1 + Math.random() * 3
  const left = Math.random() * 100
  
  return {
    left: `${left}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    width: `${size}px`,
    height: `${size}px`
  }
}

// 生成流星样式
const getMeteorStyle = (index) => {
  const delay = Math.random() * 10 + index * 3
  const duration = 3 + Math.random() * 2
  const startX = Math.random() * 100
  const startY = Math.random() * 50
  
  return {
    left: `${startX}%`,
    top: `${startY}%`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

// 获取AI回复
const getAIResponse = (userMessage) => {
  const message = userMessage.toLowerCase()
  
  // 关键词匹配
  for (const keyword in aiResponses) {
    if (message.includes(keyword)) {
      return aiResponses[keyword].replace(/\\n/g, '\n')
    }
  }
  
  // 默认回复
  const defaultResponses = [
    "这是一个很有趣的问题！让我来为您分析一下...",
    "我理解您的想法。基于我的分析，我建议...",
    "这个问题确实需要仔细考虑。从多个角度来看...",
    "很好的问题！让我为您提供一些专业的建议..."
  ]
  
  return defaultResponses[Math.floor(Math.random() * defaultResponses.length)]
}

// 发送示例消息
const sendDemoMessage = (message = null) => {
  // 检查是否已经使用过演示
  if (hasUsedDemo.value) {
    return
  }

  const text = message || demoInput.value.trim()
  if (!text || isTyping.value) return
  
  // 标记已使用演示
  hasUsedDemo.value = true
  
  // 添加用户消息
  const userMessage = {
    id: generateChatId(),
    type: 'user',
    text: text,
    time: new Date().toLocaleTimeString('zh-CN', { 
      hour: '2-digit', 
      minute: '2-digit' 
    })
  }
  
  demoMessages.value.push(userMessage)
  demoInput.value = ''
  
  // 滚动到底部
  nextTick(() => {
    if (demoChatMessages.value) {
      demoChatMessages.value.scrollTop = demoChatMessages.value.scrollHeight
    }
  })
  
  // 模拟AI回复
  isTyping.value = true
  
  setTimeout(() => {
    const aiMessage = {
      id: generateChatId(),
      type: 'ai',
      text: getAIResponse(text),
      time: new Date().toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    }
    
    demoMessages.value.push(aiMessage)
    isTyping.value = false
    
    // 添加演示结束提示
    setTimeout(() => {
      const endMessage = {
        id: generateChatId(),
        type: 'ai',
        text: '💡 演示体验结束！想要更多功能请使用完整版AI助手。点击上方卡片开始您的AI之旅！',
        time: new Date().toLocaleTimeString('zh-CN', { 
          hour: '2-digit', 
          minute: '2-digit' 
        })
      }
      demoMessages.value.push(endMessage)
      
      // 再次滚动到底部
      nextTick(() => {
        if (demoChatMessages.value) {
          demoChatMessages.value.scrollTop = demoChatMessages.value.scrollHeight
        }
      })
    }, 1000)
    
    // 滚动到底部
    nextTick(() => {
      if (demoChatMessages.value) {
        demoChatMessages.value.scrollTop = demoChatMessages.value.scrollHeight
      }
    })
  }, 1500 + Math.random() * 2000) // 1.5-3.5秒随机延迟
}

// 清空对话
const clearDemo = () => {
  demoMessages.value = []
  demoInput.value = ''
  isTyping.value = false
  hasUsedDemo.value = false // 重置使用状态
}

// SEO设置
const updateMetaTags = () => {
  document.title = 'Hachimi Agent - 智能AI助手平台'
  
  const updateMetaTag = (name, content) => {
    let meta = document.querySelector(`meta[name="${name}"]`)
    if (!meta) {
      meta = document.createElement('meta')
      meta.name = name
      document.head.appendChild(meta)
    }
    meta.content = content
  }
  
  const updateMetaProperty = (property, content) => {
    let meta = document.querySelector(`meta[property="${property}"]`)
    if (!meta) {
      meta = document.createElement('meta')
      meta.setAttribute('property', property)
      document.head.appendChild(meta)
    }
    meta.content = content
  }
  
  updateMetaTag('description', 'Hachimi Agent是专业的AI智能助手平台，提供AI写作助手和AI超级智能体服务，帮助用户解决写作问题和处理各种智能任务')
  updateMetaTag('keywords', 'AI助手,人工智能,聊天机器人,AI写作助手,智能体,写作指导,智能任务处理')
  
  updateMetaProperty('og:title', 'Hachimi Agent - 智能AI助手平台')
  updateMetaProperty('og:description', '专业的AI助手平台，提供写作指导和智能任务处理服务')
  updateMetaProperty('og:url', window.location.href)
}

// 组件挂载
onMounted(() => {
  // 设置SEO标签
  updateMetaTags()
  
  // 添加初始欢迎消息
  setTimeout(() => {
    const welcomeMessage = {
      id: generateChatId(),
      type: 'ai',
      text: '欢迎使用Hachimi AI助手！我是您的智能伙伴，可以帮您解决各种问题。请随时向我提问！',
      time: new Date().toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    }
    demoMessages.value.push(welcomeMessage)
  }, 800)
})
</script>

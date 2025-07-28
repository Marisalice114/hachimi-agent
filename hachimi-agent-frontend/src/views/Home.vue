<template>
  <div class="home-wrapper">
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
          集成先进AI技术，提供专业恋爱咨询与智能任务处理服务。
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
      
      <!-- 功能卡片区域 -->
      <div class="features-section">
        <h2 class="section-title">选择您的AI助手</h2>
        
        <div class="app-grid">
          <router-link to="/love-app" class="app-card love-card" aria-label="进入AI恋爱大师应用">
            <div class="card-icon">
              💕
            </div>
            <div class="card-content">
              <h3 class="app-card-title">AI 恋爱大师</h3>
              <p class="app-card-description">
                专业的恋爱咨询和情感建议，基于心理学理论，为您提供个性化的情感指导方案
              </p>
              <div class="card-features">
                <span class="feature-tag">情感分析</span>
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
              <a href="/love-app">AI恋爱大师</a>
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

<script>
export default {
  name: 'Home',
  mounted() {
    // 设置页面特定的meta标签
    this.updateMetaTags()
  },
  methods: {
    updateMetaTags() {
      // 更新页面标题
      document.title = 'Hachimi Agent - 智能AI助手平台'
      
      // 更新或添加meta标签
      this.updateMetaTag('description', 'Hachimi Agent是专业的AI智能助手平台，提供AI恋爱大师和AI超级智能体服务，帮助用户解决恋爱问题和处理各种智能任务')
      this.updateMetaTag('keywords', 'AI助手,人工智能,聊天机器人,AI恋爱大师,智能体,恋爱咨询,智能任务处理')
      
      // 更新Open Graph标签
      this.updateMetaProperty('og:title', 'Hachimi Agent - 智能AI助手平台')
      this.updateMetaProperty('og:description', '专业的AI助手平台，提供恋爱咨询和智能任务处理服务')
      this.updateMetaProperty('og:url', window.location.href)
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
    }
  }
}
</script>

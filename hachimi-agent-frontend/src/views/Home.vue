<template>
  <div class="home-container">
    <div>
      <h1 class="home-title">Hachimi Agent</h1>
      <p class="home-subtitle">专业的AI智能助手平台，提供恋爱咨询和智能任务处理服务</p>
    </div>
    
    <div class="app-grid">
      <router-link to="/love-app" class="app-card" aria-label="进入AI恋爱大师应用">
        <h3 class="app-card-title">AI 恋爱大师</h3>
        <p class="app-card-description">
          专业的恋爱咨询和情感建议，帮您解决恋爱中的各种问题，提供个性化的情感指导
        </p>
      </router-link>
      
      <router-link to="/manus-app" class="app-card" aria-label="进入AI超级智能体应用">
        <h3 class="app-card-title">AI 超级智能体</h3>
        <p class="app-card-description">
          强大的多功能AI助手，为您提供全方位的智能服务，包括任务处理和问题解决
        </p>
      </router-link>
    </div>
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

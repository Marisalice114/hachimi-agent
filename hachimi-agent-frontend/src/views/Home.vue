<template>
  <div class="home-container">
    <div>
      <h1 class="home-title">Hachimi Agent</h1>
      <p class="home-subtitle">选择您要使用的AI应用</p>
    </div>
    
    <div class="app-grid">
      <router-link to="/love-app" class="app-card">
        <h3 class="app-card-title">AI 恋爱大师</h3>
        <p class="app-card-description">
          专业的恋爱咨询和情感建议，帮您解决恋爱中的各种问题
        </p>
      </router-link>
      
      <router-link to="/manus-app" class="app-card">
        <h3 class="app-card-title">AI 超级智能体</h3>
        <p class="app-card-description">
          强大的多功能AI助手，为您提供全方位的智能服务
        </p>
      </router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home'
}
</script>

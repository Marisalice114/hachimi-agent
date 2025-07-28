<template>
  <div class="error-container">
    <div class="error-content">
      <div class="error-animation">
        <div class="error-number">404</div>
        <div class="error-icon">🤖</div>
      </div>
      
      <h1 class="error-title">页面未找到</h1>
      <p class="error-description">
        抱歉，您访问的页面不存在。可能是链接错误或页面已被移动。
      </p>
      
      <div class="error-actions">
        <router-link to="/" class="btn-primary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>
          </svg>
          返回首页
        </router-link>
        
        <button @click="goBack" class="btn-secondary">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
          </svg>
          返回上页
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound',
  mounted() {
    document.title = '页面未找到 - Hachimi Agent'
  },
  methods: {
    goBack() {
      if (window.history.length > 1) {
        this.$router.go(-1)
      } else {
        this.$router.push('/')
      }
    }
  }
}
</script>

<style scoped>
.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 2rem;
  background: linear-gradient(135deg, #0f1011, #1a1b1e);
}

.error-content {
  text-align: center;
  max-width: 500px;
}

.error-animation {
  position: relative;
  margin-bottom: 2rem;
}

.error-number {
  font-size: clamp(4rem, 12vw, 8rem);
  font-weight: 900;
  color: #19c37d;
  opacity: 0.8;
  line-height: 1;
  margin-bottom: 1rem;
}

.error-icon {
  font-size: 3rem;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.error-title {
  font-size: clamp(1.5rem, 4vw, 2rem);
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 1rem;
}

.error-description {
  color: #8e8ea0;
  line-height: 1.6;
  margin-bottom: 2rem;
  font-size: clamp(0.875rem, 2.5vw, 1rem);
}

.error-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.btn-primary,
.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  font-weight: 600;
  text-decoration: none;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 0.875rem;
}

.btn-primary {
  background: #19c37d;
  color: #ffffff;
}

.btn-primary:hover {
  background: #16a069;
  transform: translateY(-2px);
}

.btn-secondary {
  background: transparent;
  color: #8e8ea0;
  border: 1px solid #343541;
}

.btn-secondary:hover {
  color: #ffffff;
  border-color: #565869;
  background: #343541;
}

@media (max-width: 480px) {
  .error-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .btn-primary,
  .btn-secondary {
    width: 100%;
    justify-content: center;
    max-width: 200px;
  }
}
</style>

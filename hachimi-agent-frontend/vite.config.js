import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  
  // 路径别名配置
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  
  // 基础路径配置
  base: '/',
  
  // 构建配置
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    
    // 代码分割
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router'],
          'utils': ['axios', 'js-cookie']
        }
      }
    }
  },
  
  // 开发服务器配置
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true
      }
    }
  },
  
  // 预览环境配置（本地预览构建结果时使用）
  preview: {
    port: 3000,
    proxy: {
      '/api': {
        // 本地预览时可以测试真实的后端API
        target: 'https://hachimi-agent-176198-5-1371623266.sh.run.tcloudbase.com',
        changeOrigin: true,
        secure: true
      }
    }
  }
})
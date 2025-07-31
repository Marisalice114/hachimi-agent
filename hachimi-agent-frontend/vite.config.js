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
        target: 'https://hachimi-write-back-176198-5-1371623266.sh.run.tcloudbase.com', // 使用您的后端公网地址
        changeOrigin: true,
        secure: true, // HTTPS后端需要设置为true
        // 移除 rewrite，保持原始路径
        ws: true, // 启用WebSocket代理（对SSE也有帮助）
        // 添加超时配置
        timeout: 120000, // 增加到120秒，适应AI生成时间
        // 代理日志（开发环境可以启用）
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('🔴 代理错误:', err.message);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('📤 代理请求:', req.method, req.url, '-> https://hachimi-write-back-176198-5-1371623266.sh.run.tcloudbase.com' + req.url);
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('📥 代理响应:', proxyRes.statusCode, req.url);
          });
        }
        //   });
        //   proxy.on('proxyReq', (proxyReq, req, res) => {
        //     console.log('Sending Request to the Target:', req.method, req.url);
        //   });
        //   proxy.on('proxyRes', (proxyRes, req, res) => {
        //     console.log('Received Response from the Target:', proxyRes.statusCode, req.url);
        //   });
        // }
      }
    }
  },
  
  // 预览环境配置
  preview: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'https://hachimi-write-back-176198-5-1371623266.sh.run.tcloudbase.com',
        changeOrigin: true,
        secure: true,
        ws: true, // 启用WebSocket代理
        // SSE专用配置
        timeout: 0, // 无超时限制
        // 代理配置（生产环境已注释详细日志）
        // configure: (proxy, options) => {
        //   proxy.on('error', (err, req, res) => {
        //     console.log('Proxy error:', err);
        //   });
        //   proxy.on('proxyReq', (proxyReq, req, res) => {
        //     console.log('Proxying request:', req.method, req.url);
        //     // 为SSE请求设置特殊头部
        //     if (req.url?.includes('/sse/')) {
        //       proxyReq.setHeader('Connection', 'keep-alive');
        //       proxyReq.setHeader('Cache-Control', 'no-cache');
        //     }
        //   });
        //   proxy.on('proxyRes', (proxyRes, req, res) => {
        //     console.log('Proxy response:', proxyRes.statusCode, req.url);
        //     // 为SSE响应设置特殊头部
        //     if (req.url?.includes('/sse/')) {
        //       res.setHeader('Connection', 'keep-alive');
        //       res.setHeader('Cache-Control', 'no-cache');
        //     }
        //   });
        // }
      }
    }
  }
})
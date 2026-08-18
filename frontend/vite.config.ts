import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 개발 서버: /api·/health 를 Spring Boot(8080)로 프록시. 빌드 산출물은 dist/.
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/health': { target: 'http://localhost:8080', changeOrigin: true }
    }
  },
  build: { outDir: 'dist' }
})

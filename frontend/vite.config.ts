import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 개발 서버: /api·/health 를 Spring Boot(8080)로 프록시. 빌드 산출물은 dist/.
// prod 는 context-path(/yeokkeum) 아래 서빙되므로 base 를 맞춘다(VITE_BASE 로 오버라이드 가능).
export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  base: mode === 'production' ? '/yeokkeum/' : '/',
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/health': { target: 'http://localhost:8080', changeOrigin: true }
    }
  },
  build: { outDir: 'dist' }
}))

import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'


export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // Backend mặc định ở cổng 8081; đổi bằng biến môi trường VITE_API_TARGET nếu backend chạy cổng khác
        target: process.env.VITE_API_TARGET || 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})

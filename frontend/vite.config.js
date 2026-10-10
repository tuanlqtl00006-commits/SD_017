import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  // jsqr (đọc mã QR căn cước) chỉ được nạp khi bấm "Quét căn cước": khai báo sẵn để Vite biên dịch ngay lúc khởi động,
  // tránh lỗi "Outdated Optimize Dep" / "Failed to fetch dynamically imported module" ở lần quét đầu tiên khi chạy npm run dev.
  optimizeDeps: {
    include: ['jsqr'],
  },
  // Kiểm thử: npm test (vitest + jsdom)
  test: {
    environment: 'jsdom',
    include: ['tests/**/*.test.js'],
  },
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

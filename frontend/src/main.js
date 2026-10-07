import { createApp } from 'vue'
import './style.css'
import './assets/admin.css'
import App from './App.vue'
import router from './router'

createApp(App).use(router).mount('#app')

// Bấm vào bất kỳ chỗ nào trên ô ngày cũng mở lịch (không cần bấm đúng icon nhỏ)
document.addEventListener('click', (e) => {
  const el = e.target
  if (el instanceof HTMLInputElement && (el.type === 'date' || el.type === 'datetime-local')
      && !el.disabled && !el.readOnly && typeof el.showPicker === 'function') {
    try { el.showPicker() } catch { /* trình duyệt chặn thì bỏ qua */ }
  }
})

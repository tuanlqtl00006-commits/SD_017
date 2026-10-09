import { createApp } from 'vue'
import './style.css'
import './assets/admin.css'
import App from './App.vue'
import router from './router'

createApp(App).use(router).mount('#app')

// Bấm vào bất kỳ chỗ nào trên ô ngày cũng mở lịch (không cần bấm đúng icon nhỏ)
// Ô nào có thuộc tính data-no-auto-picker thì bỏ qua: bấm vào để gõ tay, chỉ bấm icon lịch mới mở lịch
document.addEventListener('click', (e) => {
  const el = e.target
  if (el instanceof HTMLInputElement && (el.type === 'date' || el.type === 'datetime-local')
      && !el.disabled && !el.readOnly && !el.hasAttribute('data-no-auto-picker')
      && typeof el.showPicker === 'function') {
    try { el.showPicker() } catch { /* trình duyệt chặn thì bỏ qua */ }
  }
})

import { reactive } from 'vue'
import { thuocTinhService } from '../services/thuocTinhService'

/**
 * Tải các bảng thuộc tính cần dùng cho dropdown và tra tên theo id.
 * types: ['danh-muc', 'thuong-hieu', ...]  (slug trong constants/thuocTinh.js)
 */
export function useThuocTinh(types) {
  const data = reactive(Object.fromEntries(types.map((t) => [t, []])))

  async function loadThuocTinh() {
    const results = await Promise.all(types.map((t) => thuocTinhService.getAll(t)))
    types.forEach((t, i) => { data[t] = results[i] })
  }

  /** Tên theo id; không thấy thì trả '—'. */
  const tenOf = (type, id) => data[type].find((x) => x.id === id)?.ten ?? '—'

  /**
   * Danh sách cho dropdown: chỉ các mục đang hoạt động,
   * cộng thêm mục đang được chọn (kể cả đã ngưng) để khi sửa không bị mất giá trị.
   */
  const optionsOf = (type, currentId = null) => data[type].filter((x) => x.hoatDong || x.id === currentId)

  return { data, loadThuocTinh, tenOf, optionsOf }
}

import { THUOC_TINH } from '../constants/thuocTinh'
// import api from './api'
//
// HIỆN TẠI: dữ liệu mẫu lưu trong bộ nhớ (backend chưa có API), tải lại trang sẽ về dữ liệu ban đầu.
// KHI CÓ API: giữ nguyên tên hàm và dữ liệu trả về, chỉ thay phần thân bằng lời gọi api, ví dụ:
//   getAll: async (type) => (await api.get(`/${type}`)).data      // type = 'danh-muc', 'thuong-hieu', ...

const delay = (ms = 120) => new Promise((resolve) => setTimeout(resolve, ms))

const stores = {}
const nextIds = {}
for (const [type, cfg] of Object.entries(THUOC_TINH)) {
  stores[type] = cfg.seed
    .map((ten, i) => ({ id: i + 1, ma: `${cfg.prefix}${String(i + 1).padStart(3, '0')}`, ten, hoatDong: true }))
    .reverse()
  nextIds[type] = cfg.seed.length + 1
}

function ensure(type) {
  if (!stores[type]) throw new Error('Loại thuộc tính không hợp lệ.')
}

export const thuocTinhService = {
  async getAll(type) {
    ensure(type)
    await delay()
    return stores[type].map((x) => ({ ...x }))
  },

  async create(type, payload) {
    ensure(type)
    await delay()
    if (stores[type].some((x) => x.ma === payload.ma)) throw new Error('Mã đã tồn tại.')
    if (stores[type].some((x) => x.ten.toLowerCase() === payload.ten.toLowerCase())) throw new Error('Tên đã tồn tại.')
    const item = { ...payload, id: nextIds[type]++, hoatDong: true }
    stores[type] = [item, ...stores[type]]
    return { ...item }
  },

  async update(type, id, payload) {
    ensure(type)
    await delay()
    const index = stores[type].findIndex((x) => x.id === id)
    if (index === -1) throw new Error('Không tìm thấy bản ghi.')
    if (stores[type].some((x) => x.id !== id && x.ten.toLowerCase() === payload.ten.toLowerCase())) throw new Error('Tên đã tồn tại.')
    stores[type][index] = { ...stores[type][index], ten: payload.ten, id }
    return { ...stores[type][index] }
  },

  async toggleActive(type, id) {
    ensure(type)
    await delay()
    const item = stores[type].find((x) => x.id === id)
    if (!item) throw new Error('Không tìm thấy bản ghi.')
    item.hoatDong = !item.hoatDong
    return { ...item }
  },
}

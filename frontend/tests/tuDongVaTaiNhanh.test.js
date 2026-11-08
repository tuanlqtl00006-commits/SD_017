import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const { api } = vi.hoisted(() => ({ api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))

vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('vue-router', async (goc) => ({
  ...(await goc()),
  useRoute: () => ({ fullPath: '/', query: {}, params: {} }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}))

import { manCoTheMo } from '../src/router'
import { cuaHangService } from '../src/services/cuaHangService'
import { nhanTin } from '../src/services/suKien'
import { useAuth, _datLaiAuth } from '../src/composables/useAuth'
import { _datLaiGiaoCa } from '../src/composables/useGiaoCa'
import { ghiDonHang } from '../src/utils/donHangLocal'
import DotGiamGiaLichCard from '../src/components/dotGiamGia/DotGiamGiaLichCard.vue'
import GiaoCaModal from '../src/components/giaoCa/GiaoCaModal.vue'
import AccountView from '../src/views/client/AccountView.vue'

let gan = []
const ganVao = (c, props) => {
  const w = mount(c, { attachTo: document.body, props })
  gan.push(w)
  return w
}
const coTin = async (...chuDe) => {
  nhanTin({ chuDe, nguon: 'may-khac' })
  await new Promise((r) => setTimeout(r, 200))
  await flushPromises()
}
const soLanGoi = (doan) => api.get.mock.calls.filter((c) => c[0].includes(doan)).length

beforeEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
  _datLaiAuth()
  _datLaiGiaoCa()
  for (const f of Object.values(api)) f.mockReset()
})
afterEach(() => {
  gan.forEach((w) => w.unmount())
  gan = []
})

describe('Nạp sẵn màn hình', () => {
  const man = (path, roles) => ({ path, meta: roles ? { roles } : {}, components: { default: () => path } })
  const ds = [
    man('/shop/san-pham'),
    man('/thong-ke', ['quan-ly']),
    man('/ban-hang', ['nhan-vien', 'thu-ngan', 'quan-ly']),
    man('/ho-tro-noi-bo'),
    { path: '/shop', meta: {}, components: { default: {} } }, // đã nạp sẵn (layout)
    { path: '/admin', meta: {} }, // chỉ chuyển hướng
  ]
  const duongDan = (vaiTro) => manCoTheMo(ds, vaiTro).map((nap) => nap())

  it('khách và người chưa đăng nhập chỉ nạp phần cửa hàng', () => {
    expect(duongDan(null)).toEqual(['/shop/san-pham'])
    expect(duongDan('khach-hang')).toEqual(['/shop/san-pham'])
  })

  it('nhân viên không nạp màn chỉ quản lý được vào; quản lý nạp đủ', () => {
    expect(duongDan('nhan-vien')).toEqual(['/shop/san-pham', '/ban-hang', '/ho-tro-noi-bo'])
    expect(duongDan('quan-ly')).toEqual(['/shop/san-pham', '/thong-ke', '/ban-hang', '/ho-tro-noi-bo'])
  })
})

describe('Danh mục cửa hàng', () => {
  it('nhiều phần của trang cùng cần thì chỉ hỏi máy chủ một lần; lần sau vẫn lấy mới', async () => {
    const xong = []
    api.get.mockImplementation(() => new Promise((r) => xong.push(() => r({ data: { danhMuc: [{ id: 1, ten: 'Vợt' }] } }))))
    const a = cuaHangService.getDanhMucVaThuongHieu()
    const b = cuaHangService.getDanhMucVaThuongHieu()
    expect(api.get).toHaveBeenCalledTimes(1)
    xong[0]()
    expect((await a).danhMuc[0].ten).toBe('Vợt')
    expect(await b).toBe(await a)

    const c = cuaHangService.getDanhMucVaThuongHieu()
    expect(api.get).toHaveBeenCalledTimes(2)
    xong[1]()
    await c
  })
})

describe('Tự cập nhật, không phải bấm', () => {
  it('lịch đợt giảm giá: đợt đổi thì tải lại và không nhấp nháy trạng thái đang tải; tin khác thì không', async () => {
    let nhaXong = null
    api.get.mockImplementation(async (url) => ({ data: url.includes('che-do') ? { cheDo: 'MAX' } : [] }))
    const w = ganVao(DotGiamGiaLichCard, { idDot: 1 })
    await flushPromises()
    expect(soLanGoi('/lich-giam-gia')).toBe(1)

    await coTin('hoa-don')
    expect(soLanGoi('/lich-giam-gia')).toBe(1)

    api.get.mockImplementation((url) => {
      if (url.includes('che-do')) return Promise.resolve({ data: { cheDo: 'MAX' } })
      return new Promise((r) => {
        nhaXong = () => r({ data: [] })
      })
    })
    await coTin('giam-gia')
    expect(soLanGoi('/lich-giam-gia')).toBe(2)
    expect(w.find('button').attributes('disabled')).toBeUndefined() // nút "Tải lại" không bị khóa = không ở trạng thái đang tải
    nhaXong()
    await flushPromises()
  })

  it('hộp Ca làm việc đang mở: có hóa đơn mới thì số liệu ca tự cập nhật', async () => {
    useAuth().trangThai.nguoiDung = { id: 3, hoTen: 'Lê Quốc Bảo', vaiTro: 'NHAN_VIEN' }
    api.get.mockImplementation(async () => ({ data: { dangMo: null, tongKet: null, lichHomNay: [] } }))
    ganVao(GiaoCaModal)
    await flushPromises()
    const truoc = soLanGoi('/giao-ca/hien-tai')
    expect(truoc).toBeGreaterThan(0)
    await coTin('hoa-don')
    expect(soLanGoi('/giao-ca/hien-tai')).toBe(truoc + 1)
  })

  it('đơn của khách: cửa hàng xác nhận thì trạng thái tự đổi; đang ở tab khác thì không hỏi', async () => {
    const auth = useAuth()
    auth.trangThai.daKhoiTao = true
    auth.trangThai.nguoiDung = { id: 8, hoTen: 'Khách A', vaiTro: 'KHACH_HANG' }
    ghiDonHang([{ maHoaDon: 'HD900', trangThai: 0, trangThaiText: 'Chờ xác nhận', items: [], tongTien: 1000 }])
    let trangThaiDon = 1
    api.get.mockImplementation(async (url) => {
      if (url.startsWith('/khach/don-hang/')) return { data: { trangThai: trangThaiDon, chiTiet: [] } }
      return { data: url === '/khach/tai-khoan' ? {} : [] }
    })
    const w = ganVao(AccountView)
    await flushPromises()
    expect(w.text()).toContain('Chờ xác nhận')

    trangThaiDon = 2
    await coTin('hoa-don')
    expect(w.text()).toContain('Đã xác nhận')

    const truoc = soLanGoi('/khach/don-hang/')
    const nutHoSo = w.findAll('button').find((b) => /thông tin|hồ sơ/i.test(b.text()))
    await nutHoSo.trigger('click')
    await coTin('hoa-don')
    await new Promise((r) => setTimeout(r, 50))
    expect(soLanGoi('/khach/don-hang/')).toBe(truoc)
  })
})

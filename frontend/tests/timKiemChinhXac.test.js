import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { mockApi } from './helpersDanhMuc'
import { includesExact, includesText } from '../src/utils/text'
import { THUOC_TINH as CAU_HINH } from '../src/constants/thuocTinh'

let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock.api[k](...a) }) }))

const t = (id, ma, ten, hoatDong = true) => ({ id, ma, ten, hoatDong })
const DU_LIEU = {
  'thuong-hieu': [
    t(1, 'TH01', 'Yonex'), t(2, 'TH02', 'Victor'), t(3, 'TH03', 'Li-Ning'), t(4, 'TH04', 'adidas'),
    t(5, 'TH05', 'ASICS'), t(6, 'TH06', 'Lining Pro'), t(7, 'TH07', 'Ngưng Hiệu', false),
  ],
  'xuat-xu': [
    t(1, 'XX01', 'Nhật Bản'), t(2, 'XX02', 'Trung Quốc'), t(3, 'XX03', 'Việt Nam'), t(4, 'XX04', 'Đài Loan'),
    t(5, 'XX05', 'nhật bản (hàng cũ)'),
  ],
  'mau-sac': [t(1, 'MS01', 'Đen'), t(2, 'MS02', 'Đỏ'), t(3, 'MS03', 'Xanh Dương')],
}

beforeEach(() => {
  mock = mockApi({ get: (url) => { const m = /^\/thuoc-tinh\/(.+)$/.exec(url); if (m && DU_LIEU[m[1]]) return DU_LIEU[m[1]] } })
  document.body.innerHTML = ''
})

/** Mở trang quản lý thuộc tính của một loại, trả về wrapper + hàm gõ ô tìm kiếm + hàm đọc tên đang hiển thị. */
async function moTrang(loai) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: `/san-pham/${loai}`, meta: { attr: loai }, component: { template: '<div/>' } }] })
  router.push(`/san-pham/${loai}`); await router.isReady()
  const Comp = (await import('../src/components/ThuocTinhManager.vue')).default
  const w = mount(Comp, { global: { plugins: [router] } }); await flushPromises()
  const go = async (chu) => { await w.find('#tt-keyword').setValue(chu); await flushPromises() }
  // Cột Tên là cột thứ 3 (STT, Mã, Tên, ...)
  const ten = () => w.findAll('tbody tr').filter((r) => r.findAll('td').length > 1).map((r) => r.findAll('td')[2].text())
  const ma = () => w.findAll('tbody tr').filter((r) => r.findAll('td').length > 1).map((r) => r.findAll('td')[1].text())
  return { w, go, ten, ma }
}

describe('includesExact: so khớp đúng từng chữ', () => {
  it('phân biệt chữ hoa / thường', () => {
    expect(includesExact('Yonex', 'Yonex')).toBe(true)
    expect(includesExact('Yonex', 'yonex')).toBe(false)
    expect(includesExact('Yonex', 'YONEX')).toBe(false)
    expect(includesExact('Yonex', 'Y')).toBe(true)
    expect(includesExact('Yonex', 'y')).toBe(false)
    expect(includesExact('adidas', 'Adidas')).toBe(false)
    expect(includesExact('ASICS', 'asics')).toBe(false)
    expect(includesExact('ASICS', 'ASICS')).toBe(true)
  })
  it('phân biệt dấu tiếng Việt', () => {
    expect(includesExact('Nhật Bản', 'Nhật Bản')).toBe(true)
    expect(includesExact('Nhật Bản', 'Nhat Ban')).toBe(false)
    expect(includesExact('Nhật Bản', 'Nhật Ban')).toBe(false)
    expect(includesExact('Đài Loan', 'Dai Loan')).toBe(false)
    expect(includesExact('Đài Loan', 'Đài')).toBe(true)
    expect(includesExact('Đài Loan', 'đài')).toBe(false)
  })
  it('dấu gõ dạng tổ hợp (NFD) vẫn khớp chữ dựng sẵn (NFC), không bị "gõ đúng mà không ra"', () => {
    const nfd = 'Nhật Bản'.normalize('NFD')
    expect(nfd).not.toBe('Nhật Bản')
    expect(includesExact('Nhật Bản', nfd)).toBe(true)
    expect(includesExact(nfd, 'Nhật Bản')).toBe(true)
    expect(includesExact('Nhật Bản', 'Nhat'.normalize('NFD'))).toBe(false)
  })
  it('khoảng trắng thừa được bỏ, nhưng ký tự vẫn phải khớp', () => {
    expect(includesExact('Li-Ning', '  Li-Ning  ')).toBe(true)
    expect(includesExact('Nhật Bản', 'Nhật    Bản')).toBe(true)
    expect(includesExact('Li-Ning', 'Li Ning')).toBe(false)
    expect(includesExact('Li-Ning', 'LiNing')).toBe(false)
  })
  it('từ khóa rỗng / chỉ khoảng trắng -> khớp hết; dữ liệu rỗng không gây lỗi', () => {
    expect(includesExact('Yonex', '')).toBe(true)
    expect(includesExact('Yonex', '   ')).toBe(true)
    expect(includesExact(undefined, 'Y')).toBe(false)
    expect(includesExact(null, '')).toBe(true)
  })
  it('ô tìm cũ (includesText) vẫn không phân biệt hoa thường để các trang khác không bị ảnh hưởng', () => {
    expect(includesText('Yonex', 'yonex')).toBe(true)
    expect(includesText('Nhật Bản', 'nhat ban')).toBe(true)
  })
})

describe('Cấu hình: chỉ thương hiệu và xuất xứ bắt buộc đúng từng chữ', () => {
  it('cờ timChinhXac', () => {
    expect(CAU_HINH['thuong-hieu'].timChinhXac).toBe(true)
    expect(CAU_HINH['xuat-xu'].timChinhXac).toBe(true)
    for (const k of ['danh-muc', 'chat-lieu', 'do-cung', 'diem-can-bang', 'mau-sac', 'trong-luong', 'chu-vi']) {
      expect(CAU_HINH[k].timChinhXac).toBeFalsy()
    }
  })
})

describe('Trang Thương hiệu: gõ vào ô tìm kiếm', () => {
  it('chưa gõ gì -> hiện đủ danh sách và có dòng gợi ý phân biệt hoa/thường', async () => {
    const { w, ten } = await moTrang('thuong-hieu')
    // Có 7 thương hiệu, mỗi trang 5 dòng: trang 1 hiện 5 dòng đầu, tổng vẫn báo 7 bản ghi
    expect(ten()).toEqual(['Yonex', 'Victor', 'Li-Ning', 'adidas', 'ASICS'])
    expect(w.find('.ad-count').text()).toContain('7 bản ghi')
    expect(w.find('[data-test="goi-y-chinh-xac"]').exists()).toBe(true)
  })
  it('viết hoa đầu: "Yonex" ra, "yonex" / "YONEX" không ra', async () => {
    const { go, ten, w } = await moTrang('thuong-hieu')
    await go('Yonex'); expect(ten()).toEqual(['Yonex'])
    await go('yonex'); expect(ten()).toEqual([]); expect(w.text()).toContain('Không tìm thấy thương hiệu')
    await go('YONEX'); expect(ten()).toEqual([])
    await go('Y'); expect(ten()).toEqual(['Yonex'])
    await go('y'); expect(ten()).toEqual([])
  })
  it('thương hiệu viết thường "adidas" chỉ ra khi gõ đúng thường; "Adidas" không ra', async () => {
    const { go, ten } = await moTrang('thuong-hieu')
    await go('adidas'); expect(ten()).toEqual(['adidas'])
    await go('Adidas'); expect(ten()).toEqual([])
    await go('ASICS'); expect(ten()).toEqual(['ASICS'])
    await go('Asics'); expect(ten()).toEqual([])
  })
  it('ký tự đặc biệt: "Li-Ning" khác "Li Ning", "Lining"', async () => {
    const { go, ten } = await moTrang('thuong-hieu')
    await go('Li-Ning'); expect(ten()).toEqual(['Li-Ning'])
    await go('li-ning'); expect(ten()).toEqual([])
    await go('LI-NING'); expect(ten()).toEqual([])
    await go('Li Ning'); expect(ten()).toEqual([])
    await go('Lining'); expect(ten()).toEqual(['Lining Pro'])
  })
  it('gõ một phần đúng chữ vẫn ra (đang gõ dở): "Vic" -> Victor', async () => {
    const { go, ten } = await moTrang('thuong-hieu')
    await go('Vic'); expect(ten()).toEqual(['Victor'])
    await go('vic'); expect(ten()).toEqual([])
  })
  it('mã cũng phải đúng chữ: "TH02" ra, "th02" không ra; không "dính" qua ranh giới mã | tên', async () => {
    const { go, ten, ma } = await moTrang('thuong-hieu')
    await go('TH02'); expect(ma()).toEqual(['TH02']); expect(ten()).toEqual(['Victor'])
    await go('th02'); expect(ten()).toEqual([])
    await go('02 Victor'); expect(ten()).toEqual([])
  })
  it('thừa khoảng trắng đầu / cuối vẫn ra', async () => {
    const { go, ten } = await moTrang('thuong-hieu')
    await go('  Yonex  '); expect(ten()).toEqual(['Yonex'])
    await go('  yonex  '); expect(ten()).toEqual([])
  })
  it('xóa chữ trong ô hoặc bấm "Đặt lại bộ lọc" -> hiện lại đủ danh sách', async () => {
    const { w, go, ten } = await moTrang('thuong-hieu')
    await go('Victor'); expect(ten()).toEqual(['Victor'])
    await go(''); expect(ten().length).toBe(5)
    await go('xyz'); expect(ten()).toEqual([])
    const nutDatLai = w.findAll('button').find((b) => b.text().includes('Đặt lại bộ lọc'))
    await nutDatLai.trigger('click'); await flushPromises()
    expect(w.find('#tt-keyword').element.value).toBe('')
    expect(ten().length).toBe(5)
  })
  it('kết hợp với lọc trạng thái: tìm "Ngưng" trong mục đã ngưng', async () => {
    const { w, go, ten } = await moTrang('thuong-hieu')
    await w.find('#tt-trang-thai').setValue('NGUNG'); await flushPromises()
    await go('Ngưng'); expect(ten()).toEqual(['Ngưng Hiệu'])
    await go('Ngung'); expect(ten()).toEqual([])
    await go('ngưng'); expect(ten()).toEqual([])
  })
})

describe('Trang Xuất xứ (Nơi sản xuất): gõ vào ô tìm kiếm', () => {
  it('đúng chữ và đúng dấu mới ra', async () => {
    const { go, ten, w } = await moTrang('xuat-xu')
    expect(w.find('[data-test="goi-y-chinh-xac"]').exists()).toBe(true)
    await go('Nhật Bản'); expect(ten()).toEqual(['Nhật Bản'])
    await go('Nhật'); expect(ten()).toEqual(['Nhật Bản'])
  })
  it('sai hoa/thường hoặc thiếu dấu thì không ra', async () => {
    const { go, ten } = await moTrang('xuat-xu')
    await go('nhật bản'); expect(ten()).toEqual(['nhật bản (hàng cũ)'])  // chỉ ra mục tên thật sự viết thường
    await go('NHẬT BẢN'); expect(ten()).toEqual([])
    await go('Nhat Ban'); expect(ten()).toEqual([])
    await go('Dai Loan'); expect(ten()).toEqual([])
    await go('Đài Loan'); expect(ten()).toEqual(['Đài Loan'])
    await go('đài loan'); expect(ten()).toEqual([])
  })
  it('gõ dấu dạng tổ hợp (NFD) vào ô tìm kiếm vẫn ra đúng, còn sai hoa/thường thì không', async () => {
    const { go, ten } = await moTrang('xuat-xu')
    const nfd = (x) => x.normalize('NFD')
    expect(nfd('Nhật Bản')).not.toBe('Nhật Bản')
    await go(nfd('Nhật Bản')); expect(ten()).toEqual(['Nhật Bản'])
    await go(nfd('nhật bản')); expect(ten()).toEqual(['nhật bản (hàng cũ)'])
    await go(nfd('Nhat Ban')); expect(ten()).toEqual([])
  })
  it('hai mục chỉ khác hoa/thường được tách bạch: "Nhật Bản" không lẫn "nhật bản (hàng cũ)"', async () => {
    const { go, ten } = await moTrang('xuat-xu')
    await go('Nhật Bản'); expect(ten()).not.toContain('nhật bản (hàng cũ)')
    await go('nhật bản'); expect(ten()).not.toContain('Nhật Bản')
  })
})

describe('Các thuộc tính khác vẫn tìm thoải mái (không ảnh hưởng)', () => {
  it('Màu sắc: gõ "den", "ĐEN", "xanh duong" đều ra', async () => {
    const { go, ten, w } = await moTrang('mau-sac')
    expect(w.find('[data-test="goi-y-chinh-xac"]').exists()).toBe(false)
    await go('den'); expect(ten()).toEqual(['Đen'])
    await go('ĐEN'); expect(ten()).toEqual(['Đen'])
    await go('xanh duong'); expect(ten()).toEqual(['Xanh Dương'])
  })
})

import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

// Góp ý 23/11 (Task 3.1): trang "Gợi ý" dạng trắc nghiệm từng bước, có thanh tiến trình, Quay lại / Tiếp tục, cuối cùng lọc sản phẩm
const { api } = vi.hoisted(() => ({ api: { get: vi.fn() } }))
vi.mock('../src/services/api', () => ({ default: api }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))

import { CAU_HOI, dsThuongHieu, goiYSanPham } from '../src/utils/goiYTracNghiem'
import GoiYSanPhamView from '../src/views/client/GoiYSanPhamView.vue'

const bt = (tl, gia, ton = 5) => ({ trongLuong: tl, giaBan: gia, soLuongTon: ton })
const SP = [
  { id: 1, tenSanPham: 'Vợt mềm nhẹ đầu', tenThuongHieu: 'Kumpoo', tenDanhMuc: 'Vợt cầu lông người mới', doCung: 'Mềm', diemCanBang: 'Nhẹ đầu', tongSoLuongTon: 5, phanTramGiamMax: 0, bienThe: [bt('5U (75-79g)', 900000)] },
  { id: 2, tenSanPham: 'Vợt cứng nặng đầu', tenThuongHieu: 'Yonex', tenDanhMuc: 'Vợt cầu lông', doCung: 'Cứng', diemCanBang: 'Nặng đầu', tongSoLuongTon: 5, phanTramGiamMax: 10, bienThe: [bt('3U (85-89g)', 3500000), bt('4U (80-84g)', 3600000)] },
  { id: 3, tenSanPham: 'Vợt trung bình cân bằng', tenThuongHieu: 'Lining', tenDanhMuc: 'Vợt cầu lông', doCung: 'Trung bình', diemCanBang: 'Cân bằng', tongSoLuongTon: 5, phanTramGiamMax: 0, bienThe: [bt('4U (80-84g)', 1500000)] },
  { id: 4, tenSanPham: 'Vợt hết hàng', tenThuongHieu: 'Victor', tenDanhMuc: 'Vợt cầu lông người mới', doCung: 'Mềm', diemCanBang: 'Nhẹ đầu', tongSoLuongTon: 0, phanTramGiamMax: 0, bienThe: [bt('5U (75-79g)', 800000, 0)] },
]

describe('Chấm điểm gợi ý', () => {
  it('người mới + phòng thủ + dưới 1 triệu: chỉ mẫu trong ngân sách, còn hàng; ghi lý do hợp', () => {
    const kq = goiYSanPham(SP, { trinhDo: 'moi', loiChoi: 'phongThu', nganSach: 'duoi1', trongLuong: 'khongRo', thuongHieu: [] })
    expect(kq.ngoaiNganSach).toBe(false)
    expect(kq.ds.map((x) => x.sp.id)).toEqual([1]) // mẫu 4 hết hàng thì bỏ
    expect(kq.ds[0].lyDo).toEqual(['Thân mềm hợp người mới', 'Dòng vợt cho người mới', 'Nhẹ đầu - hợp lối phòng thủ', 'Còn hàng 5U'])
  })

  it('nâng cao + tấn công + 3U + hãng Yonex: mẫu phù hợp nhất lên đầu', () => {
    const kq = goiYSanPham(SP, { trinhDo: 'nangCao', loiChoi: 'tanCong', nganSach: 'tuyY', trongLuong: '3U', thuongHieu: ['Yonex'] })
    expect(kq.ds[0].sp.id).toBe(2)
    expect(kq.ds[0].lyDo).toContain('Hãng Yonex')
    expect(kq.ds[0].lyDo).toContain('Còn hàng 3U')
    expect(kq.ds.map((x) => x.sp.id)).not.toContain(4)
  })

  it('không có mẫu trong ngân sách: gợi ý các mẫu giá gần nhất và báo ngoài ngân sách', () => {
    const kq = goiYSanPham(SP, { trinhDo: 'phongTrao', loiChoi: 'toanDien', nganSach: 'tren4', trongLuong: '4U', thuongHieu: [] })
    expect(kq.ngoaiNganSach).toBe(true)
    expect(kq.ds[0].sp.id).toBe(2) // 3.500.000 gần 4 triệu nhất
  })

  it('thương hiệu lấy từ sản phẩm còn hàng; có đủ 5 câu hỏi', () => {
    expect(dsThuongHieu(SP)).toEqual(['Kumpoo', 'Lining', 'Yonex'])
    expect(CAU_HOI.map((c) => c.khoa)).toEqual(['trinhDo', 'loiChoi', 'nganSach', 'trongLuong', 'thuongHieu'])
  })
})

describe('Trang Gợi ý từng bước', () => {
  it('thanh tiến trình, phải chọn mới Tiếp tục, Quay lại giữ lựa chọn, cuối cùng hiện sản phẩm phù hợp', async () => {
    api.get.mockResolvedValue({ data: SP })
    const w = mount(GoiYSanPhamView, { global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } } })
    await flushPromises()
    const nutTiep = () => w.get('[data-testid="gy-tiep"]')
    const chon = async (chu) => {
      await w.findAll('[data-testid="gy-lua-chon"]').find((b) => b.text().includes(chu)).trigger('click')
    }
    expect(w.get('[data-testid="gy-buoc"]').text()).toBe('Câu 1 / 5')
    expect(w.get('[data-testid="gy-quay-lai"]').attributes('disabled')).toBeDefined()
    expect(nutTiep().attributes('disabled')).toBeDefined() // chưa chọn
    await chon('Mới chơi')
    await nutTiep().trigger('click')
    expect(w.get('[data-testid="gy-cau-hoi"]').text()).toBe('Lối chơi của bạn?')
    await w.get('[data-testid="gy-quay-lai"]').trigger('click')
    expect(w.findAll('[data-testid="gy-lua-chon"]').find((b) => b.text().includes('Mới chơi')).classes()).toContain('is-chon')
    await nutTiep().trigger('click')
    await chon('Phòng thủ')
    await nutTiep().trigger('click')
    await chon('Dưới 1.000.000')
    await nutTiep().trigger('click')
    await chon('Chưa xác định')
    await nutTiep().trigger('click')
    // Câu thương hiệu: được bỏ trống
    expect(w.findAll('[data-testid="gy-lua-chon"]').map((b) => b.text())).toEqual(['Kumpoo', 'Lining', 'Yonex'])
    expect(nutTiep().text()).toContain('Xem gợi ý')
    await nutTiep().trigger('click')
    expect(w.text()).toContain('100%')
    expect(w.get('[data-testid="gy-ket-qua"]').text()).toContain('Vợt mềm nhẹ đầu')
    expect(w.text()).toContain('Hãng nào cũng được')
    await w.get('[data-testid="gy-lam-lai"]').trigger('click')
    expect(w.get('[data-testid="gy-buoc"]').text()).toBe('Câu 1 / 5')
  })
})

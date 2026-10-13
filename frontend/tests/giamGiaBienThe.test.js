import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { addDaysIso } from '../src/utils/format'

/*
 * Kiểm tra luồng: thêm biến thể vào đợt giảm giá (Đợt giảm giá -> Sản phẩm áp dụng) rồi mở trang Biến thể sản phẩm,
 * cột "Giảm giá" phải tự hiện. Backend giả bên dưới tính giảm theo ĐÚNG quy tắc của GiamGiaApDungService:
 * chỉ đợt đang bật (trangThai = 1) và hôm nay nằm trong [ngày bắt đầu, ngày kết thúc]; nhiều đợt chồng nhau thì lấy mức cao nhất.
 */
let db
let mock
vi.mock('../src/services/api', () => ({ default: new Proxy({}, { get: (_, k) => (...a) => mock[k](...a) }) }))

const iso = (n) => `${addDaysIso(n)}T00:00:00`
const DOT = (id, ma, bd, kt, trangThai = 1, pt = 12) => ({ id, maDot: ma, tenDot: 'Đợt ' + ma, phanTramGiamDot: pt, ngayBatDau: iso(bd), ngayKetThuc: iso(kt), trangThai })

function taoDb() {
  return {
    dots: [DOT(1, 'DGG001', -3, 4), DOT(2, 'DGG002', 10, 20), DOT(3, 'DGG003', -3, 4, 0), DOT(4, 'DGG004', -30, -10)],
    chiTiet: [], // { id, idDot, idSanPhamChiTiet, phanTramGiamBienThe }
    bienThes: [1, 2, 3, 4].map((id) => ({ id, ma: `SP001-BT${id}`, idSanPham: 1, idMauSac: 1, idTrongLuong: 1, idChuVi: 1, giaBan: 1000000, soLuongTon: 5, hoatDong: true })),
    seq: 100,
  }
}
const homNay = () => addDaysIso(0)
const ngay = (v) => v.substring(0, 10)
const dangDienRa = (d) => d.trangThai === 1 && ngay(d.ngayBatDau) <= homNay() && homNay() <= ngay(d.ngayKetThuc)

function backend() {
  return {
    get: vi.fn(async (url) => {
      let m
      if (url === '/dot-giam-gia/ap-dung-hom-nay') {
        const kq = {}
        for (const c of db.chiTiet) {
          const d = db.dots.find((x) => x.id === c.idDot)
          if (dangDienRa(d)) kq[c.idSanPhamChiTiet] = Math.max(kq[c.idSanPhamChiTiet] || 0, c.phanTramGiamBienThe)
        }
        return { data: kq }
      }
      if (url === '/dot-giam-gia/ap-dung') {
        const muc = db.chiTiet.map((c) => ({ c, d: db.dots.find((x) => x.id === c.idDot) }))
          .filter(({ d }) => d.trangThai === 1 && ngay(d.ngayKetThuc) >= homNay())
          .map(({ c, d }) => ({ idBienThe: c.idSanPhamChiTiet, idDot: d.id, maDot: d.maDot, tenDot: d.tenDot, phanTram: c.phanTramGiamBienThe, ngayBatDau: ngay(d.ngayBatDau), ngayKetThuc: ngay(d.ngayKetThuc) }))
        return { data: { chinhSach: 'MAX', muc } }
      }
      if (url === '/bien-the-san-pham') return { data: db.bienThes }
      if ((m = /^\/dot-giam-gia-chi-tiet\/campaign\/(\d+)$/.exec(url))) return { data: db.chiTiet.filter((c) => c.idDot === Number(m[1])) }
      if (url === '/san-pham') return { data: [{ id: 1, ma: 'SP001', ten: 'Vợt A', idDanhMuc: 1, hoatDong: true }] }
      const t = /^\/thuoc-tinh\/(.+)$/.exec(url)
      if (t) {
        const r = (id, ten) => ({ id, ma: 'X' + id, ten, hoatDong: true })
        return { data: { 'mau-sac': [r(1, 'Đen')], 'trong-luong': [r(1, '4U (80-84g)')], 'chu-vi': [r(1, 'G5')], 'danh-muc': [r(1, 'Vợt cầu lông')] }[t[1]] || [] }
      }
      return { data: [] }
    }),
    post: vi.fn(async (url, body) => {
      const m = /^\/dot-giam-gia-chi-tiet\/campaign\/(\d+)$/.exec(url)
      db.chiTiet.push({ id: ++db.seq, idDot: Number(m[1]), idSanPhamChiTiet: body.idSanPhamChiTiet, phanTramGiamBienThe: body.phanTramGiamBienThe })
      return { data: {} }
    }),
    delete: vi.fn(async (url) => {
      const id = Number(url.split('/').pop())
      db.chiTiet = db.chiTiet.filter((c) => c.id !== id)
      return { data: {} }
    }),
    put: vi.fn(async () => ({ data: {} })),
  }
}

beforeEach(() => { db = taoDb(); mock = backend(); document.body.innerHTML = '' })

async function themVaoDot(dot, idBienThe, phanTram) {
  const Modal = (await import('../src/components/dotGiamGia/DotGiamGiaChiTietModal.vue')).default
  const w = mount(Modal, { props: { campaign: dot }, attachTo: document.body })
  await flushPromises()
  await w.find('select').setValue(idBienThe)
  await w.find('input[type=number]').setValue(phanTram)
  await w.find('form').trigger('submit')
  await flushPromises()
  return w
}
async function moTrangBienThe() {
  const Comp = (await import('../src/components/BienTheManager.vue')).default
  const w = mount(Comp, { attachTo: document.body, global: { stubs: { RouterLink: true } } })
  await flushPromises()
  return w
}
const o = (w, ma) => w.findAll('tbody tr').find((r) => r.text().includes(ma))
const cotGiam = (w) => w.findAll('thead th').findIndex((th) => th.text() === 'Giảm giá')

describe('Cột "Giảm giá" ở trang Biến thể sản phẩm', () => {
  it('đầu cột là "Giảm giá" và có giải thích khi rê chuột', async () => {
    const w = await moTrangBienThe()
    const th = w.findAll('thead th').find((x) => x.text() === 'Giảm giá')
    expect(th).toBeTruthy()
    expect(th.attributes('title')).toContain('đợt giảm giá đang diễn ra')
    w.unmount()
  })

  it('chưa thêm vào đợt nào: tất cả hiện "—"', async () => {
    const w = await moTrangBienThe()
    const i = cotGiam(w)
    for (const ma of ['BT1', 'BT2', 'BT3', 'BT4']) expect(o(w, ma).findAll('td')[i].text()).toBe('—')
    w.unmount()
  })

  it('thêm biến thể vào đợt ĐANG DIỄN RA -> trang Biến thể tự hiện -12% (và giá sau giảm)', async () => {
    const modal = await themVaoDot(db.dots[0], 1, 12)
    expect(mock.post).toHaveBeenCalledWith('/dot-giam-gia-chi-tiet/campaign/1', { idSanPhamChiTiet: 1, phanTramGiamBienThe: 12 })
    modal.unmount()
    const w = await moTrangBienThe()
    const i = cotGiam(w)
    expect(o(w, 'BT1').findAll('td')[i].text()).toBe('-12%')
    expect(o(w, 'BT1').text()).toContain('880.000')
    expect(o(w, 'BT2').findAll('td')[i].text()).toBe('—') // biến thể khác không bị ảnh hưởng
    w.unmount()
  })

  it('mức giảm riêng của biến thể (khác % của đợt) được hiện đúng', async () => {
    const modal = await themVaoDot(db.dots[0], 2, 30)
    modal.unmount()
    const w = await moTrangBienThe()
    expect(o(w, 'BT2').findAll('td')[cotGiam(w)].text()).toBe('-30%')
    w.unmount()
  })

  it('đợt SẮP DIỄN RA: chưa giảm nhưng cột báo "Sắp giảm -x% từ dd/mm/yyyy" (không còn chỉ "—")', async () => {
    const modal = await themVaoDot(db.dots[1], 3, 25)
    modal.unmount()
    const w = await moTrangBienThe()
    const cell = o(w, 'BT3').findAll('td')[cotGiam(w)]
    expect(cell.find('[data-testid="giam-hien-tai"]').exists()).toBe(false)
    const [y, m, d] = addDaysIso(10).split('-')
    expect(cell.find('[data-testid="giam-sap-dien-ra"]').text()).toBe(`Sắp giảm -25% từ ${d}/${m}/${y}`)
    expect(o(w, 'BT3').text()).toContain('1.000.000') // vẫn bán giá gốc
    w.unmount()
  })

  it('đợt đang NGỪNG HOẠT ĐỘNG hoặc ĐÃ KẾT THÚC: không giảm, không báo sắp giảm', async () => {
    db.chiTiet.push({ id: 1, idDot: 3, idSanPhamChiTiet: 1, phanTramGiamBienThe: 12 }, { id: 2, idDot: 4, idSanPhamChiTiet: 2, phanTramGiamBienThe: 12 })
    const w = await moTrangBienThe()
    const i = cotGiam(w)
    expect(o(w, 'BT1').findAll('td')[i].text()).toBe('—')
    expect(o(w, 'BT2').findAll('td')[i].text()).toBe('—')
    w.unmount()
  })

  it('gỡ biến thể khỏi đợt -> cột Giảm giá trở lại "—"', async () => {
    const modal = await themVaoDot(db.dots[0], 1, 12)
    const gỡ = modal.find('button[title="Gỡ sản phẩm khỏi đợt"]')
    await gỡ.trigger('click'); await flushPromises()
    modal.unmount()
    const w = await moTrangBienThe()
    expect(o(w, 'BT1').findAll('td')[cotGiam(w)].text()).toBe('—')
    w.unmount()
  })

  it('biến thể nằm trong 2 đợt đang diễn ra chồng nhau: lấy mức cao nhất', async () => {
    db.dots.push(DOT(5, 'DGG005', -1, 3, 1, 20))
    db.chiTiet.push({ id: 1, idDot: 1, idSanPhamChiTiet: 4, phanTramGiamBienThe: 12 }, { id: 2, idDot: 5, idSanPhamChiTiet: 4, phanTramGiamBienThe: 20 })
    const w = await moTrangBienThe()
    expect(o(w, 'BT4').findAll('td')[cotGiam(w)].text()).toBe('-20%')
    w.unmount()
  })
})

describe('Ghi chú trạng thái trong cửa sổ "Sản phẩm áp dụng"', () => {
  const ghiChu = async (dot) => {
    const Modal = (await import('../src/components/dotGiamGia/DotGiamGiaChiTietModal.vue')).default
    const w = mount(Modal, { props: { campaign: dot }, attachTo: document.body })
    await flushPromises()
    return w
  }
  it('đang diễn ra: báo sẽ giảm ngay và hiện ở cột Giảm giá', async () => {
    const w = await ghiChu(db.dots[0])
    expect(w.get('[data-testid="ghi-chu-dot"]').text()).toContain('giảm giá ngay')
    expect(w.get('[data-testid="ghi-chu-dot"]').text()).toContain('Giảm giá')
    w.unmount()
  })
  it('sắp diễn ra: báo ngày bắt đầu được giảm', async () => {
    const w = await ghiChu(db.dots[1])
    const [y, m, d] = addDaysIso(10).split('-')
    expect(w.get('[data-testid="ghi-chu-dot"]').text()).toContain(`${d}/${m}/${y}`)
    w.unmount()
  })
  it('ngừng hoạt động: nhắc phải bật đợt', async () => {
    const w = await ghiChu(db.dots[2])
    expect(w.get('[data-testid="ghi-chu-dot"]').text()).toContain('ngừng hoạt động')
    w.unmount()
  })
  it('đã kết thúc: khóa form thêm sản phẩm', async () => {
    const w = await ghiChu(db.dots[3])
    expect(w.get('[data-testid="ghi-chu-dot"]').text()).toContain('đã kết thúc')
    expect(w.get('button[type=submit]').attributes('disabled')).toBeDefined()
    expect(w.get('select').attributes('disabled')).toBeDefined()
    w.unmount()
  })
})

describe('Hóa đơn: cột SĐT KH', () => {
  const HD = (id, ten, sdt) => ({ id, maHoaDon: 'HD' + String(id).padStart(6, '0'), maNhanVien: 'NV0001', tenKhachHang: ten, sdtKhachHang: sdt, tongTien: 1000000, loaiDon: 'Tại quầy', ngayTao: '2026-10-08T09:00:00', trangThai: 6 })
  it('hiện SĐT của khách; hóa đơn khách vãng lai có để lại SĐT cũng hiện; không có SĐT mới hiện —', async () => {
    mock.get = vi.fn(async (url) => (url === '/hoa-don/dem-trang-thai'
      ? { data: { tatCa: 3 } }
      : { data: { content: [HD(16, 'Khách vãng lai', '0912345016'), HD(14, 'Vũ Khánh Linh', '0901000006'), HD(15, 'Khách vãng lai', null)], totalElements: 3, totalPages: 1 } }))
    const Comp = (await import('../src/components/HoaDonManager.vue')).default
    const w = mount(Comp, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    const i = w.findAll('thead th').findIndex((th) => th.text() === 'SĐT KH')
    const rows = w.findAll('tbody tr')
    expect(rows[0].findAll('td')[i].text()).toBe('0912345016')
    expect(rows[1].findAll('td')[i].text()).toBe('0901000006')
    expect(rows[2].findAll('td')[i].text()).toBe('—')
    w.unmount()
  })
})

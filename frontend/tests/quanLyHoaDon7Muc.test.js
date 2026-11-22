// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import HoaDonChiTiet from '../src/components/HoaDonChiTiet.vue'
import HoaDonNoiDung from '../src/components/hoaDon/HoaDonNoiDung.vue'
import SuaGiaoHangModal from '../src/components/hoaDon/SuaGiaoHangModal.vue'
import { taoHtmlHoaDon } from '../src/utils/hoaDonIn'
import { TT } from '../src/constants/hoaDon'

let mockApi = {}
vi.mock('../src/services/api', () => ({
  default: new Proxy({}, { get: (_, k) => (...a) => (mockApi[k] ? mockApi[k](...a) : Promise.resolve({ data: {} })) }),
}))

const mockToast = { error: vi.fn(), success: vi.fn() }
vi.mock('../src/composables/useToast', () => ({
  useToast: () => mockToast,
}))

vi.mock('../src/composables/useXacNhan', () => ({
  hoiXacNhan: vi.fn(async () => true),
}))

vi.mock('../src/services/caiDatVanChuyen', () => ({
  dungCaiDatVanChuyen: () => ({ mienPhiTu: 500000, phiDongGia: 30000 }),
}))

async function taoChiTietWrapper(chiTiet, put = vi.fn(async () => ({ data: chiTiet })), post = vi.fn(async () => ({ data: chiTiet }))) {
  mockApi = { get: vi.fn(async () => ({ data: chiTiet })), put, post }
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/hoa-don', component: { template: '<div/>' } },
      { path: '/hoa-don/:id', component: { template: '<div/>' } },
    ],
  })
  router.push(`/hoa-don/${chiTiet?.hoaDon?.id || 1}`)
  await router.isReady()
  const w = mount(HoaDonChiTiet, {
    global: { plugins: [router] },
    attachTo: document.body,
  })
  await flushPromises()
  return { w, router, put, post }
}

describe('Quản lý hóa đơn - 7 mục nghiệp vụ mới', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    vi.clearAllMocks()
  })

  // 1. Thứ tự sắp xếp (FIFO) - Đơn cũ lên đầu, đơn mới xuống cuối được xử lý trong backend controller
  // 2. Mã nhân viên xử lý
  describe('Mục 2: Hiển thị mã nhân viên trên hóa đơn', () => {
    it('HoaDonNoiDung hiển thị mã nhân viên ở header và khối thông tin khách hàng', () => {
      const data = {
        hoaDon: {
          maHoaDon: 'HD001',
          tenLoaiDon: 'Đơn giao hàng',
          loaiDon: 2,
          trangThai: TT.DA_XAC_NHAN,
          maNhanVien: 'NV01',
          tenNhanVien: 'Nguyễn Văn A',
          thanhTien: 300000,
        },
        khachHang: { ten: 'Khách A', sdt: '0901234567' },
        giaoHang: { hoTenNguoiNhan: 'Khách A', sdtNguoiNhan: '0901234567', diaChi: 'Hà Nội' },
        sanPham: [],
        thanhToan: [],
      }

      const wrapper = mount(HoaDonNoiDung, {
        props: { data, cheDoKhach: false },
      })

      // Header có badge mã nhân viên
      const headerNv = wrapper.find('[data-testid="hd-header-ma-nv"]')
      expect(headerNv.exists()).toBe(true)
      expect(headerNv.text()).toContain('NV: [NV01] Nguyễn Văn A')

      // Khối thông tin admin
      const nvXuLy = wrapper.find('[data-testid="hd-nhan-vien-xu-ly"]')
      expect(nvXuLy.exists()).toBe(true)
      expect(nvXuLy.text()).toContain('[NV01]')
      expect(nvXuLy.text()).toContain('Nguyễn Văn A')
    })

    it('HoaDonNoiDung ở chế độ khách tra cứu hiển thị mã nhân viên xử lý nếu có', () => {
      const data = {
        hoaDon: {
          maHoaDon: 'HD002',
          tenLoaiDon: 'Đơn giao hàng',
          loaiDon: 2,
          trangThai: TT.DANG_GIAO,
          maNhanVien: 'NV02',
          tenNhanVien: 'Trần Thị B',
          thanhTien: 500000,
        },
        khachHang: { ten: 'Khách B', sdt: '0912345678' },
        giaoHang: { hoTenNguoiNhan: 'Khách B', sdtNguoiNhan: '0912345678', diaChi: 'Hải Phòng' },
        sanPham: [],
        thanhToan: [],
      }

      const wrapper = mount(HoaDonNoiDung, {
        props: { data, cheDoKhach: true },
      })

      const khachNv = wrapper.find('[data-testid="hd-khach-ma-nv"]')
      expect(khachNv.exists()).toBe(true)
      expect(khachNv.text()).toContain('[NV02]')
      expect(khachNv.text()).toContain('Trần Thị B')
    })

    it('Bản in hóa đơn in rõ mã nhân viên xử lý', () => {
      const html = taoHtmlHoaDon({
        hd: {
          maHoaDon: 'HD003',
          tenLoaiDon: 'Đơn trực tuyến',
          trangThai: TT.DA_XAC_NHAN,
          maNhanVien: 'NV05',
          tenNhanVien: 'Lê Văn C',
          thanhTien: 200000,
        },
        khach: { ten: 'Khách C' },
        sanPham: [],
        thanhToan: [],
      })

      expect(html).toContain('[NV05] Lê Văn C')
    })
  })

  // 3. Chỉnh sửa đơn hàng online: không cho sửa sản phẩm, chỉ cho sửa địa chỉ trong phạm vi phí ship bằng nhau
  describe('Mục 3: Chỉnh sửa đơn hàng online', () => {
    it('Đơn hàng online có duocChinhSua = false nên không hiện nút Chỉnh sửa đơn', async () => {
      const data = {
        hoaDon: {
          id: 10,
          maHoaDon: 'HD010',
          loaiDon: 2,
          tenLoaiDon: 'Trực tuyến',
          trangThai: TT.CHO_XAC_NHAN,
          duocChinhSua: false,
          duocSuaGiaoHang: true,
          chuyenDuoc: [{ trangThai: TT.DA_XAC_NHAN, ten: 'Xác nhận đơn' }],
        },
        khachHang: { ten: 'Khách D' },
        giaoHang: { diaChi: 'Hà Nội' },
        sanPham: [],
        thanhToan: [],
        lichSu: [],
      }

      const { w } = await taoChiTietWrapper(data)
      expect(w.find('[data-testid="hd-sua-don"]').exists()).toBe(false)
    })

    it('SuaGiaoHangModal: Đơn đã thanh toán trước nếu phí ship thay đổi thì hiển thị cảnh báo và khóa nút Lưu', async () => {
      const modalData = {
        daThanhToan: 350000,
        hoaDon: {
          tongTien: 320000,
          phiVanChuyen: 30000,
        },
        giaoHang: {
          hoTenNguoiNhan: 'Người nhận A',
          sdtNguoiNhan: '0900000000',
          diaChi: 'Số 1 Phố Huế, Phường Hàng Bài, Thành phố Hà Nội',
        },
      }

      const wrapper = mount(SuaGiaoHangModal, {
        props: {
          data: modalData,
          url: '/hoa-don/10/giao-hang',
        },
        global: {
          stubs: {
            BaseModal: { template: '<div><slot /><slot name="footer" /></div>' },
            DiaChiHanhChinhSelect: true,
          },
        },
      })

      // Đổi địa chỉ làm thay đổi phí ship (giả sử địa chỉ mới tỉnh khác)
      wrapper.vm.form.tinhThanh = 'Tỉnh Cà Mau'
      wrapper.vm.form.phuongXa = 'Phường 1'
      wrapper.vm.form.diaChiCuThe = 'Số 99'
      wrapper.vm.phiTheoDiaChiMoi = 60000
      await flushPromises()

      // Hiển thị cảnh báo lệch phí ship
      const warning = wrapper.find('[data-testid="sgh-lech-phi-warning"]')
      expect(warning.exists()).toBe(true)
      expect(warning.text()).toContain('chỉ cho phép đổi địa chỉ trong phạm vi phí vận chuyển bằng nhau')

      // Nút Lưu bị disabled
      const btnLuu = wrapper.find('[data-testid="sgh-luu"]')
      expect(btnLuu.attributes('disabled')).toBeDefined()
    })
  })

  // 4. Đơn chuyển khoản chưa thanh toán: Backend hỗ trợ đổi sang COD và xác nhận thanh toán
  describe('Mục 4: Admin chỉnh sửa trạng thái đơn chuyển khoản chưa thanh toán', () => {
    it('Đơn chuyển khoản có cờ duocDoiSangCod và duocXacNhanThanhToan từ máy chủ', async () => {
      const data = {
        duocDoiSangCod: true,
        duocXacNhanThanhToan: true,
        hoaDon: {
          id: 20,
          maHoaDon: 'HD020',
          loaiDon: 2,
          tenLoaiDon: 'Trực tuyến',
          trangThai: TT.CHO_XAC_NHAN,
          thanhTien: 450000,
          chuyenDuoc: [],
        },
        khachHang: {},
        giaoHang: {},
        sanPham: [],
        thanhToan: [],
        lichSu: [],
      }

      const { w } = await taoChiTietWrapper(data)
      expect(w.vm.data.duocDoiSangCod).toBe(true)
      expect(w.vm.data.duocXacNhanThanhToan).toBe(true)
    })
  })

  // 5. Đơn hàng không đủ tồn kho: Bỏ gửi email báo hết hàng, chỉ cho phép nhân viên Hủy đơn
  describe('Mục 5: Đơn thiếu tồn kho chỉ cho phép Hủy đơn', () => {
    it('Hộp thoại hết hàng không có nút Giữ hàng hay Gửi mail, chỉ có nút Hủy đơn và Đóng', async () => {
      const data = {
        duocBaoHetHang: true,
        dsThieuHang: ['Vợt Victor DriveX (kho còn 0, đơn cần 1)'],
        daThanhToan: 0,
        hoaDon: {
          id: 30,
          maHoaDon: 'HD030',
          loaiDon: 2,
          tenLoaiDon: 'Trực tuyến',
          trangThai: TT.CHO_XAC_NHAN,
          chuyenDuoc: [{ trangThai: TT.DA_XAC_NHAN, ten: 'Xác nhận đơn' }],
        },
        khachHang: {},
        giaoHang: {},
        sanPham: [],
        thanhToan: [],
        lichSu: [],
      }

      const { w } = await taoChiTietWrapper(data)

      // Bấm nút Xác nhận đơn sẽ mở modal thiếu hàng
      const btnXacNhan = w.findAll('button').find((b) => b.text().includes('Xác nhận đơn'))
      expect(btnXacNhan).toBeTruthy()
      await btnXacNhan.trigger('click')
      await flushPromises()

      // Kiểm tra modal hết hàng
      const modal = document.querySelector('[data-testid="hd-het-hang"]')
      expect(modal).not.toBeNull()
      expect(document.querySelector('[data-testid="hd-het-hang-giu"]')).toBeNull()
      expect(document.querySelector('[data-testid="hd-het-hang-mail"]')).toBeNull()
      expect(document.querySelector('[data-testid="hd-het-hang-hoan-ok"]')).not.toBeNull()
    })
  })

  // 6. Xác nhận tiền đơn COD: Trước khi chuyển sang Hoàn thành, bắt buộc phải có bước xác nhận đã nhận tiền
  describe('Mục 6: Xác nhận tiền đơn COD trước khi hoàn thành', () => {
    it('Bấm Hoàn thành đơn COD: Hiện thông báo xác nhận đã nhận tiền, xác nhận xong mới mở popup hoàn thành đơn', async () => {
      const { hoiXacNhan } = await import('../src/composables/useXacNhan')
      const data = {
        duocXacNhanTienCod: true,
        hoaDon: {
          id: 40,
          maHoaDon: 'HD040',
          loaiDon: 2,
          tenLoaiDon: 'Trực tuyến',
          trangThai: TT.DA_GIAO,
          thanhTien: 600000,
          chuyenDuoc: [{ trangThai: TT.HOAN_THANH, ten: 'Hoàn thành đơn' }],
        },
        khachHang: {},
        giaoHang: {},
        sanPham: [],
        thanhToan: [],
        lichSu: [],
      }

      const post = vi.fn(async () => ({ data: { ...data, duocXacNhanTienCod: false } }))
      const { w } = await taoChiTietWrapper(data, vi.fn(), post)

      const btnHoanThanh = w.findAll('button').find((b) => b.text().includes('Hoàn thành đơn'))
      expect(btnHoanThanh).toBeTruthy()
      await btnHoanThanh.trigger('click')
      await flushPromises()

      // Đã gọi hộp thoại xác nhận đã nhận tiền
      expect(hoiXacNhan).toHaveBeenCalledWith(expect.objectContaining({
        title: expect.stringContaining('Xác nhận đã nhận tiền'),
      }))
      // Đã gọi API xác nhận tiền COD
      expect(post).toHaveBeenCalledWith('/hoa-don/40/xac-nhan-tien-cod')
    })
  })

  // 7. Hoàn phí (Refund): Bổ sung xử lý hoàn tiền lại cho khách khi đơn đã trả tiền trước bị hủy
  describe('Mục 7: Hoàn tiền khi đơn đã trả trước bị hủy', () => {
    it('Đơn hủy có cờ duocXacNhanHoanTien hiển thị nút Xác nhận đã hoàn tiền', async () => {
      const data = {
        duocXacNhanHoanTien: true,
        soTienCanHoan: 750000,
        hoaDon: {
          id: 50,
          maHoaDon: 'HD050',
          loaiDon: 2,
          tenLoaiDon: 'Trực tuyến',
          trangThai: TT.DA_HUY,
          chuyenDuoc: [],
        },
        khachHang: {},
        giaoHang: {},
        sanPham: [],
        thanhToan: [
          { id: 1, phuongThuc: 'Chuyển khoản QR', soTien: 750000, trangThai: 1 },
          { id: 2, phuongThuc: 'Hoàn tiền cho khách', soTien: -750000, trangThai: 0 },
        ],
        lichSu: [],
      }

      const { w } = await taoChiTietWrapper(data)
      const btnHoanTien = w.find('[data-testid="hd-xac-nhan-hoan-tien"]')
      expect(btnHoanTien.exists()).toBe(true)
    })
  })
})

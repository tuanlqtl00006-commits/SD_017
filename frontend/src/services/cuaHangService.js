import api from './api'
import { sanPhamService, bienTheService } from './sanPhamService'
import { dotGiamGiaService } from './dotGiamGiaService'
import { thuocTinhService } from './thuocTinhService'

function formatSanPhamCuaHang(d) {
  const bts = d.bienThe || []
  return {
    id: d.id,
    ma: d.maSanPham || d.ma || '',
    ten: d.tenSanPham || d.ten || '',
    moTa: d.moTa || '',
    idDanhMuc: d.idDanhMuc,
    tenDanhMuc: d.tenDanhMuc || '',
    idThuongHieu: d.idThuongHieu,
    brandName: d.tenThuongHieu || '',
    xuatXu: d.xuatXu || '',
    chatLieu: d.chatLieu || '',
    doCung: d.doCung || '',
    diemCanBang: d.diemCanBang || '',
    anhChinh: d.anhChinh || (d.danhSachAnh && d.danhSachAnh[0]) || '',
    danhSachAnh: d.danhSachAnh || (d.anhChinh ? [d.anhChinh] : []),
    minPrice: Number(d.giaBanMin) || 0,
    originalPrice: Number(d.giaGocMin) || 0,
    maxDiscountPercent: Number(d.phanTramGiamMax) || 0,
    hasDiscount: (Number(d.phanTramGiamMax) || 0) > 0,
    tongSoLuongTon: d.tongSoLuongTon || 0,
    bienThe: bts,
    hoatDong: 1,
  }
}

function formatChiTietCuaHang(d) {
  const variants = (d.bienThe || []).map((b) => ({
    id: b.id,
    idSanPham: d.id,
    ma: b.maSpct || b.ma || '',
    maSpct: b.maSpct || b.ma || '',
    idMauSac: b.idMauSac,
    tenMau: b.tenMau || '',
    idTrongLuong: b.idTrongLuong,
    trongLuong: b.trongLuong || '',
    idChuVi: b.idChuVi,
    chuVi: b.chuVi || '',
    giaBan: b.giaBanGoc || b.giaBan || 0,
    giaSauGiam: b.giaBan || b.giaBanGoc || 0,
    phanTramGiam: b.phanTramGiam || 0,
    soLuongTon: b.soLuongTon ?? 0,
    hoatDong: 1,
  }))

  const colorMap = new Map()
  const weightMap = new Map()
  const gripMap = new Map()

  for (const v of variants) {
    if (v.idMauSac && v.tenMau) {
      colorMap.set(v.idMauSac, { id: v.idMauSac, ten: v.tenMau })
    }
    if (v.idTrongLuong && v.trongLuong) {
      weightMap.set(v.idTrongLuong, { id: v.idTrongLuong, ten: v.trongLuong })
    }
    if (v.idChuVi && v.chuVi) {
      gripMap.set(v.idChuVi, { id: v.idChuVi, ten: v.chuVi })
    }
  }

  const discounts = {}
  for (const v of variants) {
    if (v.phanTramGiam > 0) {
      discounts[v.id] = v.phanTramGiam
    }
  }

  return {
    product: {
      id: d.id,
      ma: d.maSanPham || d.ma || '',
      ten: d.tenSanPham || d.ten || '',
      moTa: d.moTa || '',
      anhChinh: d.anhChinh || (d.danhSachAnh && d.danhSachAnh[0]) || '',
      danhSachAnh: d.danhSachAnh || (d.anhChinh ? [d.anhChinh] : []),
      idDanhMuc: d.idDanhMuc,
      categoryName: d.tenDanhMuc || '',
      idThuongHieu: d.idThuongHieu,
      brandName: d.tenThuongHieu || '',
      originName: d.xuatXu || '',
      materialName: d.chatLieu || '',
      flexName: d.doCung || '',
      balanceName: d.diemCanBang || '',
    },
    variants,
    colors: Array.from(colorMap.values()),
    weights: Array.from(weightMap.values()),
    grips: Array.from(gripMap.values()),
    discounts,
  }
}

export const cuaHangService = {
  // 1. Lấy danh sách sản phẩm thật từ CSDL
  async getDanhSachSanPham(params = {}) {
    // Ưu tiên gọi API công khai của cửa hàng
    try {
      const res = await api.get('/cua-hang/san-pham', { params })
      if (res.data && Array.isArray(res.data) && res.data.length > 0) {
        return res.data.map(formatSanPhamCuaHang)
      }
    } catch {
      // Tiếp tục thử sanPhamService bên dưới
    }

    // Nếu /cua-hang/san-pham chưa có hoặc trống, gọi danh sách sản phẩm và biến thể thật
    try {
      const [dsSp, dsBt, dgg, thRes] = await Promise.all([
        sanPhamService.getAll().catch(() => []),
        bienTheService.getAll().catch(() => []),
        dotGiamGiaService.getApDungHomNay().catch(() => ({})),
        thuocTinhService.getAll('thuong-hieu').catch(() => []),
      ])

      if (Array.isArray(dsSp) && dsSp.length > 0) {
        return dsSp
          .filter((sp) => sp.hoatDong !== 0 && sp.hoatDong !== false)
          .map((sp) => {
            const bts = (dsBt || []).filter(
              (b) => b.idSanPham === sp.id && b.hoatDong !== 0 && b.hoatDong !== false
            )
            let minPrice = 0
            let originalPrice = 0
            let maxDiscountPercent = 0

            if (bts.length > 0) {
              let min = Infinity
              let orig = Infinity
              for (const b of bts) {
                const giaGoc = Number(b.giaBan) || 0
                const pt = Number(dgg[b.id]) || 0
                const giaSauGiam = pt > 0 ? giaGoc * (1 - pt / 100) : giaGoc
                if (giaSauGiam < min) {
                  min = giaSauGiam
                  orig = giaGoc
                }
                if (pt > maxDiscountPercent) {
                  maxDiscountPercent = pt
                }
              }
              minPrice = min === Infinity ? 0 : min
              originalPrice = orig === Infinity ? 0 : orig
            }

            const brand = (thRes || []).find((t) => t.id === sp.idThuongHieu)

            return {
              id: sp.id,
              ma: sp.ma,
              ten: sp.ten,
              idDanhMuc: sp.idDanhMuc,
              idThuongHieu: sp.idThuongHieu,
              brandName: brand?.ten || '',
              anhChinh: sp.anhChinh || (sp.anhPhu && sp.anhPhu[0]) || '',
              danhSachAnh: [sp.anhChinh, ...(sp.anhPhu || [])].filter(Boolean),
              minPrice,
              originalPrice,
              maxDiscountPercent,
              hasDiscount: maxDiscountPercent > 0,
              hoatDong: sp.hoatDong,
              moTa: sp.moTa || '',
              bienThe: bts,
            }
          })
      }
    } catch (err) {
      console.error('Lỗi khi lấy danh sách sản phẩm thật:', err)
    }

    return []
  },

  // 2. Lấy chi tiết sản phẩm thật từ CSDL theo ID
  async getChiTietSanPham(id) {
    const numId = Number(id)
    if (!numId) return null

    // Bước 1: Thử gọi API công khai của cửa hàng
    try {
      const res = await api.get(`/cua-hang/san-pham/${numId}`)
      if (res.data && res.data.id) {
        return formatChiTietCuaHang(res.data)
      }
    } catch {
      // Tiếp tục thử sanPhamService bên dưới
    }

    // Bước 2: Gọi sanPhamService + bienTheService + thuộc tính thật
    try {
      const [sp, allVariants, dgg, msRes, tlRes, cvRes, thRes, xxRes, clRes, dcRes, cbRes] =
        await Promise.all([
          sanPhamService.getById(numId),
          bienTheService.getAll().catch(() => []),
          dotGiamGiaService.getApDungHomNay().catch(() => ({})),
          thuocTinhService.getAll('mau-sac').catch(() => []),
          thuocTinhService.getAll('trong-luong').catch(() => []),
          thuocTinhService.getAll('chu-vi').catch(() => []),
          thuocTinhService.getAll('thuong-hieu').catch(() => []),
          thuocTinhService.getAll('xuat-xu').catch(() => []),
          thuocTinhService.getAll('chat-lieu').catch(() => []),
          thuocTinhService.getAll('do-cung').catch(() => []),
          thuocTinhService.getAll('diem-can-bang').catch(() => []),
        ])

      if (!sp || !sp.id) return null

      const variants = (allVariants || []).filter(
        (b) => b.idSanPham === numId && b.hoatDong !== 0 && b.hoatDong !== false
      )

      const brand = (thRes || []).find((b) => b.id === sp.idThuongHieu)
      const origin = (xxRes || []).find((x) => x.id === sp.idXuatXu)
      const material = (clRes || []).find((c) => c.id === sp.idChatLieu)
      const flex = (dcRes || []).find((d) => d.id === sp.idDoCung)
      const balance = (cbRes || []).find((b) => b.id === sp.idDiemCanBang)

      return {
        product: {
          id: sp.id,
          ma: sp.ma,
          ten: sp.ten,
          moTa: sp.moTa || '',
          anhChinh: sp.anhChinh || (sp.anhPhu && sp.anhPhu[0]) || '',
          danhSachAnh: [sp.anhChinh, ...(sp.anhPhu || [])].filter(Boolean),
          idDanhMuc: sp.idDanhMuc,
          idThuongHieu: sp.idThuongHieu,
          brandName: brand?.ten || '',
          originName: origin?.ten || '',
          materialName: material?.ten || '',
          flexName: flex?.ten || '',
          balanceName: balance?.ten || '',
        },
        variants,
        colors: msRes || [],
        weights: tlRes || [],
        grips: cvRes || [],
        discounts: dgg || {},
      }
    } catch (err) {
      console.error('Không tìm thấy sản phẩm trong CSDL:', err)
      return null
    }
  },

  // 3. Lấy danh mục và thương hiệu thật từ CSDL
  async getDanhMucVaThuongHieu() {
    try {
      const res = await api.get('/cua-hang/danh-muc-va-thuong-hieu')
      if (res.data && res.data.danhMuc && res.data.danhMuc.length > 0) {
        return res.data
      }
    } catch {
      // fallback
    }

    try {
      const [dmList, thList] = await Promise.all([
        thuocTinhService.getAll('danh-muc').catch(() => []),
        thuocTinhService.getAll('thuong-hieu').catch(() => []),
      ])

      const activeDm = (dmList || [])
        .filter((d) => d.trangThai !== 0 && d.trangThai !== false && d.hoatDong !== 0)
        .map((d) => ({ id: d.id, ten: d.ten || d.tenDanhMuc || '' }))

      const activeTh = (thList || [])
        .filter((t) => t.trangThai !== 0 && t.trangThai !== false && t.hoatDong !== 0)
        .map((t) => ({ id: t.id, ten: t.ten || t.tenThuongHieu || '' }))

      return {
        danhMuc: activeDm.length > 0 ? activeDm : [
          { id: 1, ten: 'Vợt cầu lông' },
          { id: 2, ten: 'Vợt cầu lông người mới' },
          { id: 3, ten: 'Quần áo thể thao' },
          { id: 4, ten: 'Túi - balo' },
          { id: 5, ten: 'Phụ kiện' },
          { id: 6, ten: 'Quả cầu lông' },
        ],
        thuongHieu: activeTh.length > 0 ? activeTh : [
          { id: 1, ten: 'Yonex' },
          { id: 2, ten: 'Lining' },
          { id: 3, ten: 'Victor' },
          { id: 4, ten: 'Mizuno' },
          { id: 5, ten: 'Kumpoo' },
        ],
      }
    } catch (err) {
      console.error('Lỗi khi lấy danh mục & thương hiệu:', err)
      return {
        danhMuc: [
          { id: 1, ten: 'Vợt cầu lông' },
          { id: 2, ten: 'Vợt cầu lông người mới' },
          { id: 3, ten: 'Quần áo thể thao' },
          { id: 4, ten: 'Túi - balo' },
          { id: 5, ten: 'Phụ kiện' },
          { id: 6, ten: 'Quả cầu lông' },
        ],
        thuongHieu: [
          { id: 1, ten: 'Yonex' },
          { id: 2, ten: 'Lining' },
          { id: 3, ten: 'Victor' },
          { id: 4, ten: 'Mizuno' },
          { id: 5, ten: 'Kumpoo' },
        ],
      }
    }
  },
}

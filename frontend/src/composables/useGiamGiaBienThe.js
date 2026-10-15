import { ref } from 'vue'
import { dotGiamGiaService } from '../services/dotGiamGiaService'
import { ngayIso } from '../constants/dotGiamGia'
import { todayIso } from '../utils/format'
import { tinhGiaSauGiam } from '../utils/giamGia'

/**
 * Giảm giá của từng biến thể, lấy từ các ĐỢT GIẢM GIÁ (trang Đợt giảm giá -> "Sản phẩm áp dụng"):
 *  - pctOf(id)     : % giảm đang áp dụng HÔM NAY (backend tính: chỉ các đợt đang bật và hôm nay nằm trong khoảng ngày;
 *                    biến thể nằm trong nhiều đợt chồng nhau thì gộp theo MAX / TRUNG_BINH trong application.properties).
 *  - sapGiamOf(id) : đợt SẮP DIỄN RA gần nhất của biến thể ({ phanTram, tuNgay }) hoặc null, để cột "Giảm giá" không chỉ hiện "—"
 *                    khi biến thể đã được thêm vào một đợt nhưng đợt đó chưa tới ngày bắt đầu.
 * Dùng để hiển thị giá gốc gạch ngang + giá sau giảm ở danh sách sản phẩm / biến thể.
 */
export function useGiamGiaBienThe() {
  const phanTram = ref({}) // { [idBienThe]: phần trăm giảm hôm nay }
  const sapGiam = ref({}) // { [idBienThe]: { phanTram, tuNgay: 'yyyy-mm-dd' } }

  // kemSapDienRa = true: tải thêm các đợt sắp diễn ra (trang Biến thể cần, trang Sản phẩm chỉ cần giá hôm nay)
  async function loadGiamGia({ kemSapDienRa = false } = {}) {
    try {
      phanTram.value = (await dotGiamGiaService.getApDungHomNay()) || {}
    } catch {
      // Không tải được giảm giá thì cứ hiện giá gốc, không làm hỏng trang
    }
    if (!kemSapDienRa) return
    try {
      const kq = await dotGiamGiaService.apDung()
      const homNay = todayIso()
      const map = {}
      for (const m of kq?.muc ?? []) {
        const tuNgay = ngayIso(m.ngayBatDau)
        if (!tuNgay || tuNgay <= homNay) continue // đã / đang diễn ra: đã nằm trong phanTram
        const cu = map[m.idBienThe]
        if (!cu || tuNgay < cu.tuNgay) map[m.idBienThe] = { phanTram: m.phanTram, tuNgay }
      }
      sapGiam.value = map
    } catch {
      // Không có thông tin đợt sắp diễn ra thì thôi, không ảnh hưởng giá đang áp dụng
    }
  }

  const pctOf = (idBienThe) => phanTram.value[idBienThe] || 0
  const sapGiamOf = (idBienThe) => sapGiam.value[idBienThe] || null
  const giaSauGiam = (b) => tinhGiaSauGiam(b.giaBan, pctOf(b.id))

  return { loadGiamGia, pctOf, sapGiamOf, giaSauGiam }
}

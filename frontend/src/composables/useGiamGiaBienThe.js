import { ref } from 'vue'
import { dotGiamGiaService } from '../services/dotGiamGiaService'
import { bayGioDayDu, mocBatDau } from '../utils/ngayGio'
import { tinhGiaSauGiam } from '../utils/giamGia'

/**
 * Giảm giá của từng biến thể, lấy từ các ĐỢT GIẢM GIÁ (trang Đợt giảm giá -> "Sản phẩm áp dụng"):
 *  - pctOf(id)     : % giảm đang áp dụng NGAY LÚC NÀY (backend tính tới giờ phút: chỉ các đợt đang bật và bây giờ nằm trong
 *                    [ngày giờ bắt đầu, ngày giờ kết thúc]; biến thể nằm trong nhiều đợt chồng nhau thì gộp theo
 *                    MAX / CONG_DON / TRUNG_BINH trong application.properties).
 *  - sapGiamOf(id) : đợt SẮP DIỄN RA gần nhất của biến thể ({ phanTram, tuLuc: 'yyyy-MM-ddTHH:mm:ss' }) hoặc null, để ô Giá bán
 *                    còn báo "Sắp giảm" khi biến thể đã được thêm vào một đợt nhưng đợt đó chưa tới giờ bắt đầu.
 * Dùng để hiển thị giá gốc gạch ngang + giá sau giảm ở danh sách sản phẩm / biến thể.
 */
export function useGiamGiaBienThe() {
  const phanTram = ref({}) // { [idBienThe]: phần trăm giảm hiện tại }
  const sapGiam = ref({}) // { [idBienThe]: { phanTram, tuLuc } }

  // kemSapDienRa = true: tải thêm các đợt sắp diễn ra (trang Biến thể cần, trang Sản phẩm chỉ cần giá hiện tại)
  async function loadGiamGia({ kemSapDienRa = false } = {}) {
    try {
      phanTram.value = (await dotGiamGiaService.getApDungHomNay()) || {}
    } catch {
      // Không tải được giảm giá thì cứ hiện giá gốc, không làm hỏng trang
    }
    if (!kemSapDienRa) return
    try {
      const kq = await dotGiamGiaService.apDung()
      const bayGio = bayGioDayDu()
      const map = {}
      for (const m of kq?.muc ?? []) {
        const tuLuc = mocBatDau(m.ngayBatDau)
        if (!tuLuc || tuLuc <= bayGio) continue // đã / đang diễn ra: đã nằm trong phanTram
        const cu = map[m.idBienThe]
        if (!cu || tuLuc < cu.tuLuc) map[m.idBienThe] = { phanTram: m.phanTram, tuLuc }
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

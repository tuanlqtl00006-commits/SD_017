import { ref } from 'vue'
import { dotGiamGiaService } from '../services/dotGiamGiaService'

/**
 * Giảm giá đang áp dụng HÔM NAY cho từng biến thể (backend tính: chỉ các đợt đang bật và hôm nay nằm trong khoảng ngày;
 * biến thể nằm trong nhiều đợt chồng nhau thì gộp theo MAX / TRUNG_BINH trong application.properties).
 * Dùng để hiển thị giá gốc gạch ngang + giá sau giảm ở danh sách sản phẩm / biến thể.
 */
export function useGiamGiaBienThe() {
  const phanTram = ref({}) // { [idBienThe]: phần trăm giảm }

  async function loadGiamGia() {
    try {
      phanTram.value = await dotGiamGiaService.getApDungHomNay()
    } catch {
      // Không tải được giảm giá thì cứ hiện giá gốc, không làm hỏng trang
    }
  }

  const pctOf = (idBienThe) => phanTram.value[idBienThe] || 0
  const giaSauGiam = (b) => Math.round(b.giaBan * (1 - pctOf(b.id) / 100))

  return { loadGiamGia, pctOf, giaSauGiam }
}

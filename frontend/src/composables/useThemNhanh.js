import { ref } from 'vue'
import { THUOC_TINH } from '../constants/thuocTinh'
import { thuocTinhService } from '../services/thuocTinhService'
import { NHOM_DANH_MUC } from '../utils/danhMuc'
import { normalizeText } from '../utils/text'

/**
 * "Thêm nhanh" thuộc tính ngay trong ô chọn Select2 (theo lời cô: khỏi sang trang Danh sách thuộc tính):
 * gõ tên chưa có -> bấm "Thêm nhanh" -> tạo qua API rồi trả về mục mới để trang gán luôn vào ô đang chọn.
 *
 *  - layDanhSach(slug): danh sách thuộc tính hiện có (kể cả đã ngưng) để báo trùng trước khi gọi API.
 *  - themVaoDanhSach(slug, muc): trang chèn mục mới vào danh sách của mình.
 *  - nhomChatLieu(): các nhóm danh mục của chất liệu mới (chất liệu bắt buộc có ít nhất một nhóm); mặc định mọi nhóm.
 */
export function useThemNhanh({ layDanhSach, themVaoDanhSach, toast, nhomChatLieu = null }) {
  const dangThem = ref(false)

  /** @returns {Promise<object|null>} mục mới ({ id, ma, ten, hoatDong }) hoặc null nếu không thêm được (đã báo lỗi). */
  async function themNhanh(slug, tenGoc) {
    const meta = THUOC_TINH[slug]
    if (!meta) return null
    const nhan = meta.label.toLowerCase()
    const ten = String(tenGoc ?? '').trim().replace(/\s+/g, ' ')
    if (!ten) {
      toast.error(`Nhập tên ${nhan} cần thêm.`)
      return null
    }
    if (ten.length > 100) {
      toast.error('Tên tối đa 100 ký tự.')
      return null
    }
    const daCo = (layDanhSach(slug) ?? []).find((x) => normalizeText(x.ten ?? '') === normalizeText(ten))
    if (daCo) {
      toast.error(
        daCo.hoatDong === false
          ? `${meta.label} “${daCo.ten}” đã có nhưng đang ngưng hoạt động. Vào Danh sách thuộc tính → ${meta.label} để kích hoạt lại.`
          : `${meta.label} “${daCo.ten}” đã có trong danh sách.`,
      )
      return null
    }
    dangThem.value = true
    try {
      const payload = { ten }
      if (slug === 'chat-lieu') payload.nhom = nhomChatLieu?.() ?? NHOM_DANH_MUC.map((n) => n.ma)
      const moi = await thuocTinhService.create(slug, payload)
      themVaoDanhSach(slug, moi)
      toast.success(`Đã thêm nhanh ${nhan} “${moi.ten ?? ten}”.`)
      return moi
    } catch (e) {
      toast.error(e.message || `Không thêm được ${nhan}. Vui lòng thử lại.`)
      return null
    } finally {
      dangThem.value = false
    }
  }

  return { themNhanh, dangThem }
}

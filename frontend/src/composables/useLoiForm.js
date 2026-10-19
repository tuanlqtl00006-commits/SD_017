import { computed, nextTick, reactive, ref } from 'vue'

/**
 * Hiện lỗi NGAY DƯỚI TỪNG Ô của một biểu mẫu (cô yêu cầu: khách hàng / nhân viên phải có lỗi dưới từng ô).
 *
 *  - tinhLoi: () => ({ tenO: 'câu báo lỗi', ... }) - luật kiểm tra của form, chạy lại mỗi khi dữ liệu đổi.
 *  - Lỗi của một ô chỉ hiện khi người dùng đã chạm vào ô đó (gõ hoặc rời ô) hoặc sau khi bấm lưu - không hiện cả loạt lỗi
 *    ngay khi vừa mở form trống.
 *  - Lỗi do máy chủ trả về (error.loiTheoTruong: trùng email, trùng số điện thoại...) được gắn vào đúng ô,
 *    và tự mất khi người dùng sửa ô đó.
 *  - Form SỬA dùng đúng bộ này như form THÊM nên hai bên báo lỗi giống hệt nhau.
 */
export function useLoiForm(tinhLoi, options = {}) {
  const daCham = reactive({}) // ô nào đã chạm vào
  const daBamLuu = ref(false)
  const loiMayChu = reactive({}) // tenO -> câu báo lỗi do backend trả về
  const chiKhiLuu = options.chiKhiLuu ?? false

  const loiTatCa = computed(() => {
    const kq = {}
    for (const [khoa, noiDung] of Object.entries(tinhLoi() ?? {})) if (noiDung) kq[khoa] = noiDung
    return kq
  })

  /** Lỗi đang hiện trên màn hình: lỗi cục bộ của ô đã chạm (hoặc tất cả sau khi bấm lưu), nếu không có thì lỗi từ máy chủ. */
  const loi = computed(() => {
    const kq = {}
    for (const [khoa, noiDung] of Object.entries(loiTatCa.value)) {
      if (chiKhiLuu ? daBamLuu.value : (daBamLuu.value || daCham[khoa])) kq[khoa] = noiDung
    }
    for (const [khoa, noiDung] of Object.entries(loiMayChu)) {
      if (noiDung && !kq[khoa]) kq[khoa] = noiDung
    }
    return kq
  })

  const coLoi = computed(() => Object.keys(loiTatCa.value).length > 0)

  /** Gọi khi RỜI ô (blur): ô bắt đầu được kiểm tra. */
  function cham(khoa) {
    daCham[khoa] = true
  }

  /** Gọi khi người dùng SỬA ô (gõ / chọn): kiểm tra ô đó và bỏ lỗi cũ do máy chủ báo (vì giá trị đã đổi). */
  function sua(khoa) {
    daCham[khoa] = true
    if (khoa in loiMayChu) delete loiMayChu[khoa]
  }

  /** Bấm lưu: từ giờ mọi ô đều hiện lỗi. Trả về true nếu form hợp lệ (được phép gửi). */
  function batDauGui() {
    daBamLuu.value = true
    return !coLoi.value
  }

  function xoaLoiMayChu() {
    for (const khoa of Object.keys(loiMayChu)) delete loiMayChu[khoa]
  }

  /**
   * Gắn lỗi theo ô từ lỗi axios của backend ({ loiTheoTruong: { tenO: 'câu' } }).
   * anhXa: đổi tên ô của backend sang tên ô của form (vd { soDienThoai: 'sdt' }). Trả về true nếu gắn được ít nhất một ô.
   */
  function datLoiMayChu(err, anhXa = {}) {
    xoaLoiMayChu()
    const theoTruong = err?.loiTheoTruong ?? err?.response?.data?.loiTheoTruong
    if (!theoTruong || typeof theoTruong !== 'object') return false
    let coGan = false
    for (const [khoa, noiDung] of Object.entries(theoTruong)) {
      if (!noiDung) continue
      loiMayChu[anhXa[khoa] ?? khoa] = String(noiDung)
      coGan = true
    }
    return coGan
  }

  /** Đưa con trỏ tới ô lỗi đầu tiên trong form (selector của form), cuộn ra giữa màn hình. */
  async function focusLoiDau(selectorForm) {
    await nextTick()
    const goc = typeof selectorForm === 'string' ? document.querySelector(selectorForm) : selectorForm
    const o = goc?.querySelector?.('.is-invalid, [aria-invalid="true"]')
    o?.scrollIntoView?.({ block: 'center' })
    o?.focus?.()
  }

  /** Về trạng thái ban đầu (vd nạp lại dữ liệu sửa). */
  function datLai() {
    for (const khoa of Object.keys(daCham)) delete daCham[khoa]
    daBamLuu.value = false
    xoaLoiMayChu()
  }

  return { loi, loiTatCa, coLoi, cham, sua, batDauGui, datLoiMayChu, xoaLoiMayChu, focusLoiDau, datLai, daBamLuu }
}

package com.footstyle.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Lập kế hoạch gửi mail khi sửa phiếu giảm giá cá nhân còn "Sắp diễn ra".
 * Lớp thuần Java (không phụ thuộc Spring) nên kiểm tra được độc lập.
 *
 * Luật (theo ghi chú):
 *   - Thông tin phiếu đổi, danh sách khách giữ nguyên  -> gửi lại mail cập nhật cho mọi khách.
 *   - Thông tin phiếu không đổi, danh sách khách đổi   -> khách mới: mail mới; khách bị bỏ: mail hủy; khách giữ nguyên: không gửi.
 *   - Đổi cả hai                                       -> khách mới: mail mới; khách bị bỏ: mail hủy; khách giữ nguyên: mail cập nhật.
 */
public final class PhieuMailKeHoach {

    private PhieuMailKeHoach() {
    }

    /** Các thông tin của phiếu mà khách quan tâm; đổi một trong số này là "thông tin phiếu đổi". */
    public record ThongTin(String ten, Integer loaiGiam, Long giaTri, Long giamToiDa, Long donToiThieu,
                           LocalDateTime batDau, LocalDateTime ketThuc) {
    }

    /** Id khách hàng cần gửi từng loại mail. */
    public record KeHoach(List<Integer> moi, List<Integer> huy, List<Integer> capNhat) {
        public int tong() {
            return moi.size() + huy.size() + capNhat.size();
        }
    }

    public static KeHoach lap(Collection<Integer> khachCu, Collection<Integer> khachMoi, boolean thongTinDoi) {
        Set<Integer> cu = new LinkedHashSet<>(khachCu);
        Set<Integer> moi = new LinkedHashSet<>(khachMoi);

        List<Integer> mailMoi = new ArrayList<>();
        List<Integer> mailHuy = new ArrayList<>();
        List<Integer> mailCapNhat = new ArrayList<>();
        for (Integer id : moi) {
            if (!cu.contains(id)) mailMoi.add(id);
            else if (thongTinDoi) mailCapNhat.add(id);
        }
        for (Integer id : cu) {
            if (!moi.contains(id)) mailHuy.add(id);
        }
        return new KeHoach(mailMoi, mailHuy, mailCapNhat);
    }
}

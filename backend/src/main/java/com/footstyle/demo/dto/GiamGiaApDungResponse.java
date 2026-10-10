package com.footstyle.demo.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Các mức giảm giá theo đợt đang chờ / đang áp dụng cho từng biến thể (chỉ đợt đang bật, chưa kết thúc).
 * Frontend dùng danh sách này để tính giá hiển thị và dựng "lịch giảm giá" theo từng khoảng ngày.
 *
 * @param chinhSach cách tính khi một biến thể nằm trong nhiều đợt cùng thời điểm:
 *                  "MAX" (lấy mức cao nhất) hoặc "TRUNG_BINH" (trung bình cộng các đợt đang chồng nhau).
 */
public record GiamGiaApDungResponse(String chinhSach, List<Muc> muc) {

    public record Muc(
            Long idBienThe,
            Long idDot,
            String maDot,
            String tenDot,
            Integer phanTram,
            LocalDate ngayBatDau,
            LocalDate ngayKetThuc) {
    }
}

package com.footstyle.demo.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Quy ước ngày GIỜ của phiếu giảm giá và đợt giảm giá (dùng chung để hai module không lệch nhau):
 * <ul>
 *   <li>tính theo PHÚT: bắt đầu tính từ đầu phút (17:00:00), kết thúc tính tới hết phút (17:00:59);</li>
 *   <li>cùng quy ước "23:59:59" cuối ngày của dữ liệu cũ nên phiếu / đợt cũ lưu theo ngày vẫn chạy đúng;</li>
 *   <li>tạo mới: giờ bắt đầu không được ở quá khứ (cho lệch {@value #DUNG_SAI_PHUT} phút để bấm "Bây giờ" rồi lưu không bị báo lỗi);</li>
 *   <li>sửa: đã bắt đầu thì không đổi giờ bắt đầu; giờ kết thúc không được dời sang một thời điểm khác trong quá khứ.</li>
 * </ul>
 */
public final class ThoiGianPhut {

    /** Số phút cho phép giờ bắt đầu của phiếu / đợt MỚI lệch về quá khứ. */
    public static final int DUNG_SAI_PHUT = 5;

    private ThoiGianPhut() {
    }

    /** 17:00:30 -> 17:00:00 (giờ bắt đầu tính từ đầu phút). */
    public static LocalDateTime dauPhut(LocalDateTime t) {
        return t.truncatedTo(ChronoUnit.MINUTES);
    }

    /** 17:00:00 -> 17:00:59 (giờ kết thúc tính tới hết phút). */
    public static LocalDateTime cuoiPhut(LocalDateTime t) {
        return t.truncatedTo(ChronoUnit.MINUTES).withSecond(59);
    }

    /** Hai thời điểm có cùng phút không (null chỉ bằng null). */
    public static boolean cungPhut(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.truncatedTo(ChronoUnit.MINUTES).equals(b.truncatedTo(ChronoUnit.MINUTES));
    }

    /**
     * Kiểm tra khoảng thời gian (đã chuẩn hóa dauPhut / cuoiPhut). Trả về câu báo lỗi, hoặc null nếu hợp lệ.
     *
     * @param ten        "Phiếu" hoặc "Đợt" (đầu câu báo lỗi về việc đã bắt đầu)
     * @param batDauCu   giờ bắt đầu đang lưu (null khi tạo mới)
     * @param ketThucCu  giờ kết thúc đang lưu (null khi tạo mới)
     */
    public static String kiemTra(String ten, LocalDateTime batDau, LocalDateTime ketThuc, LocalDateTime batDauCu,
                                 LocalDateTime ketThucCu, boolean taoMoi, LocalDateTime bayGio) {
        if (!ketThuc.truncatedTo(ChronoUnit.MINUTES).isAfter(batDau)) {
            return "Ngày giờ kết thúc phải sau ngày giờ bắt đầu.";
        }
        LocalDateTime sanDuoi = bayGio.minusMinutes(DUNG_SAI_PHUT);
        if (taoMoi) {
            if (batDau.isBefore(sanDuoi)) {
                return "Ngày giờ bắt đầu không được ở quá khứ.";
            }
            if (ketThuc.isBefore(bayGio)) {
                return "Ngày giờ kết thúc không được ở quá khứ.";
            }
            return null;
        }
        boolean daBatDau = batDauCu != null && !batDauCu.isAfter(bayGio);
        boolean doiBatDau = !cungPhut(batDau, batDauCu);
        if (doiBatDau && daBatDau) {
            return ten + " đã bắt đầu nên không đổi được ngày giờ bắt đầu.";
        }
        if (doiBatDau && batDau.isBefore(sanDuoi)) {
            return "Ngày giờ bắt đầu mới không được ở quá khứ.";
        }
        if (!cungPhut(ketThuc, ketThucCu) && ketThuc.isBefore(bayGio)) {
            return "Ngày giờ kết thúc mới không được ở quá khứ.";
        }
        return null;
    }
}

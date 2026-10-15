package com.footstyle.demo.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

/**
 * Các quy tắc kiểm tra dữ liệu DÙNG CHUNG cho Khách hàng và Nhân viên (thêm, sửa, nhập Excel), để hai nơi luôn giống nhau.
 * Giao diện có bản sao các quy tắc này ở frontend/src/utils/kiemTra.js (hai bản phải khớp nhau, có test kiểm tra).
 *
 * CĂN CỨ ĐỘ TUỔI (cũng ghi trên form và trong CAN_CU_PHAP_LY_VA_QUY_TAC.md):
 *  - Khách hàng từ đủ 15 tuổi: Bộ luật Dân sự 2015, Điều 21 khoản 4 - người từ đủ 15 đến chưa đủ 18 tuổi tự mình xác lập, thực hiện giao dịch
 *    dân sự (như mua bán hàng thông thường) mà không cần người đại diện đồng ý (trừ bất động sản, động sản phải đăng ký...).
 *    Người từ đủ 6 đến chưa đủ 15 tuổi phải được người đại diện theo pháp luật đồng ý (khoản 3, trừ giao dịch phục vụ nhu cầu sinh hoạt hằng
 *    ngày phù hợp lứa tuổi), dưới 6 tuổi do người đại diện xác lập (khoản 2). Cửa hàng không kiểm chứng được sự đồng ý đó khi tạo hồ sơ khách
 *    nên lấy mốc 15 tuổi (luật KHÔNG cấm trẻ nhỏ mua hàng, đây là quy định nội bộ dựa trên Điều 21).
 *  - Nhân viên từ đủ 18 tuổi: Bộ luật Lao động 2019, Điều 143 khoản 1 - "lao động chưa thành niên là người lao động chưa đủ 18 tuổi"; loại lao động
 *    này bị hạn chế công việc / nơi làm việc (Điều 143 khoản 2-3, Điều 147), thời giờ làm việc (Điều 146) và có nguyên tắc sử dụng riêng (Điều 144).
 *    Luật không cấm hẳn người 15-17 tuổi đi làm; cửa hàng chọn mốc 18 để nhân viên không thuộc diện lao động chưa thành niên (quy định nội bộ).
 *  - Tuổi tối đa 100: KHÔNG phải quy định pháp luật, chỉ để chặn gõ nhầm năm sinh (ví dụ 1900 thay vì 2000).
 */
public final class QuyTacDuLieu {

    public static final int TUOI_KHACH_TOI_THIEU = 15;
    public static final int TUOI_NHAN_VIEN_TOI_THIEU = 18;
    public static final int TUOI_TOI_DA = 100;

    /** Số điện thoại di động Việt Nam: 10 số, đầu số 03x / 05x / 07x / 08x / 09x. */
    public static final Pattern SDT = Pattern.compile("^0[35789]\\d{8}$");
    public static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    /** Họ tên: bắt đầu bằng chữ cái, chỉ gồm chữ cái (có dấu), khoảng trắng, dấu chấm, nháy đơn, gạch ngang - không có chữ số. */
    public static final Pattern HO_TEN = Pattern.compile("^\\p{L}[\\p{L} .'’-]*$");
    public static final Pattern CCCD = Pattern.compile("^\\d{12}$");

    public static final String LOI_SDT = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.";
    public static final String LOI_EMAIL = "Email chưa đúng định dạng, ví dụ: ten@gmail.com.";
    public static final String LOI_HO_TEN_KY_TU = "Họ tên chỉ gồm chữ cái và khoảng trắng, không có chữ số hay ký tự đặc biệt.";

    private QuyTacDuLieu() {
    }

    /** null hoặc chỉ có khoảng trắng -> "" ; ngược lại bỏ khoảng trắng đầu / cuối. */
    public static String cat(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Kiểm tra đường dẫn ảnh đại diện. Hợp lệ: ảnh tải lên từ máy (/api/uploads/ten-file) hoặc link http(s), tối đa 500 ký tự
     * (khớp cột khach_hang.anh_dai_dien). Trả về câu báo lỗi hoặc null nếu hợp lệ (hoặc để trống).
     */
    public static String kiemTraAnhDaiDien(String url) {
        String anh = cat(url);
        if (anh.isEmpty()) return null;
        if (anh.length() > 500) return "Đường dẫn ảnh đại diện tối đa 500 ký tự.";
        boolean laLinkNgoai = anh.matches("(?i)^https?://\\S+$");
        boolean laAnhNoiBo = anh.matches("^/api/uploads/[A-Za-z0-9._-]+$") && !anh.contains("..");
        return laLinkNgoai || laAnhNoiBo ? null : "Ảnh đại diện phải được tải lên từ máy hoặc là đường dẫn http:// hoặc https://.";
    }

    /** Họ tên: bỏ khoảng trắng thừa ("  Nguyễn   An " -> "Nguyễn An"). */
    public static String chuanHoaHoTen(String s) {
        return cat(s).replaceAll("\\s+", " ");
    }

    /** Số tuổi tròn tại ngày `ngay`. */
    public static int tuoi(LocalDate ngaySinh, LocalDate ngay) {
        return Period.between(ngaySinh, ngay).getYears();
    }

    /**
     * Kiểm tra ngày sinh. Trả về câu báo lỗi hoặc null nếu hợp lệ.
     * @param tuoiToiThieu 15 (khách hàng) hoặc 18 (nhân viên)
     * @param doiTuong     "Khách hàng" / "Nhân viên" (đưa vào câu báo lỗi)
     */
    public static String kiemTraNgaySinh(LocalDate ngaySinh, int tuoiToiThieu, String doiTuong, LocalDate homNay) {
        if (ngaySinh == null) {
            return null;
        }
        if (ngaySinh.isAfter(homNay)) {
            return "Ngày sinh không được ở tương lai.";
        }
        if (tuoi(ngaySinh, homNay) < tuoiToiThieu) {
            return doiTuong + " phải từ đủ " + tuoiToiThieu + " tuổi.";
        }
        if (tuoi(ngaySinh, homNay) > TUOI_TOI_DA) {
            return "Ngày sinh không hợp lệ (quá " + TUOI_TOI_DA + " tuổi).";
        }
        return null;
    }
}

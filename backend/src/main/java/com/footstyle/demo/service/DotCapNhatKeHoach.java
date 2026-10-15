package com.footstyle.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Lập kế hoạch đồng bộ biến thể khi SỬA đợt giảm giá (tương tự PhieuMailKeHoach của Phiếu giảm giá, nhưng KHÔNG gửi mail).
 * Lớp thuần Java (không phụ thuộc Spring) nên kiểm tra được độc lập.
 *
 * Luật switch-case khi sửa đợt:
 *   - KHONG_DOI        : thông tin đợt và danh sách biến thể giữ nguyên -> giữ nguyên toàn bộ.
 *   - TH1_DOI_THONG_TIN: thông tin đợt đổi (%, ngày), danh sách biến thể giữ nguyên -> cập nhật lại mọi biến thể trong đợt.
 *   - TH2_DOI_BIEN_THE : thông tin đợt giữ nguyên, danh sách biến thể đổi -> biến thể mới: thêm; biến thể bị bỏ: gỡ; biến thể giữ lại: giữ nguyên.
 *   - TH3_DOI_CA_HAI   : đổi cả thông tin đợt và danh sách biến thể -> biến thể mới: thêm; biến thể bị bỏ: gỡ; biến thể giữ lại: cập nhật.
 */
public final class DotCapNhatKeHoach {

    private DotCapNhatKeHoach() {
    }

    /** Các trường hợp khi cập nhật đợt giảm giá. */
    public enum TruongHop {
        KHONG_DOI,
        TH1_DOI_THONG_TIN,
        TH2_DOI_BIEN_THE,
        TH3_DOI_CA_HAI
    }

    /** Trạng thái hiển thị của đợt giảm giá (tương tự Phiếu giảm giá). */
    public enum TrangThaiHienThi {
        SAP_DIEN_RA,
        DANG_DIEN_RA,
        DA_KET_THUC,
        NGUNG_HOAT_DONG
    }

    /** Thông tin chính của đợt ảnh hưởng tới giảm giá của biến thể. */
    public record ThongTinDot(String tenDot, Integer phanTram, LocalDateTime batDau, LocalDateTime ketThuc) {
    }

    /** Kết quả phân loại biến thể cần thêm mới, gỡ bỏ, cập nhật hoặc giữ nguyên. */
    public record KeHoach(
            TruongHop truongHop,
            List<Long> moi,
            List<Long> goBo,
            List<Long> capNhat,
            List<Long> giuNguyen
    ) {
        public String moTaTomTat() {
            return switch (truongHop) {
                case KHONG_DOI -> "Không có thay đổi về biến thể.";
                case TH1_DOI_THONG_TIN -> "TH1 (đổi thông tin đợt): cập nhật " + capNhat.size() + " biến thể.";
                case TH2_DOI_BIEN_THE -> String.format(
                        "TH2 (đổi danh sách biến thể): thêm %d, gỡ %d, giữ nguyên %d biến thể.",
                        moi.size(), goBo.size(), giuNguyen.size());
                case TH3_DOI_CA_HAI -> String.format(
                        "TH3 (đổi thông tin và danh sách biến thể): thêm %d, gỡ %d, cập nhật %d biến thể.",
                        moi.size(), goBo.size(), capNhat.size());
            };
        }
    }

    /** Xác định trường hợp sửa đợt dựa vào việc thông tin đợt và danh sách biến thể có thay đổi hay không. */
    public static TruongHop xacDinhTruongHop(boolean thongTinDoi, boolean danhSachBienTheDoi) {
        if (thongTinDoi && danhSachBienTheDoi) return TruongHop.TH3_DOI_CA_HAI;
        if (thongTinDoi) return TruongHop.TH1_DOI_THONG_TIN;
        if (danhSachBienTheDoi) return TruongHop.TH2_DOI_BIEN_THE;
        return TruongHop.KHONG_DOI;
    }

    /** Lập kế hoạch xử lý biến thể theo switch-case (TH1 / TH2 / TH3). */
    public static KeHoach lap(Collection<Long> bienTheCu, Collection<Long> bienTheMoi, boolean thongTinDoi) {
        Set<Long> cu = new LinkedHashSet<>(bienTheCu == null ? List.of() : bienTheCu);
        Set<Long> moi = new LinkedHashSet<>(bienTheMoi == null ? List.of() : bienTheMoi);
        boolean danhSachDoi = !Objects.equals(cu, moi);
        TruongHop truongHop = xacDinhTruongHop(thongTinDoi, danhSachDoi);

        List<Long> dsMoi = new ArrayList<>();
        List<Long> dsGoBo = new ArrayList<>();
        List<Long> dsCapNhat = new ArrayList<>();
        List<Long> dsGiuNguyen = new ArrayList<>();

        switch (truongHop) {
            case KHONG_DOI -> dsGiuNguyen.addAll(moi);
            case TH1_DOI_THONG_TIN -> dsCapNhat.addAll(moi);
            case TH2_DOI_BIEN_THE -> {
                for (Long id : moi) {
                    if (!cu.contains(id)) dsMoi.add(id);
                    else dsGiuNguyen.add(id);
                }
                for (Long id : cu) {
                    if (!moi.contains(id)) dsGoBo.add(id);
                }
            }
            case TH3_DOI_CA_HAI -> {
                for (Long id : moi) {
                    if (!cu.contains(id)) dsMoi.add(id);
                    else dsCapNhat.add(id);
                }
                for (Long id : cu) {
                    if (!moi.contains(id)) dsGoBo.add(id);
                }
            }
        }
        return new KeHoach(truongHop, dsMoi, dsGoBo, dsCapNhat, dsGiuNguyen);
    }

    /**
     * Tính trạng thái hiển thị của đợt giảm giá theo ngày hiện tại và công tắc bật/tắt:
     *  - Công tắc tắt (trangThai != 1) -> NGUNG_HOAT_DONG
     *  - Quá ngày kết thúc             -> DA_KET_THUC
     *  - Chưa tới ngày bắt đầu         -> SAP_DIEN_RA
     *  - Trong khoảng [bắt đầu, kết thúc] -> DANG_DIEN_RA
     */
    public static TrangThaiHienThi tinhTrangThai(Integer trangThai, LocalDate batDau, LocalDate ketThuc, LocalDate homNay) {
        if (trangThai == null || trangThai != 1) return TrangThaiHienThi.NGUNG_HOAT_DONG;
        if (ketThuc != null && homNay.isAfter(ketThuc)) return TrangThaiHienThi.DA_KET_THUC;
        if (batDau != null && homNay.isBefore(batDau)) return TrangThaiHienThi.SAP_DIEN_RA;
        return TrangThaiHienThi.DANG_DIEN_RA;
    }

    /** Nhãn tiếng Việt tương ứng với từng trạng thái (switch-case). */
    public static String nhanTrangThai(TrangThaiHienThi tt) {
        return switch (tt) {
            case SAP_DIEN_RA -> "Sắp diễn ra";
            case DANG_DIEN_RA -> "Đang diễn ra";
            case DA_KET_THUC -> "Đã kết thúc";
            case NGUNG_HOAT_DONG -> "Ngừng hoạt động";
        };
    }
}

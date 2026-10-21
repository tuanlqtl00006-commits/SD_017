package com.footstyle.demo.service;

import com.footstyle.demo.entity.PhieuGiamGia;
import java.time.LocalDateTime;

/**
 * Luật dùng phiếu giảm giá khi thanh toán (bán tại quầy; trang bán online dùng lại được). Một đơn chỉ dùng MỘT phiếu.
 * Phiếu tính trên tổng tiền hàng đã trừ đợt giảm giá:
 *  - giảm %: làm tròn XUỐNG tới đồng, không vượt "giảm tối đa";
 *  - giảm tiền: không vượt tổng đơn.
 * Giống frontend/src/utils/tinhPhieu.js (tinhGiamPhieu).
 */
public final class PhieuApDung {

    private PhieuApDung() {
    }

    /**
     * Lý do KHÔNG dùng được phiếu (null = dùng được).
     * @param laPhieuCuaKhach phiếu cá nhân: khách đang chọn có được tặng phiếu này và chưa dùng không
     * @param soLanKhachDaDung phiếu công khai: khách đã dùng phiếu này bao nhiêu lần (khách lẻ: 0)
     * @param coKhach đơn có gắn khách hàng không (phiếu cá nhân bắt buộc có khách)
     */
    public static String lyDoKhongDung(PhieuGiamGia p, long tongTien, LocalDateTime luc, boolean coKhach,
                                       boolean laPhieuCuaKhach, long soLanKhachDaDung) {
        if (p == null) return "Không tìm thấy phiếu giảm giá.";
        if (!Integer.valueOf(PhieuGiamGia.HOAT_DONG).equals(p.getTrangThai())) return "Phiếu đang ngừng hoạt động.";
        if (p.getNgayBatDau() != null && luc.isBefore(p.getNgayBatDau())) return "Phiếu chưa đến ngày áp dụng.";
        if (p.getNgayKetThuc() != null && luc.isAfter(p.getNgayKetThuc())) return "Phiếu đã hết hạn.";
        int daDung = p.getSoLuongDaDung() == null ? 0 : p.getSoLuongDaDung();
        if (p.getSoLuong() != null && daDung >= p.getSoLuong()) return "Phiếu đã hết lượt sử dụng.";
        if (Integer.valueOf(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE).equals(p.getDoiTuongApDung())) {
            if (!coKhach) return "Phiếu cá nhân: hãy chọn khách hàng được tặng phiếu.";
            if (!laPhieuCuaKhach) return "Phiếu không dành cho khách hàng này hoặc khách đã dùng phiếu.";
        } else if (coKhach && p.getGioiHanMoiKhach() != null && p.getGioiHanMoiKhach() > 0
                && soLanKhachDaDung >= p.getGioiHanMoiKhach()) {
            return "Khách đã dùng phiếu này " + soLanKhachDaDung + " lần (tối đa " + p.getGioiHanMoiKhach() + " lần mỗi khách).";
        }
        long toiThieu = p.getGiaTriDonHangToiThieu() == null ? 0 : p.getGiaTriDonHangToiThieu();
        if (tongTien < toiThieu) {
            return "Đơn tối thiểu " + GiaoCaService.dinhDangTien(java.math.BigDecimal.valueOf(toiThieu))
                    + " (còn thiếu " + GiaoCaService.dinhDangTien(java.math.BigDecimal.valueOf(toiThieu - tongTien)) + ").";
        }
        return null;
    }

    /** Số tiền phiếu giảm cho đơn có tổng tiền hàng `tongTien` (đã đủ điều kiện). */
    public static long soTienGiam(PhieuGiamGia p, long tongTien) {
        long t = Math.max(0, tongTien);
        long giaTri = p.getGiaTriGiam() == null ? 0 : p.getGiaTriGiam();
        if (giaTri <= 0) return 0;
        if (Integer.valueOf(PhieuGiamGia.LOAI_PHAN_TRAM).equals(p.getLoaiGiam())) {
            long giam = t * Math.min(100, giaTri) / 100; // chia nguyên = làm tròn xuống
            if (p.getGiamToiDa() != null && p.getGiamToiDa() > 0) giam = Math.min(giam, p.getGiamToiDa());
            return giam;
        }
        return Math.min(giaTri, t);
    }

    /** Giá sau đợt giảm giá của một biến thể (làm tròn tới đồng, giống tinhGiaSauGiam ở frontend). */
    public static long giaSauDot(long giaBan, double phanTram) {
        if (phanTram <= 0) return giaBan;
        if (phanTram >= 100) return 0;
        return Math.round((giaBan * (100 - phanTram)) / 100);
    }
}

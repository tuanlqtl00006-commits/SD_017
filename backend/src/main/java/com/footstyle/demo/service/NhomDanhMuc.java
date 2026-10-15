package com.footstyle.demo.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Nhóm danh mục dùng để gắn chất liệu với danh mục (vd Quần áo thể thao không dùng chất liệu khung vợt như Nhôm / Titanium).
 * Nhóm suy ra từ TÊN danh mục; danh mục lạ không nhận ra nhóm thì không bị lọc chất liệu.
 * Phải khớp nhomDanhMuc() trong frontend/src/utils/danhMuc.js.
 */
public final class NhomDanhMuc {

    public static final String VOT = "VOT";
    public static final String QUAN_AO = "QUAN_AO";
    public static final String TUI_BALO = "TUI_BALO";
    public static final String PHU_KIEN = "PHU_KIEN";
    public static final String QUA_CAU = "QUA_CAU";

    /** Mọi nhóm hợp lệ. */
    public static final List<String> TAT_CA = List.of(VOT, QUAN_AO, TUI_BALO, PHU_KIEN, QUA_CAU);

    private NhomDanhMuc() {
    }

    /** Nhóm của danh mục theo tên; null nếu không nhận ra. */
    public static String tuTen(String tenDanhMuc) {
        String t = boDau(tenDanhMuc);
        if (t.isEmpty()) {
            return null;
        }
        List<String> tu = List.of(t.split("[^a-z0-9]+"));
        if (tu.contains("vot")) {
            return VOT;
        }
        if (t.contains("qua cau")) {
            return QUA_CAU;
        }
        if (tu.contains("quan") || tu.contains("ao")) {
            return QUAN_AO;
        }
        if (tu.contains("tui") || tu.contains("balo")) {
            return TUI_BALO;
        }
        if (t.contains("phu kien")) {
            return PHU_KIEN;
        }
        return null;
    }

    /** Danh mục vợt (tên có chữ "vợt") có đủ Độ cứng, Điểm cân bằng, Trọng lượng, Chu vi. Khớp laDanhMucVot() ở frontend. */
    public static boolean laVot(String tenDanhMuc) {
        return VOT.equals(tuTen(tenDanhMuc));
    }

    /** Mục "Không áp dụng" (DC00, CB00, TL00, CV00) dùng cho danh mục không phải vợt. */
    public static boolean laKhongApDung(String ten) {
        return "khong ap dung".equals(boDau(ten).replaceAll("\\s+", " "));
    }

    /**
     * Thuộc tính chỉ có ở vợt (độ cứng, điểm cân bằng, trọng lượng, chu vi) phải khớp danh mục:
     * vợt thì phải chọn giá trị thật, danh mục khác thì phải là "Không áp dụng". Trả về câu lỗi, hợp lệ thì null.
     */
    public static String loiThuocTinhVot(String tenDanhMuc, String tenThuocTinh, String nhan) {
        boolean khongApDung = laKhongApDung(tenThuocTinh);
        if (laVot(tenDanhMuc) && khongApDung) {
            return "Danh mục \"" + tenDanhMuc + "\" phải chọn " + nhan + " cụ thể (không được chọn \"Không áp dụng\").";
        }
        if (!laVot(tenDanhMuc) && !khongApDung) {
            return "Danh mục \"" + tenDanhMuc + "\" không có " + nhan + ", hãy chọn \"Không áp dụng\".";
        }
        return null;
    }

    /** Chất liệu (danh sách nhóm đã gắn) có dùng được cho danh mục này không. Danh mục lạ hoặc chưa gắn nhóm: dùng được. */
    public static boolean hop(List<String> nhomChatLieu, String tenDanhMuc) {
        String nhom = tuTen(tenDanhMuc);
        return nhom == null || nhomChatLieu == null || nhomChatLieu.isEmpty() || nhomChatLieu.contains(nhom);
    }

    /** "VOT,QUAN_AO" -> [VOT, QUAN_AO] (bỏ phần rỗng, bỏ trùng). */
    public static List<String> tach(String chuoi) {
        if (chuoi == null || chuoi.isBlank()) {
            return List.of();
        }
        return List.of(chuoi.split(",")).stream().map(String::trim).filter(x -> !x.isEmpty()).distinct().toList();
    }

    private static String boDau(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}

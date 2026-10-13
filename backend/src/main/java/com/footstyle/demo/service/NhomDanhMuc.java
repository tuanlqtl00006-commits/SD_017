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

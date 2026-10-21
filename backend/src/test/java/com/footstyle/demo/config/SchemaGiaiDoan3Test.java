package com.footstyle.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.CaLamViec;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoiThoai;
import com.footstyle.demo.entity.LichLamViec;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.PhuongThucThanhToan;
import com.footstyle.demo.entity.ThanhVienHoiThoai;
import com.footstyle.demo.entity.TinNhan;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * CSDL giai đoạn 3 được tạo ở 4 nơi: SD_17.sql (CSDL mới), PHẦN 9 của SD_17_du_lieu_va_nang_cap.sql, SD_17_giai_doan_3.sql
 * (CSDL demo đang có dữ liệu) và SchemaGiaiDoan3 (backend tự chạy khi khởi động). Test này giữ cho 4 nơi không lệch nhau
 * và mọi cột khai báo trong entity đều có trong bảng (ddl-auto=none nên lệch là lỗi "Invalid column name" lúc chạy).
 */
class SchemaGiaiDoan3Test {

    private static String gon(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    static Path timFile(String ten) {
        String goc = System.getenv("FOOTSTYLE_GOC");
        List<Path> ungVien = goc != null && !goc.isBlank()
                ? List.of(Path.of(goc, "database", ten))
                : List.of(Path.of("..", "database", ten), Path.of("database", ten));
        for (Path p : ungVien) {
            if (Files.isRegularFile(p)) return p;
        }
        throw new AssertionError("Không tìm thấy " + ten + " (đã thử " + ungVien + ").");
    }

    private static String doc(String ten) throws IOException {
        return gon(Files.readString(timFile(ten), StandardCharsets.UTF_8).replace("﻿", ""));
    }

    /** Đọc file SQL, bỏ chú thích "-- ..." cuối dòng (để tách cột trong CREATE TABLE không bị dính chú thích). */
    private static String docBoChuThich(String ten) throws IOException {
        return gon(Files.readString(timFile(ten), StandardCharsets.UTF_8).replace("﻿", "").replaceAll("--[^\\n]*", ""));
    }

    @Test
    void moiLenhCuaBackend_coTrongPhan9VaFileGiaiDoan3() throws IOException {
        String phan9 = doc("SD_17_du_lieu_va_nang_cap.sql");
        phan9 = phan9.substring(phan9.indexOf("-- PHẦN 9:"));
        String rieng = doc("SD_17_giai_doan_3.sql");
        for (String lenh : SchemaGiaiDoan3.LENH) {
            assertTrue(phan9.contains(gon(lenh) + ";"), "PHẦN 9 thiếu / khác lệnh:\n" + lenh);
            assertTrue(rieng.contains(gon(lenh) + ";"), "SD_17_giai_doan_3.sql thiếu / khác lệnh:\n" + lenh);
        }
    }

    /** Thân lệnh CREATE TABLE của bảng `bang` (từ "CREATE TABLE bang (" tới dấu ")" đóng). */
    static String thanBang(String sql, String bang) {
        int i = sql.indexOf("CREATE TABLE " + bang + " (");
        if (i < 0) return null;
        int moNgoac = sql.indexOf('(', i);
        int sau = 0;
        for (int k = moNgoac; k < sql.length(); k++) {
            char c = sql.charAt(k);
            if (c == '(') sau++;
            if (c == ')' && --sau == 0) return sql.substring(i, k + 1);
        }
        return null;
    }

    private static final List<String> BANG_MOI = List.of(
            "ca_lam_viec", "lich_lam_viec", "giao_ca", "hoi_thoai", "thanh_vien_hoi_thoai", "tin_nhan");

    @Test
    void bangMoiTrongSD17_giongHetBangBackendTao() throws IOException {
        String sd17 = docBoChuThich("SD_17.sql");
        String lenh = gon(String.join(" ; ", SchemaGiaiDoan3.LENH));
        for (String bang : BANG_MOI) {
            String a = thanBang(sd17, bang);
            String b = thanBang(lenh, bang);
            assertTrue(a != null, "SD_17.sql chưa có bảng " + bang);
            assertTrue(b != null, "SchemaGiaiDoan3 chưa tạo bảng " + bang);
            assertEquals(a.replace("( ", "(").replace(" )", ")"), b.replace("( ", "(").replace(" )", ")"), "Bảng " + bang + " lệch giữa SD_17.sql và backend");
        }
        assertTrue(thanBang(sd17, "hoa_don").contains("id_giao_ca INT NULL"), "SD_17.sql: hoa_don thiếu cột id_giao_ca");
        assertTrue(thanBang(sd17, "lich_su_thanh_toan").contains("id_phuong_thuc_thanh_toan INT NULL"),
                "SD_17.sql: lich_su_thanh_toan thiếu cột id_phuong_thuc_thanh_toan");
        // bảng giao_ca phải tạo trước hoa_don (hoa_don có khóa ngoại trỏ tới giao_ca)
        assertTrue(sd17.indexOf("CREATE TABLE giao_ca (") < sd17.indexOf("CREATE TABLE hoa_don ("));
    }

    @Test
    void backendCoThemCotMoiVaDuLieuMacDinh() {
        String lenh = gon(String.join(" ; ", SchemaGiaiDoan3.LENH));
        assertTrue(lenh.contains("IF COL_LENGTH('hoa_don', 'id_giao_ca') IS NULL ALTER TABLE hoa_don ADD id_giao_ca INT NULL"));
        assertTrue(lenh.contains("IF COL_LENGTH('lich_su_thanh_toan', 'id_phuong_thuc_thanh_toan') IS NULL"));
        assertTrue(lenh.contains("'PT05'"), "phương thức tiền mặt + chuyển khoản");
        assertTrue(lenh.contains("'CA01'") && lenh.contains("'CA02'") && lenh.contains("'CA03'"), "3 ca mặc định");
        assertTrue(lenh.contains("CREATE UNIQUE INDEX ux_giao_ca_dang_mo ON giao_ca (id_nhan_vien) WHERE trang_thai = 1"),
                "mỗi nhân viên chỉ 1 ca đang mở");
        assertFalse(lenh.contains("INSERT INTO lich_lam_viec"), "lịch làm mẫu chỉ có trong file SQL, backend không tự thêm");
        // Chỉ thêm, không xóa
        assertFalse(lenh.toUpperCase().contains("DROP TABLE"));
        assertFalse(lenh.toUpperCase().contains("DELETE FROM"));
    }

    private static Set<String> cotTrongBang(String than) {
        Set<String> cot = new LinkedHashSet<>();
        String trong = than.substring(than.indexOf('(') + 1, than.length() - 1);
        // tách theo dấu phẩy ở mức ngoặc 0
        List<String> phan = new ArrayList<>();
        int sau = 0;
        StringBuilder cur = new StringBuilder();
        for (char c : trong.toCharArray()) {
            if (c == '(') sau++;
            if (c == ')') sau--;
            if (c == ',' && sau == 0) {
                phan.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        phan.add(cur.toString());
        Pattern p = Pattern.compile("^\\s*([a-z_]+)\\s+[A-Z]");
        for (String x : phan) {
            Matcher m = p.matcher(x);
            if (m.find() && !List.of("CONSTRAINT", "FOREIGN", "PRIMARY").contains(m.group(1).toUpperCase())) cot.add(m.group(1));
        }
        return cot;
    }

    @Test
    void moiCotCuaEntityDeuCoTrongBang() throws IOException {
        String sd17 = docBoChuThich("SD_17.sql");
        for (Class<?> c : List.of(CaLamViec.class, LichLamViec.class, GiaoCa.class, HoiThoai.class, ThanhVienHoiThoai.class,
                TinNhan.class, HoaDon.class, LichSuThanhToan.class, PhuongThucThanhToan.class)) {
            String bang = c.getAnnotation(Table.class).name();
            Set<String> cot = cotTrongBang(thanBang(sd17, bang));
            for (Field f : c.getDeclaredFields()) {
                String ten = null;
                if (f.isAnnotationPresent(Column.class)) ten = f.getAnnotation(Column.class).name();
                if (f.isAnnotationPresent(JoinColumn.class)) ten = f.getAnnotation(JoinColumn.class).name();
                if (ten == null || ten.isEmpty()) continue;
                assertTrue(cot.contains(ten), c.getSimpleName() + "." + f.getName() + " -> cột " + bang + "." + ten + " không có trong SD_17.sql " + cot);
            }
        }
    }
}

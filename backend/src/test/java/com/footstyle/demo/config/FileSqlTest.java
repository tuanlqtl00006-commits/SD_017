package com.footstyle.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.ChatLieu;
import com.footstyle.demo.entity.ChuVi;
import com.footstyle.demo.entity.DanhMuc;
import com.footstyle.demo.entity.DiaChiKhachHang;
import com.footstyle.demo.entity.DiaChiNhanVien;
import com.footstyle.demo.entity.DiemCanBang;
import com.footstyle.demo.entity.DoCung;
import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.MauSac;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.ThuongHieu;
import com.footstyle.demo.entity.TrongLuong;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.entity.XuatXu;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * CSDL gồm đúng 2 file SQL (theo yêu cầu của nhóm): FILE 1 chỉ có CẤU TRÚC (database, bảng, cột, khóa, chỉ mục),
 * FILE 2 chỉ có DỮ LIỆU (dữ liệu mẫu + sửa dữ liệu cũ). Test này giữ đúng sự tách biệt đó, giữ cho đường "tạo mới" (CREATE TABLE)
 * và đường "nâng cấp CSDL cũ" (ALTER TABLE ADD) không lệch nhau, và mọi cột của entity đều có trong bảng
 * (ddl-auto=none nên thiếu cột là lỗi "Invalid column name" lúc chạy).
 */
class FileSqlTest {

    static final String CAU_TRUC = "SD_17_1_cau_truc.sql";
    static final String DU_LIEU = "SD_17_2_du_lieu.sql";

    /** Mọi entity của backend. Thêm entity mới thì thêm vào đây. */
    static final List<Class<?>> ENTITY = List.of(ChatLieu.class, ChuVi.class, DanhMuc.class, DiaChiKhachHang.class,
            DiaChiNhanVien.class, DiemCanBang.class, DoCung.class, DotGiamGia.class, DotGiamGiaChiTiet.class,
            HinhAnhSanPham.class, HoaDon.class, HoaDonChiTiet.class, KhachHang.class,
            LichSuHoaDon.class, LichSuThanhToan.class, MauSac.class, NhanVien.class, PhieuGiamGia.class, PhieuGiamGiaKhachHang.class,
            SanPham.class, SanPhamChiTiet.class, ThuongHieu.class, TrongLuong.class, VaiTro.class, XuatXu.class);

    private static String gon(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    /** Tìm file SQL: chạy từ thư mục backend (Maven), từ thư mục gốc dự án (IDE) hoặc theo biến môi trường FOOTSTYLE_GOC. */
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

    private static String docTho(String ten) throws IOException {
        return Files.readString(timFile(ten), StandardCharsets.UTF_8).replace("﻿", "").replace("\r\n", "\n");
    }

    /** Lệnh thêm / sửa dữ liệu thì nằm ở file dữ liệu, còn lại (tạo bảng, cột, chỉ mục) nằm ở file cấu trúc. */
    static boolean laLenhDuLieu(String lenh) {
        String l = gon(lenh);
        return l.startsWith("INSERT ") || l.startsWith("UPDATE ") || l.startsWith("DELETE ") || l.contains("EXEC('UPDATE ");
    }

    /** Bỏ khối chú thích, chú thích cuối dòng (ngoài chuỗi '...') rồi gom khoảng trắng. */
    static String boChuThich(String sql) {
        String t = sql.replaceAll("(?s)/\\*.*?\\*/", "");
        StringBuilder sb = new StringBuilder();
        for (String dong : t.split("\n")) {
            boolean trongChuoi = false;
            int i = 0;
            for (; i < dong.length(); i++) {
                char c = dong.charAt(i);
                if (c == '\'') trongChuoi = !trongChuoi;
                if (!trongChuoi && c == '-' && i + 1 < dong.length() && dong.charAt(i + 1) == '-') break;
            }
            sb.append(dong, 0, i).append('\n');
        }
        return gon(sb.toString());
    }

    @Test
    void thuMucDatabase_chiCoDung2FileSql() throws IOException {
        Path thuMuc = timFile(CAU_TRUC).getParent();
        try (Stream<Path> ds = Files.list(thuMuc)) {
            List<String> sql = ds.map(p -> p.getFileName().toString()).filter(n -> n.toLowerCase(java.util.Locale.ROOT).endsWith(".sql")).sorted().toList();
            assertEquals(List.of(CAU_TRUC, DU_LIEU), sql, "database/ chỉ có file cấu trúc + file dữ liệu");
        }
    }

    @Test
    void fileCauTruc_khongCoLenhDuLieu() throws IOException {
        String sql = boChuThich(docTho(CAU_TRUC));
        for (String mau : List.of("INSERT INTO", "DELETE FROM", "MERGE ", "TRUNCATE ")) {
            assertFalse(sql.contains(mau), CAU_TRUC + " không được có lệnh dữ liệu: " + mau);
        }
        assertFalse(Pattern.compile("\\bUPDATE\\s+\\w+\\s+SET\\b").matcher(sql).find(), CAU_TRUC + " không được có UPDATE ... SET");
        assertTrue(sql.contains("IF DB_ID('shop_cau_long_footstyle') IS NULL CREATE DATABASE shop_cau_long_footstyle;"));
    }

    @Test
    void fileDuLieu_khongTaoSuaBangCotChiMuc() throws IOException {
        String sql = boChuThich(docTho(DU_LIEU));
        assertFalse(Pattern.compile("CREATE TABLE (?!#)").matcher(sql).find(), "chỉ được tạo bảng tạm #...");
        assertFalse(Pattern.compile("DROP TABLE (?!#)").matcher(sql).find(), "chỉ được xóa bảng tạm #...");
        assertFalse(sql.contains("ALTER TABLE"), DU_LIEU + " không được sửa bảng");
        assertFalse(Pattern.compile("CREATE (UNIQUE )?INDEX").matcher(sql).find(), DU_LIEU + " không được tạo chỉ mục");
        assertFalse(sql.contains("DROP INDEX"));
        assertFalse(sql.contains("CREATE DATABASE"));
        // Hàm tạm rút gọn mã: tạo ra thì phải xóa ngay
        assertTrue(sql.contains("CREATE FUNCTION dbo.fn_rut_gon_ma") == sql.contains("DROP FUNCTION dbo.fn_rut_gon_ma;"));
    }

    /** Thân "CREATE TABLE bang (...)" (tới dấu ngoặc đóng tương ứng). */
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

    static Set<String> cotTrongBang(String than) {
        Set<String> cot = new LinkedHashSet<>();
        String trong = than.substring(than.indexOf('(') + 1, than.length() - 1);
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
            if (m.find() && !List.of("CONSTRAINT", "FOREIGN", "PRIMARY").contains(m.group(1).toUpperCase(java.util.Locale.ROOT))) cot.add(m.group(1));
        }
        return cot;
    }

    @Test
    void moiCotThemChoCsdlCu_cungCoTrongCreateTable() throws IOException {
        String sql = boChuThich(docTho(CAU_TRUC));
        Matcher m = Pattern.compile("IF COL_LENGTH\\('(\\w+)', '(\\w+)'\\) IS NULL ALTER TABLE (\\w+) ADD (\\w+) ").matcher(sql);
        int dem = 0;
        while (m.find()) {
            dem++;
            assertEquals(m.group(1), m.group(3));
            assertEquals(m.group(2), m.group(4));
            String than = thanBang(sql, m.group(1));
            assertTrue(than != null, "chưa có CREATE TABLE " + m.group(1));
            assertTrue(cotTrongBang(than).contains(m.group(2)),
                    "CSDL cũ được thêm cột " + m.group(1) + "." + m.group(2) + " nhưng CREATE TABLE (CSDL mới) lại thiếu cột này");
        }
        assertTrue(dem >= 7, "phải có các lệnh nâng cấp CSDL cũ (đang thấy " + dem + ")");
    }

    @Test
    void moiBangDuocTaoMotLan_coDieuKienChayLaiDuoc() throws IOException {
        String sql = boChuThich(docTho(CAU_TRUC));
        Matcher m = Pattern.compile("CREATE TABLE (\\w+) \\(").matcher(sql);
        Set<String> bang = new LinkedHashSet<>();
        while (m.find()) {
            assertTrue(bang.add(m.group(1)), "tạo bảng 2 lần: " + m.group(1));
            assertTrue(sql.contains("IF OBJECT_ID('" + m.group(1) + "', 'U') IS NULL CREATE TABLE " + m.group(1) + " ("),
                    "CREATE TABLE " + m.group(1) + " phải có IF OBJECT_ID(...) IS NULL để chạy lại không lỗi");
        }
        assertEquals(29, bang.size());
        // Khóa ngoại trỏ tới bảng tạo trước đó
        Matcher fk = Pattern.compile("REFERENCES (\\w+)\\(id\\)").matcher(sql);
        while (fk.find()) {
            String duocTro = fk.group(1);
            int viTriTao = sql.indexOf("CREATE TABLE " + duocTro + " (");
            assertTrue(viTriTao >= 0 && viTriTao < fk.start(), "khóa ngoại trỏ tới " + duocTro + " trước khi bảng đó được tạo");
        }
    }

    @Test
    void moiBangFileDuLieuGhiVao_deuCoTrongFileCauTruc() throws IOException {
        String cauTruc = boChuThich(docTho(CAU_TRUC));
        String duLieu = boChuThich(docTho(DU_LIEU));
        Matcher m = Pattern.compile("INSERT INTO (\\w+) \\(([^)]*)\\)").matcher(duLieu);
        int dem = 0;
        while (m.find()) {
            dem++;
            String than = thanBang(cauTruc, m.group(1));
            assertTrue(than != null, DU_LIEU + " ghi vào bảng chưa tạo: " + m.group(1));
            Set<String> cot = cotTrongBang(than);
            for (String c : m.group(2).split(",")) {
                assertTrue(cot.contains(c.trim()), DU_LIEU + " ghi vào cột chưa có: " + m.group(1) + "." + c.trim());
            }
        }
        assertTrue(dem > 30);
    }

    /** Tên cột của một field theo cách Hibernate của Spring Boot đặt (CamelCaseToUnderscoresNamingStrategy). */
    private static String snake(String ten) {
        return ten.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    static Set<String> cotCuaEntity(Class<?> c) {
        Map<String, String> ghiDe = new HashMap<>();
        for (AttributeOverride o : c.getAnnotationsByType(AttributeOverride.class)) ghiDe.put(o.name(), o.column().name());
        Set<String> cot = new LinkedHashSet<>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.isAnnotationPresent(Transient.class) || f.isAnnotationPresent(OneToMany.class)) {
                    continue;
                }
                String ten;
                if (ghiDe.containsKey(f.getName())) ten = ghiDe.get(f.getName());
                else if (f.isAnnotationPresent(JoinColumn.class)) ten = f.getAnnotation(JoinColumn.class).name();
                else if (f.isAnnotationPresent(Column.class) && !f.getAnnotation(Column.class).name().isEmpty()) ten = f.getAnnotation(Column.class).name();
                else if (f.isAnnotationPresent(ManyToOne.class)) ten = snake(f.getName()) + "_id";
                else ten = snake(f.getName());
                cot.add(ten);
            }
        }
        return cot;
    }

    @Test
    void moiCotCuaMoiEntity_deuCoTrongBang() throws IOException {
        String sql = boChuThich(docTho(CAU_TRUC));
        for (Class<?> c : ENTITY) {
            String bang = c.getAnnotation(Table.class).name();
            String than = thanBang(sql, bang);
            assertTrue(than != null, c.getSimpleName() + ": chưa có CREATE TABLE " + bang);
            Set<String> cot = cotTrongBang(than);
            for (String ten : cotCuaEntity(c)) {
                assertTrue(cot.contains(ten), c.getSimpleName() + " -> cột " + bang + "." + ten + " không có trong " + CAU_TRUC + " " + cot);
            }
        }
    }

    /** Lệnh backend tự chạy khi khởi động mà file SQL làm theo cách khác (vẫn cùng kết quả) - không so từng chữ. */
    private static final List<String> CHI_BACKEND = List.of(
            "INSERT INTO chat_lieu (ma_chat_lieu, ten_chat_lieu, trang_thai, nhom_ap_dung) SELECT 'CL' +", // file SQL thêm theo mã cố định CL05 - CL15
            "ux_khach_hang_email", "ux_khach_hang_sdt"); // file SQL in danh sách trùng ra trước khi tạo chỉ mục

    @Test
    void lenhBackendTuNangCap_coTrongDungFile() throws IOException {
        String cauTruc = gon(boChuThich(docTho(CAU_TRUC)).replace("( ", "(").replace(" )", ")"));
        String duLieu = gon(boChuThich(docTho(DU_LIEU)).replace("( ", "(").replace(" )", ")"));
        for (String lenh : SchemaUpgradeRunner.LENH) {
            String l = gon(lenh).replace("( ", "(").replace(" )", ")");
            if (CHI_BACKEND.stream().anyMatch(l::contains)) {
                continue;
            }
            if (laLenhDuLieu(lenh)) {
                assertTrue(duLieu.contains(l + ";"), DU_LIEU + " thiếu / khác lệnh của SchemaUpgradeRunner:\n" + lenh);
            } else {
                assertTrue(cauTruc.contains(l + ";"), CAU_TRUC + " thiếu / khác lệnh của SchemaUpgradeRunner:\n" + lenh);
            }
        }
        assertTrue(cauTruc.contains("ux_khach_hang_email ON khach_hang (email) WHERE email IS NOT NULL AND email <> ''''"));
        assertTrue(cauTruc.contains("ux_khach_hang_sdt ON khach_hang (sdt) WHERE sdt IS NOT NULL AND sdt <> ''''"));
    }
}

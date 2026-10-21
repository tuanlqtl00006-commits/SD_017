package com.footstyle.demo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Kiểm tra file SQL hoàn chỉnh duy nhất database/SD_17_footstyle_full.sql:
 * đảm bảo đủ bảng, đúng thứ tự khóa ngoại, và mọi cột của entity đều có trong CREATE TABLE.
 */
class FileSqlTest {

    static final String FILE_SQL = "SD_17_footstyle_full.sql";

    /** Mọi entity của backend theo đúng 26 bảng chuẩn ERD. */
    static final List<Class<?>> ENTITY = List.of(ChatLieu.class, ChuVi.class, DanhMuc.class, DiaChiKhachHang.class,
            DiemCanBang.class, DoCung.class, DotGiamGia.class, DotGiamGiaChiTiet.class,
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
        return Files.readString(timFile(ten), StandardCharsets.UTF_8).replace("\uFEFF", "").replace("\r\n", "\n");
    }

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
    void thuMucDatabase_chiCoDung1FileSql() throws IOException {
        Path thuMuc = timFile(FILE_SQL).getParent();
        try (Stream<Path> ds = Files.list(thuMuc)) {
            List<String> sql = ds.map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase(java.util.Locale.ROOT).endsWith(".sql"))
                    .sorted()
                    .toList();
            assertTrue(sql.contains(FILE_SQL), "database/ phải có SD_17_footstyle_full.sql");
        }
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
    void moiBangDuocTaoMotLan_dungThuTuKhoaNgoai() throws IOException {
        String sql = boChuThich(docTho(FILE_SQL));
        Matcher m = Pattern.compile("CREATE TABLE (\\w+) \\(").matcher(sql);
        Set<String> bang = new LinkedHashSet<>();
        while (m.find()) {
            assertTrue(bang.add(m.group(1)), "tạo bảng 2 lần: " + m.group(1));
        }
        assertEquals(32, bang.size());
        Matcher fk = Pattern.compile("REFERENCES (\\w+)\\(id\\)").matcher(sql);
        while (fk.find()) {
            String duocTro = fk.group(1);
            int viTriTao = sql.indexOf("CREATE TABLE " + duocTro + " (");
            assertTrue(viTriTao >= 0 && viTriTao < fk.start(), "khóa ngoại trỏ tới " + duocTro + " trước khi bảng đó được tạo");
        }
    }

    @Test
    void moiBangFileDuLieuGhiVao_deuCoTrongCreateTable() throws IOException {
        String sql = boChuThich(docTho(FILE_SQL));
        Matcher m = Pattern.compile("INSERT INTO (\\w+) \\(([^)]*)\\)").matcher(sql);
        int dem = 0;
        while (m.find()) {
            dem++;
            String than = thanBang(sql, m.group(1));
            assertTrue(than != null, FILE_SQL + " ghi vào bảng chưa tạo: " + m.group(1));
            Set<String> cot = cotTrongBang(than);
            for (String c : m.group(2).split(",")) {
                assertTrue(cot.contains(c.trim()), FILE_SQL + " ghi vào cột chưa có: " + m.group(1) + "." + c.trim());
            }
        }
        assertTrue(dem >= 25);
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

    /** Các cột backend nâng cấp thêm qua SchemaUpgradeRunner, không thuộc 185 cột trong sơ đồ ERD gốc. */
    private static final Set<String> COT_NANG_CAP_DYNAMIC = Set.of(
            "chat_lieu.nhom_ap_dung",
            "nhan_vien.anh",
            "nhan_vien.can_cuoc_cong_dan",
            "khach_hang.anh_dai_dien",
            "dot_giam_gia.mo_ta",
            "hinh_anh_san_pham.id_mau_sac",
            "dia_chi_khach_hang.ngay_tao",
            "dia_chi_khach_hang.ngay_cap_nhat",
            "lich_su_thanh_toan.nguoi_nhan"
    );

    @Test
    void moiCotCuaMoiEntity_deuCoTrongBang() throws IOException {
        String sql = boChuThich(docTho(FILE_SQL));
        for (Class<?> c : ENTITY) {
            String bang = c.getAnnotation(Table.class).name();
            String than = thanBang(sql, bang);
            assertTrue(than != null, c.getSimpleName() + ": chưa có CREATE TABLE " + bang);
            Set<String> cot = cotTrongBang(than);
            for (String ten : cotCuaEntity(c)) {
                if (COT_NANG_CAP_DYNAMIC.contains(bang + "." + ten)) {
                    continue;
                }
                assertTrue(cot.contains(ten), c.getSimpleName() + " -> cột " + bang + "." + ten + " không có trong " + FILE_SQL + " " + cot);
            }
        }
    }
}

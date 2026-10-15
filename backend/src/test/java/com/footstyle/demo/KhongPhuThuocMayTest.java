package com.footstyle.demo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Rào chắn để code không dính vào cài đặt riêng của từng máy (ngôn ngữ, bảng mã): những lỗi này chỉ hiện ra trên máy khác,
 * máy của người viết chạy vẫn đúng. Quét toàn bộ mã nguồn backend và báo lỗi nếu có:
 * <ul>
 *   <li>{@code toUpperCase()} / {@code toLowerCase()} không nêu Locale: máy đặt tiếng Thổ Nhĩ Kỳ thì "i" thành "İ", mã sản phẩm / phiếu bị từ chối;</li>
 *   <li>{@code String.format(...)}, {@code .formatted(...)}, {@code printf} không nêu Locale: máy đặt tiếng Ả Rập / Thái thì số thành "٠٣" / "๐๓", mã nhân viên NV03 thành "NV٠٣";</li>
 *   <li>{@code new Locale(...)}: đã lỗi thời từ JDK 19, dùng {@code Locale.forLanguageTag}; {@code new FileReader/FileWriter}: dùng bảng mã mặc định của máy.</li>
 * </ul>
 */
class KhongPhuThuocMayTest {

    private static final Pattern DOI_CHU_KHONG_LOCALE = Pattern.compile("\\.to(Upper|Lower)Case\\(\\s*\\)");
    private static final Pattern FORMAT_KHONG_LOCALE = Pattern.compile("String\\.format\\(\\s*(?!Locale\\.|[A-Za-z_]*[Ll]ocale\\b)");
    private static final Pattern CAC_KHAC = Pattern.compile("\\.formatted\\(|\\bprintf\\(|new\\s+Locale\\(|new\\s+FileReader\\(|new\\s+FileWriter\\(");

    private static Path thuMucMaNguon() {
        List<Path> ungVien = new ArrayList<>();
        ungVien.add(Path.of("src", "main", "java"));
        ungVien.add(Path.of("backend", "src", "main", "java"));
        String goc = System.getenv("FOOTSTYLE_GOC");
        if (goc != null && !goc.isBlank()) ungVien.add(Path.of(goc, "backend", "src", "main", "java"));
        for (Path p : ungVien) {
            if (Files.isDirectory(p)) return p;
        }
        throw new AssertionError("Không tìm thấy mã nguồn backend (đã thử " + ungVien + "). Chạy test từ thư mục backend hoặc đặt FOOTSTYLE_GOC.");
    }

    @Test
    void maNguonKhongDungHamPhuThuocNgonNguHayBangMaCuaMay() throws IOException {
        List<String> loi = new ArrayList<>();
        int soFile = 0;
        try (Stream<Path> ds = Files.walk(thuMucMaNguon())) {
            for (Path f : (Iterable<Path>) ds.filter(p -> p.toString().endsWith(".java"))::iterator) {
                soFile++;
                List<String> dong = Files.readAllLines(f, StandardCharsets.UTF_8);
                for (int i = 0; i < dong.size(); i++) {
                    String d = dong.get(i);
                    String code = d.strip();
                    if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) continue; // bỏ chú thích
                    if (DOI_CHU_KHONG_LOCALE.matcher(d).find() || FORMAT_KHONG_LOCALE.matcher(d).find() || CAC_KHAC.matcher(d).find()) {
                        loi.add(f.getFileName() + ":" + (i + 1) + "  " + code);
                    }
                }
            }
        }
        assertTrue(soFile > 50, "quét được quá ít file (" + soFile + "), đường dẫn mã nguồn có đúng không?");
        assertTrue(loi.isEmpty(), "Có chỗ phụ thuộc cài đặt của máy (thêm Locale.ROOT / Locale.forLanguageTag(\"vi-VN\") hoặc nêu rõ UTF-8):\n  " + String.join("\n  ", loi));
    }
}

package com.footstyle.demo.config;

import com.footstyle.demo.service.MaUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tự bổ sung phần còn thiếu của CSDL cũ khi backend khởi động (chạy lại nhiều lần vẫn an toàn).
 *
 * Vì sao cần: nếu CSDL được tạo từ bản SD_17.sql cũ thì bảng nhan_vien chưa có cột can_cuoc_cong_dan và chưa có
 * bảng dia_chi_nhan_vien. Khi đó mọi câu truy vấn có đọc nhan_vien (danh sách nhân viên,
 * danh sách hóa đơn...) báo lỗi "Invalid column name 'can_cuoc_cong_dan'". Các lệnh dưới đây giống hệt phần
 * tương ứng trong database/SD_17_du_lieu_va_nang_cap.sql; CSDL đã đủ cột / bảng thì không có gì thay đổi.
 *
 * Mỗi lệnh chạy riêng (không gộp batch) vì SQL Server không cho dùng cột vừa ALTER ADD
 * ở cùng một batch (tạo chỉ mục theo cột can_cuoc_cong_dan phải nằm ở lệnh sau).
 */
@Component
@Order(0)
public class SchemaUpgradeRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgradeRunner.class);

    /** Các lệnh nâng cấp, theo đúng thứ tự phụ thuộc. */
    static final List<String> LENH = List.of(
            // nhan_vien.can_cuoc_cong_dan (số căn cước). Bản cũ từng dùng tên cột cccd: dọn chỉ mục cũ rồi chép dữ liệu sang cột mới.
            "IF COL_LENGTH('nhan_vien', 'can_cuoc_cong_dan') IS NULL ALTER TABLE nhan_vien ADD can_cuoc_cong_dan VARCHAR(12) NULL",
            "IF EXISTS (SELECT 1 FROM sys.indexes i JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id "
                    + "JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id "
                    + "WHERE i.name = 'ux_nhan_vien_cccd' AND i.object_id = OBJECT_ID('nhan_vien') AND c.name = 'cccd') "
                    + "DROP INDEX ux_nhan_vien_cccd ON nhan_vien",
            "IF COL_LENGTH('nhan_vien', 'cccd') IS NOT NULL "
                    + "EXEC('UPDATE nhan_vien SET can_cuoc_cong_dan = cccd WHERE can_cuoc_cong_dan IS NULL AND cccd IS NOT NULL')",
            "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_cccd' AND object_id = OBJECT_ID('nhan_vien')) "
                    + "CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (can_cuoc_cong_dan) WHERE can_cuoc_cong_dan IS NOT NULL",
            // dia_chi_nhan_vien (nhân viên có nhiều địa chỉ) + chuyển địa chỉ cũ thành địa chỉ chính
            "IF OBJECT_ID('dia_chi_nhan_vien', 'U') IS NULL CREATE TABLE dia_chi_nhan_vien ("
                    + "id INT IDENTITY(1,1) PRIMARY KEY, "
                    + "id_nhan_vien INT NOT NULL, "
                    + "tinh_thanh NVARCHAR(100), "
                    + "phuong_xa NVARCHAR(100), "
                    + "dia_chi_chi_tiet NVARCHAR(500) NOT NULL, "
                    + "la_mac_dinh BIT NOT NULL DEFAULT 0, "
                    + "trang_thai TINYINT DEFAULT 1, "
                    + "ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + "ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id))",
            "INSERT INTO dia_chi_nhan_vien (id_nhan_vien, dia_chi_chi_tiet, la_mac_dinh) "
                    + "SELECT n.id, n.dia_chi, 1 FROM nhan_vien n "
                    + "WHERE n.dia_chi IS NOT NULL AND LTRIM(RTRIM(n.dia_chi)) <> '' "
                    + "AND NOT EXISTS (SELECT 1 FROM dia_chi_nhan_vien d WHERE d.id_nhan_vien = n.id)",
            // dia_chi_khach_hang.ngay_tao / ngay_cap_nhat
            "IF COL_LENGTH('dia_chi_khach_hang', 'ngay_tao') IS NULL "
                    + "ALTER TABLE dia_chi_khach_hang ADD ngay_tao DATETIME NULL CONSTRAINT df_dckh_ngay_tao DEFAULT CURRENT_TIMESTAMP",
            "IF COL_LENGTH('dia_chi_khach_hang', 'ngay_cap_nhat') IS NULL "
                    + "ALTER TABLE dia_chi_khach_hang ADD ngay_cap_nhat DATETIME NULL CONSTRAINT df_dckh_ngay_cap_nhat DEFAULT CURRENT_TIMESTAMP",
            // dot_giam_gia.mo_ta
            "IF COL_LENGTH('dot_giam_gia', 'mo_ta') IS NULL ALTER TABLE dot_giam_gia ADD mo_ta NVARCHAR(500) NULL",
            // Mục "Không áp dụng" cho danh mục không phải vợt (Phụ kiện, Túi - balo...): không có độ cứng, điểm cân bằng, trọng lượng, chu vi
            // Mã gọn DC00, CB00, TL00, CV00 (CSDL cũ có DC000... sẽ được đổi sang mã gọn ở bước rutGonMaCu bên dưới)
            "INSERT INTO do_cung (ma_do_cung, ten_do_cung, trang_thai) SELECT 'DC00', N'Không áp dụng', 1 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM do_cung t WHERE t.ma_do_cung IN ('DC00', 'DC000') OR t.ten_do_cung = N'Không áp dụng')",
            "INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai) SELECT 'CB00', N'Không áp dụng', 1 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM diem_can_bang t WHERE t.ma_diem_can_bang IN ('CB00', 'CB000') OR t.ten_diem_can_bang = N'Không áp dụng')",
            "INSERT INTO trong_luong (ma_trong_luong, ten_trong_luong, trang_thai) SELECT 'TL00', N'Không áp dụng', 1 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM trong_luong t WHERE t.ma_trong_luong IN ('TL00', 'TL000') OR t.ten_trong_luong = N'Không áp dụng')",
            "INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai) SELECT 'CV00', N'Không áp dụng', 1 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM chu_vi t WHERE t.ma_chu_vi IN ('CV00', 'CV000') OR t.ten_chu_vi = N'Không áp dụng')",
            // Chất liệu theo danh mục: cột nhom_ap_dung (các nhóm danh mục dùng chất liệu, NULL = mọi danh mục)
            "IF COL_LENGTH('chat_lieu', 'nhom_ap_dung') IS NULL ALTER TABLE chat_lieu ADD nhom_ap_dung VARCHAR(100) NULL",
            "UPDATE chat_lieu SET nhom_ap_dung = 'VOT' "
                    + "WHERE nhom_ap_dung IS NULL AND ten_chat_lieu IN (N'Carbon', N'Graphite', N'Nhôm', N'Titanium')",
            "UPDATE chat_lieu SET nhom_ap_dung = 'TUI_BALO,PHU_KIEN' WHERE nhom_ap_dung IS NULL AND ten_chat_lieu = N'Da PU'",
            // Chất liệu cho quần áo, túi - balo, phụ kiện, quả cầu lông (chỉ thêm khi chưa có tên đó; mã = số lớn nhất + thứ tự)
            "INSERT INTO chat_lieu (ma_chat_lieu, ten_chat_lieu, trang_thai, nhom_ap_dung) "
                    + "SELECT 'CL' + CASE WHEN m.so + ROW_NUMBER() OVER (ORDER BY v.thu_tu) < 10 THEN '0' ELSE '' END "
                    + "+ CAST(m.so + ROW_NUMBER() OVER (ORDER BY v.thu_tu) AS VARCHAR(10)), v.ten, 1, v.nhom "
                    + "FROM (VALUES (1, N'Cotton', 'QUAN_AO,PHU_KIEN'), (2, N'Polyester', 'QUAN_AO,TUI_BALO,PHU_KIEN'), "
                    + "(3, N'Thun lạnh', 'QUAN_AO'), (4, N'Spandex', 'QUAN_AO'), (5, N'Nylon', 'TUI_BALO,PHU_KIEN'), "
                    + "(6, N'Vải dù', 'TUI_BALO'), (7, N'Cao su', 'PHU_KIEN'), (8, N'Lông ngỗng', 'QUA_CAU'), "
                    + "(9, N'Lông vịt', 'QUA_CAU'), (10, N'Nhựa', 'QUA_CAU,PHU_KIEN')) AS v(thu_tu, ten, nhom) "
                    + "CROSS JOIN (SELECT ISNULL(MAX(TRY_CAST(SUBSTRING(ma_chat_lieu, 3, 10) AS INT)), 0) AS so FROM chat_lieu) m "
                    + "WHERE NOT EXISTS (SELECT 1 FROM chat_lieu t WHERE t.ten_chat_lieu = v.ten)",
            // Ảnh theo màu: hinh_anh_san_pham.id_mau_sac (NULL = ảnh chung của sản phẩm, như dữ liệu cũ; có giá trị = ảnh của màu đó,
            // dùng cho mọi biến thể cùng màu). Thêm cột + khóa ngoại ở 1 lệnh, chỉ mục ở lệnh sau (không dùng cột vừa thêm trong cùng batch).
            "IF COL_LENGTH('hinh_anh_san_pham', 'id_mau_sac') IS NULL "
                    + "ALTER TABLE hinh_anh_san_pham ADD id_mau_sac INT NULL CONSTRAINT fk_hasp_mau_sac FOREIGN KEY REFERENCES mau_sac(id)",
            "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_hasp_san_pham_mau' AND object_id = OBJECT_ID('hinh_anh_san_pham')) "
                    + "CREATE INDEX ix_hasp_san_pham_mau ON hinh_anh_san_pham (id_san_pham, id_mau_sac)");

    /**
     * Các bảng có mã do hệ thống tự cấp: {bảng, cột mã}. Mã cũ nhiều số 0 (HD000001, NV0001, SP001, DGG001, DC000...)
     * được đổi sang dạng gọn (HD01, NV01, SP01, DGG01, DC00...). Mã nhập tay (phiếu giảm giá VCH...) không nằm trong danh sách này.
     * san_pham phải đứng trước san_pham_chi_tiet vì mã biến thể bắt đầu bằng mã sản phẩm.
     */
    static final List<String[]> BANG_MA = List.of(
            new String[] {"nhan_vien", "ma_nhan_vien"},
            new String[] {"khach_hang", "ma_khach_hang"},
            new String[] {"hoa_don", "ma_hoa_don"},
            new String[] {"dot_giam_gia", "ma_dot"},
            new String[] {"san_pham", "ma_san_pham"},
            new String[] {"danh_muc", "ma_danh_muc"},
            new String[] {"thuong_hieu", "ma_thuong_hieu"},
            new String[] {"xuat_xu", "ma_xuat_xu"},
            new String[] {"chat_lieu", "ma_chat_lieu"},
            new String[] {"do_cung", "ma_do_cung"},
            new String[] {"diem_can_bang", "ma_diem_can_bang"},
            new String[] {"mau_sac", "ma_mau_sac"},
            new String[] {"trong_luong", "ma_trong_luong"},
            new String[] {"chu_vi", "ma_chu_vi"});

    private final JdbcTemplate jdbc;

    public SchemaUpgradeRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String sql : LENH) {
            try {
                // Lệnh INSERT cần QUOTED_IDENTIFIER ON khi bảng có chỉ mục lọc; JDBC mặc định đã bật.
                jdbc.execute(sql);
            } catch (Exception ex) {
                // Không làm backend chết: ghi log để người chạy biết cần chạy tay database/SD_17_du_lieu_va_nang_cap.sql.
                log.warn("Không tự nâng cấp được CSDL ({}). Hãy chạy tay database/SD_17_du_lieu_va_nang_cap.sql. Lệnh: {}",
                        ex.getMessage(), sql);
            }
        }
        rutGonMaCu();
        log.info("Đã kiểm tra / nâng cấp cấu trúc CSDL (can_cuoc_cong_dan, dia_chi_nhan_vien, chat_lieu.nhom_ap_dung, hinh_anh_san_pham.id_mau_sac, mã gọn...).");
    }

    /**
     * Đổi mọi mã cũ nhiều số 0 sang mã gọn (HD000001 -> HD01, NV0001 -> NV01, SP001-DEN-4U-G5 -> SP01-DEN-4U-G5...).
     * Chạy lại nhiều lần vẫn an toàn: mã đã gọn thì không đổi; mã gọn sẽ trùng với mã đang có thì giữ nguyên mã cũ.
     * Chỉ đổi cột mã (không khóa ngoại nào trỏ vào cột mã, các bảng liên kết bằng id) nên không làm mất liên kết dữ liệu.
     */
    void rutGonMaCu() {
        int tong = 0;
        for (String[] b : BANG_MA) {
            try {
                tong += rutGonCot(b[0], b[1]);
            } catch (Exception ex) {
                log.warn("Không rút gọn được mã cũ của bảng {} ({}).", b[0], ex.getMessage());
            }
        }
        try {
            tong += rutGonMaBienThe();
        } catch (Exception ex) {
            log.warn("Không rút gọn được mã biến thể san_pham_chi_tiet ({}).", ex.getMessage());
        }
        if (tong > 0) log.info("Đã rút gọn {} mã cũ nhiều số 0 (ví dụ HD000001 -> HD01).", tong);
    }

    /** Rút gọn một cột mã; trả về số dòng đã đổi. */
    int rutGonCot(String bang, String cot) {
        List<Map<String, Object>> ds = jdbc.queryForList("SELECT id, " + cot + " AS ma FROM " + bang);
        Set<String> dangCo = new HashSet<>();
        for (Map<String, Object> r : ds) {
            if (r.get("ma") != null) dangCo.add(r.get("ma").toString().trim().toLowerCase(Locale.ROOT));
        }
        int doi = 0;
        for (Map<String, Object> r : ds) {
            if (r.get("ma") == null) continue;
            String cu = r.get("ma").toString();
            String moi = MaUtil.rutGon(cu);
            if (moi == null || moi.equals(cu)) continue;
            String khoaMoi = moi.toLowerCase(Locale.ROOT);
            if (dangCo.contains(khoaMoi)) continue; // đã có mã gọn này rồi: giữ mã cũ để không vi phạm UNIQUE
            jdbc.update("UPDATE " + bang + " SET " + cot + " = ? WHERE id = ?", moi, r.get("id"));
            dangCo.remove(cu.trim().toLowerCase(Locale.ROOT));
            dangCo.add(khoaMoi);
            doi++;
        }
        return doi;
    }

    /** Mã biến thể bắt đầu bằng mã sản phẩm (SP001-DEN-4U-G5): đổi theo mã sản phẩm đã gọn (SP01-DEN-4U-G5). */
    int rutGonMaBienThe() {
        List<Map<String, Object>> ds = jdbc.queryForList(
                "SELECT c.id AS id, c.ma_spct AS ma, p.ma_san_pham AS ma_sp FROM san_pham_chi_tiet c JOIN san_pham p ON p.id = c.id_san_pham");
        Set<String> dangCo = new HashSet<>();
        for (Map<String, Object> r : ds) {
            if (r.get("ma") != null) dangCo.add(r.get("ma").toString().trim().toLowerCase(Locale.ROOT));
        }
        int doi = 0;
        for (Map<String, Object> r : ds) {
            if (r.get("ma") == null || r.get("ma_sp") == null) continue;
            String cu = r.get("ma").toString();
            String moi = MaUtil.rutGonMaBienThe(cu);
            if (moi == null || moi.equals(cu)) continue;
            // Chỉ đổi khi phần đầu mã đúng là mã sản phẩm hiện tại; mã lạ (nhập tay) thì để nguyên.
            if (!moi.toUpperCase(Locale.ROOT).startsWith(r.get("ma_sp").toString().trim().toUpperCase(Locale.ROOT) + "-")) continue;
            String khoaMoi = moi.toLowerCase(Locale.ROOT);
            if (dangCo.contains(khoaMoi)) continue;
            jdbc.update("UPDATE san_pham_chi_tiet SET ma_spct = ? WHERE id = ?", moi, r.get("id"));
            dangCo.remove(cu.trim().toLowerCase(Locale.ROOT));
            dangCo.add(khoaMoi);
            doi++;
        }
        return doi;
    }
}

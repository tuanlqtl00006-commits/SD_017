package com.footstyle.demo.config;

import java.util.List;
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
 * Vì sao cần: nếu CSDL được tạo từ bản SD_17.sql cũ thì bảng nhan_vien chưa có cột cccd và chưa có
 * bảng vi_tri / nhan_vien_vi_tri. Khi đó mọi câu truy vấn có đọc nhan_vien (danh sách nhân viên,
 * danh sách hóa đơn...) báo lỗi "Invalid column name 'cccd'". Các lệnh dưới đây giống hệt phần
 * tương ứng trong phần 6 "Nâng cấp CSDL cũ" của database/SD_17_tong.sql; CSDL đã đủ cột / bảng thì không có gì thay đổi.
 *
 * Mỗi lệnh chạy riêng (không gộp batch) vì SQL Server không cho dùng cột vừa ALTER ADD
 * ở cùng một batch (tạo chỉ mục theo cột cccd phải nằm ở lệnh sau).
 */
@Component
@Order(0)
public class SchemaUpgradeRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaUpgradeRunner.class);

    /** Các lệnh nâng cấp, theo đúng thứ tự phụ thuộc. */
    static final List<String> LENH = List.of(
            // nhan_vien.cccd
            "IF COL_LENGTH('nhan_vien', 'cccd') IS NULL ALTER TABLE nhan_vien ADD cccd VARCHAR(12) NULL",
            // Bản cũ đặt tên cột là can_cuoc_cong_dan: chép số sang cccd và bỏ chỉ mục cũ (cột cũ được giữ lại, không mất dữ liệu)
            "IF COL_LENGTH('nhan_vien', 'can_cuoc_cong_dan') IS NOT NULL AND EXISTS (SELECT 1 FROM sys.indexes i "
                    + "JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id "
                    + "JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id "
                    + "WHERE i.name = 'ux_nhan_vien_cccd' AND i.object_id = OBJECT_ID('nhan_vien') AND c.name = 'can_cuoc_cong_dan') "
                    + "DROP INDEX ux_nhan_vien_cccd ON nhan_vien",
            "IF COL_LENGTH('nhan_vien', 'can_cuoc_cong_dan') IS NOT NULL "
                    + "EXEC('UPDATE nhan_vien SET cccd = can_cuoc_cong_dan WHERE cccd IS NULL AND can_cuoc_cong_dan IS NOT NULL')",
            "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_cccd' AND object_id = OBJECT_ID('nhan_vien')) "
                    + "CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (cccd) WHERE cccd IS NOT NULL",
            // vi_tri
            "IF OBJECT_ID('vi_tri', 'U') IS NULL CREATE TABLE vi_tri ("
                    + "id INT IDENTITY(1,1) PRIMARY KEY, "
                    + "ten_vi_tri NVARCHAR(100) NOT NULL UNIQUE, "
                    + "trang_thai TINYINT DEFAULT 1)",
            // nhan_vien_vi_tri
            "IF OBJECT_ID('nhan_vien_vi_tri', 'U') IS NULL CREATE TABLE nhan_vien_vi_tri ("
                    + "id_nhan_vien INT NOT NULL, "
                    + "id_vi_tri INT NOT NULL, "
                    + "PRIMARY KEY (id_nhan_vien, id_vi_tri), "
                    + "FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id), "
                    + "FOREIGN KEY (id_vi_tri) REFERENCES vi_tri(id))",
            // vị trí mẫu (chỉ thêm khi chưa có tên đó)
            "INSERT INTO vi_tri (ten_vi_tri, trang_thai) "
                    + "SELECT v.ten, 1 FROM (VALUES (N'Bán hàng tại quầy'), (N'Thu ngân'), (N'Kho'), (N'Giao hàng'), (N'Chăm sóc khách hàng')) AS v(ten) "
                    + "WHERE NOT EXISTS (SELECT 1 FROM vi_tri t WHERE t.ten_vi_tri = v.ten)",
            // dia_chi_nhan_vien: nhân viên có nhiều địa chỉ; địa chỉ cũ ở cột dia_chi được chuyển thành địa chỉ chính (chạy lại không nhân đôi)
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
            "INSERT INTO do_cung (ma_do_cung, ten_do_cung, trang_thai) SELECT 'DC000', N'Không áp dụng', 1 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM do_cung t WHERE t.ma_do_cung = 'DC000')",
            "INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai) SELECT 'CB000', N'Không áp dụng', 1 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM diem_can_bang t WHERE t.ma_diem_can_bang = 'CB000')",
            "INSERT INTO trong_luong (ma_trong_luong, ten_trong_luong, trang_thai) SELECT 'TL000', N'Không áp dụng', 1 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM trong_luong t WHERE t.ma_trong_luong = 'TL000')",
            "INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai) SELECT 'CV000', N'Không áp dụng', 1 "
                    + "WHERE NOT EXISTS (SELECT 1 FROM chu_vi t WHERE t.ma_chu_vi = 'CV000')");

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
                // Không làm backend chết: ghi log để người chạy biết cần chạy tay phần 6 (Nâng cấp CSDL cũ) của database/SD_17_tong.sql.
                log.warn("Không tự nâng cấp được CSDL ({}). Hãy chạy tay phần 6 (Nâng cấp CSDL cũ) của database/SD_17_tong.sql. Lệnh: {}",
                        ex.getMessage(), sql);
            }
        }
        log.info("Đã kiểm tra / nâng cấp cấu trúc CSDL (cccd, vi_tri, nhan_vien_vi_tri, dia_chi_nhan_vien...).");
    }
}

package com.footstyle.demo.config;

import java.util.List;

/**
 * Lệnh nâng cấp CSDL cho giai đoạn 3 (ca làm việc, lịch làm việc, giao ca, bán tại quầy, chat). SchemaUpgradeRunner chạy sau các lệnh cũ.
 * Giống hệt PHẦN 9 của database/SD_17_du_lieu_va_nang_cap.sql và file database/SD_17_giai_doan_3.sql (trừ lịch làm mẫu chỉ có trong file SQL);
 * SchemaGiaiDoan3Test kiểm tra các bên không lệch nhau. Mỗi lệnh chạy riêng, chạy lại nhiều lần vẫn an toàn.
 */
public final class SchemaGiaiDoan3 {

    private SchemaGiaiDoan3() {
    }

    static final List<String> LENH = List.of(
            "IF OBJECT_ID('ca_lam_viec', 'U') IS NULL CREATE TABLE ca_lam_viec ( id INT IDENTITY(1,1) PRIMARY KEY, "
                    + "ma_ca VARCHAR(20) UNIQUE NOT NULL, ten_ca NVARCHAR(100) NOT NULL, gio_bat_dau TIME NOT NULL, gio_ket_thuc "
                    + "TIME NOT NULL, trang_thai TINYINT NOT NULL DEFAULT 1, ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + "ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP )",
            "IF OBJECT_ID('lich_lam_viec', 'U') IS NULL CREATE TABLE lich_lam_viec ( id INT IDENTITY(1,1) PRIMARY KEY, "
                    + "id_nhan_vien INT NOT NULL, id_ca_lam_viec INT NOT NULL, ngay_lam DATE NOT NULL, ghi_chu NVARCHAR(255), "
                    + "trang_thai TINYINT NOT NULL DEFAULT 1, ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP, CONSTRAINT "
                    + "ux_lich_lam_viec UNIQUE (id_nhan_vien, id_ca_lam_viec, ngay_lam), FOREIGN KEY (id_nhan_vien) REFERENCES "
                    + "nhan_vien(id), FOREIGN KEY (id_ca_lam_viec) REFERENCES ca_lam_viec(id) )",
            "IF OBJECT_ID('giao_ca', 'U') IS NULL CREATE TABLE giao_ca ( id INT IDENTITY(1,1) PRIMARY KEY, ma_giao_ca "
                    + "VARCHAR(20) UNIQUE NOT NULL, id_nhan_vien INT NOT NULL, id_lich_lam_viec INT NULL, thoi_gian_nhan_ca "
                    + "DATETIME NOT NULL, tien_dau_ca DECIMAL(18,0) NOT NULL DEFAULT 0, ghi_chu_nhan_ca NVARCHAR(500), "
                    + "so_phut_di_muon INT NULL, thoi_gian_ket_ca DATETIME NULL, so_hoa_don INT NULL, doanh_thu DECIMAL(18,0) "
                    + "NULL, tien_mat_thu DECIMAL(18,0) NULL, chuyen_khoan_thu DECIMAL(18,0) NULL, tien_mat_he_thong "
                    + "DECIMAL(18,0) NULL, tien_mat_thuc_te DECIMAL(18,0) NULL, chenh_lech DECIMAL(18,0) NULL, tien_ban_giao "
                    + "DECIMAL(18,0) NULL, id_nhan_vien_nhan_ban_giao INT NULL, ghi_chu_ket_ca NVARCHAR(500), trang_thai TINYINT "
                    + "NOT NULL DEFAULT 1, FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id), FOREIGN KEY (id_lich_lam_viec) "
                    + "REFERENCES lich_lam_viec(id), FOREIGN KEY (id_nhan_vien_nhan_ban_giao) REFERENCES nhan_vien(id) )",
            "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_giao_ca_dang_mo' AND object_id = "
                    + "OBJECT_ID('giao_ca')) CREATE UNIQUE INDEX ux_giao_ca_dang_mo ON giao_ca (id_nhan_vien) WHERE trang_thai = "
                    + "1",
            "IF COL_LENGTH('hoa_don', 'id_giao_ca') IS NULL ALTER TABLE hoa_don ADD id_giao_ca INT NULL CONSTRAINT "
                    + "fk_hoa_don_giao_ca FOREIGN KEY REFERENCES giao_ca(id)",
            "IF COL_LENGTH('lich_su_thanh_toan', 'id_phuong_thuc_thanh_toan') IS NULL ALTER TABLE lich_su_thanh_toan "
                    + "ADD id_phuong_thuc_thanh_toan INT NULL CONSTRAINT fk_lstt_phuong_thuc FOREIGN KEY REFERENCES "
                    + "phuong_thuc_thanh_toan(id)",
            "IF OBJECT_ID('hoi_thoai', 'U') IS NULL CREATE TABLE hoi_thoai ( id INT IDENTITY(1,1) PRIMARY KEY, loai "
                    + "TINYINT NOT NULL, tieu_de NVARCHAR(150), id_khach_hang INT NULL, ten_khach NVARCHAR(100), sdt_khach "
                    + "VARCHAR(15), ma_truy_cap VARCHAR(64), che_do TINYINT NOT NULL DEFAULT 1, id_nhan_vien_phu_trach INT NULL, "
                    + "khoa_rieng VARCHAR(50) NULL, ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP, ngay_cap_nhat DATETIME DEFAULT "
                    + "CURRENT_TIMESTAMP, FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id), FOREIGN KEY "
                    + "(id_nhan_vien_phu_trach) REFERENCES nhan_vien(id) )",
            "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_hoi_thoai_khoa_rieng' AND object_id = "
                    + "OBJECT_ID('hoi_thoai')) CREATE UNIQUE INDEX ux_hoi_thoai_khoa_rieng ON hoi_thoai (khoa_rieng) WHERE "
                    + "khoa_rieng IS NOT NULL",
            "IF OBJECT_ID('thanh_vien_hoi_thoai', 'U') IS NULL CREATE TABLE thanh_vien_hoi_thoai ( id INT "
                    + "IDENTITY(1,1) PRIMARY KEY, id_hoi_thoai INT NOT NULL, id_nhan_vien INT NOT NULL, id_tin_nhan_da_doc INT "
                    + "NOT NULL DEFAULT 0, ngay_tham_gia DATETIME DEFAULT CURRENT_TIMESTAMP, CONSTRAINT ux_thanh_vien_hoi_thoai "
                    + "UNIQUE (id_hoi_thoai, id_nhan_vien), FOREIGN KEY (id_hoi_thoai) REFERENCES hoi_thoai(id), FOREIGN KEY "
                    + "(id_nhan_vien) REFERENCES nhan_vien(id) )",
            "IF OBJECT_ID('tin_nhan', 'U') IS NULL CREATE TABLE tin_nhan ( id INT IDENTITY(1,1) PRIMARY KEY, "
                    + "id_hoi_thoai INT NOT NULL, loai_nguoi_gui TINYINT NOT NULL, id_nhan_vien INT NULL, ten_nguoi_gui "
                    + "NVARCHAR(100), noi_dung NVARCHAR(2000) NOT NULL, thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN "
                    + "KEY (id_hoi_thoai) REFERENCES hoi_thoai(id), FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id) )",
            "IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_tin_nhan_hoi_thoai' AND object_id = "
                    + "OBJECT_ID('tin_nhan')) CREATE INDEX ix_tin_nhan_hoi_thoai ON tin_nhan (id_hoi_thoai, id)",
            "INSERT INTO phuong_thuc_thanh_toan (id_hinh_thuc_thanh_toan, ma_phuong_thuc, ten_phuong_thuc, trang_thai) "
                    + "SELECT h.id, 'PT05', N'Tiền mặt + chuyển khoản', 1 FROM hinh_thuc_thanh_toan h WHERE h.ma_hinh_thuc = "
                    + "'HT01' AND NOT EXISTS (SELECT 1 FROM phuong_thuc_thanh_toan t WHERE t.ma_phuong_thuc = 'PT05')",
            "INSERT INTO ca_lam_viec (ma_ca, ten_ca, gio_bat_dau, gio_ket_thuc, trang_thai) SELECT v.ma, v.ten, v.bd, "
                    + "v.kt, 1 FROM (VALUES ('CA01', N'Ca sáng', CAST('07:00' AS TIME), CAST('12:00' AS TIME)), ('CA02', N'Ca "
                    + "chiều', CAST('12:00' AS TIME), CAST('17:00' AS TIME)), ('CA03', N'Ca tối', CAST('17:00' AS TIME), "
                    + "CAST('22:00' AS TIME)) ) AS v(ma, ten, bd, kt) WHERE NOT EXISTS (SELECT 1 FROM ca_lam_viec c WHERE "
                    + "c.ma_ca = v.ma)");
}

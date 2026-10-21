-- =================================================================
-- GIAI ĐOẠN 3: THÊM BẢNG / CỘT CHO CA LÀM VIỆC, LỊCH LÀM VIỆC, GIAO CA, BÁN TẠI QUẦY, CHAT
-- Chạy trên database đang có dữ liệu demo (KHÔNG xóa / sửa dữ liệu cũ). Chạy lại nhiều lần không lỗi.
-- (CSDL tạo mới bằng SD_17.sql bản này đã có sẵn các bảng; khởi động lại backend cũng tự tạo phần còn thiếu.)
--
-- Gồm: bảng ca_lam_viec, lich_lam_viec, giao_ca, hoi_thoai, thanh_vien_hoi_thoai, tin_nhan;
--      cột hoa_don.id_giao_ca, lich_su_thanh_toan.id_phuong_thuc_thanh_toan; phương thức PT05 "Tiền mặt + chuyển khoản";
--      3 ca mặc định (sáng / chiều / tối) và lịch làm mẫu cho tuần này + tuần sau.
-- =================================================================
USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON;
GO

-- 9.1 Ca làm việc, lịch làm việc, giao ca
-- Ca làm việc (định nghĩa ca: Ca sáng 07:00 - 12:00...). Giờ bắt đầu < giờ kết thúc (không có ca qua đêm).
IF OBJECT_ID('ca_lam_viec', 'U') IS NULL
CREATE TABLE ca_lam_viec (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_ca VARCHAR(20) UNIQUE NOT NULL,
    ten_ca NVARCHAR(100) NOT NULL,
    gio_bat_dau TIME NOT NULL,
    gio_ket_thuc TIME NOT NULL,
    trang_thai TINYINT NOT NULL DEFAULT 1,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP
);
GO

-- Lịch làm việc: nhân viên nào làm ca nào ngày nào. Một người không có 2 dòng cùng ca cùng ngày.
IF OBJECT_ID('lich_lam_viec', 'U') IS NULL
CREATE TABLE lich_lam_viec (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_nhan_vien INT NOT NULL,
    id_ca_lam_viec INT NOT NULL,
    ngay_lam DATE NOT NULL,
    ghi_chu NVARCHAR(255),
    trang_thai TINYINT NOT NULL DEFAULT 1,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_lich_lam_viec UNIQUE (id_nhan_vien, id_ca_lam_viec, ngay_lam),
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    FOREIGN KEY (id_ca_lam_viec) REFERENCES ca_lam_viec(id)
);
GO

-- Giao ca: một lần nhân viên nhận ca (tiền đầu ca) -> bán hàng -> kết ca (đếm tiền, bàn giao cho ca sau).
-- trang_thai: 1 = đang trong ca, 2 = đã giao ca. Mỗi nhân viên chỉ có 1 ca đang mở (chỉ mục ux_giao_ca_dang_mo).
IF OBJECT_ID('giao_ca', 'U') IS NULL
CREATE TABLE giao_ca (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_giao_ca VARCHAR(20) UNIQUE NOT NULL,
    id_nhan_vien INT NOT NULL,
    id_lich_lam_viec INT NULL,
    thoi_gian_nhan_ca DATETIME NOT NULL,
    tien_dau_ca DECIMAL(18,0) NOT NULL DEFAULT 0,
    ghi_chu_nhan_ca NVARCHAR(500),
    so_phut_di_muon INT NULL,
    thoi_gian_ket_ca DATETIME NULL,
    so_hoa_don INT NULL,
    doanh_thu DECIMAL(18,0) NULL,
    tien_mat_thu DECIMAL(18,0) NULL,
    chuyen_khoan_thu DECIMAL(18,0) NULL,
    tien_mat_he_thong DECIMAL(18,0) NULL,
    tien_mat_thuc_te DECIMAL(18,0) NULL,
    chenh_lech DECIMAL(18,0) NULL,
    tien_ban_giao DECIMAL(18,0) NULL,
    id_nhan_vien_nhan_ban_giao INT NULL,
    ghi_chu_ket_ca NVARCHAR(500),
    trang_thai TINYINT NOT NULL DEFAULT 1,
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    FOREIGN KEY (id_lich_lam_viec) REFERENCES lich_lam_viec(id),
    FOREIGN KEY (id_nhan_vien_nhan_ban_giao) REFERENCES nhan_vien(id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_giao_ca_dang_mo' AND object_id = OBJECT_ID('giao_ca'))
    CREATE UNIQUE INDEX ux_giao_ca_dang_mo ON giao_ca (id_nhan_vien) WHERE trang_thai = 1;
GO

-- 9.2 Hóa đơn tại quầy thuộc ca nào; lịch sử thanh toán ghi phương thức của từng lần trả tiền
IF COL_LENGTH('hoa_don', 'id_giao_ca') IS NULL
    ALTER TABLE hoa_don ADD id_giao_ca INT NULL CONSTRAINT fk_hoa_don_giao_ca FOREIGN KEY REFERENCES giao_ca(id);
GO

IF COL_LENGTH('lich_su_thanh_toan', 'id_phuong_thuc_thanh_toan') IS NULL
    ALTER TABLE lich_su_thanh_toan ADD id_phuong_thuc_thanh_toan INT NULL
        CONSTRAINT fk_lstt_phuong_thuc FOREIGN KEY REFERENCES phuong_thuc_thanh_toan(id);
GO

-- 9.3 Chat
-- Chat. loai: 1 = khách hỏi cửa hàng (hỗ trợ trực tuyến), 2 = nội bộ 1-1, 3 = nội bộ nhóm.
-- che_do (chỉ hội thoại hỗ trợ): 1 = trợ lý ảo đang trả lời, 2 = chờ nhân viên, 3 = nhân viên đang hỗ trợ, 4 = đã đóng.
-- ma_truy_cap: chuỗi bí mật cấp cho khách khi bắt đầu chat (khách không đăng nhập vẫn chỉ đọc được hội thoại của mình).
-- khoa_rieng: hội thoại 1-1 giữa 2 nhân viên "idNho-idLon" để không tạo trùng.
IF OBJECT_ID('hoi_thoai', 'U') IS NULL
CREATE TABLE hoi_thoai (
    id INT IDENTITY(1,1) PRIMARY KEY,
    loai TINYINT NOT NULL,
    tieu_de NVARCHAR(150),
    id_khach_hang INT NULL,
    ten_khach NVARCHAR(100),
    sdt_khach VARCHAR(15),
    ma_truy_cap VARCHAR(64),
    che_do TINYINT NOT NULL DEFAULT 1,
    id_nhan_vien_phu_trach INT NULL,
    khoa_rieng VARCHAR(50) NULL,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    FOREIGN KEY (id_nhan_vien_phu_trach) REFERENCES nhan_vien(id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_hoi_thoai_khoa_rieng' AND object_id = OBJECT_ID('hoi_thoai'))
    CREATE UNIQUE INDEX ux_hoi_thoai_khoa_rieng ON hoi_thoai (khoa_rieng) WHERE khoa_rieng IS NOT NULL;
GO

-- Thành viên của hội thoại nội bộ + mốc đã đọc (id tin nhắn cuối đã đọc) của từng nhân viên.
IF OBJECT_ID('thanh_vien_hoi_thoai', 'U') IS NULL
CREATE TABLE thanh_vien_hoi_thoai (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hoi_thoai INT NOT NULL,
    id_nhan_vien INT NOT NULL,
    id_tin_nhan_da_doc INT NOT NULL DEFAULT 0,
    ngay_tham_gia DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_thanh_vien_hoi_thoai UNIQUE (id_hoi_thoai, id_nhan_vien),
    FOREIGN KEY (id_hoi_thoai) REFERENCES hoi_thoai(id),
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id)
);
GO

-- loai_nguoi_gui: 1 = khách, 2 = nhân viên, 3 = trợ lý ảo, 4 = thông báo hệ thống
IF OBJECT_ID('tin_nhan', 'U') IS NULL
CREATE TABLE tin_nhan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hoi_thoai INT NOT NULL,
    loai_nguoi_gui TINYINT NOT NULL,
    id_nhan_vien INT NULL,
    ten_nguoi_gui NVARCHAR(100),
    noi_dung NVARCHAR(2000) NOT NULL,
    thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_hoi_thoai) REFERENCES hoi_thoai(id),
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_tin_nhan_hoi_thoai' AND object_id = OBJECT_ID('tin_nhan'))
    CREATE INDEX ix_tin_nhan_hoi_thoai ON tin_nhan (id_hoi_thoai, id);
GO

-- 9.4 Phương thức "Tiền mặt + chuyển khoản" cho đơn tại quầy trả kết hợp
INSERT INTO phuong_thuc_thanh_toan (id_hinh_thuc_thanh_toan, ma_phuong_thuc, ten_phuong_thuc, trang_thai)
SELECT h.id, 'PT05', N'Tiền mặt + chuyển khoản', 1
FROM hinh_thuc_thanh_toan h
WHERE h.ma_hinh_thuc = 'HT01'
  AND NOT EXISTS (SELECT 1 FROM phuong_thuc_thanh_toan t WHERE t.ma_phuong_thuc = 'PT05');
GO

-- 9.5 Ba ca mặc định
INSERT INTO ca_lam_viec (ma_ca, ten_ca, gio_bat_dau, gio_ket_thuc, trang_thai)
SELECT v.ma, v.ten, v.bd, v.kt, 1
FROM (VALUES
    ('CA01', N'Ca sáng', CAST('07:00' AS TIME), CAST('12:00' AS TIME)),
    ('CA02', N'Ca chiều', CAST('12:00' AS TIME), CAST('17:00' AS TIME)),
    ('CA03', N'Ca tối', CAST('17:00' AS TIME), CAST('22:00' AS TIME))
) AS v(ma, ten, bd, kt)
WHERE NOT EXISTS (SELECT 1 FROM ca_lam_viec c WHERE c.ma_ca = v.ma);
GO

-- 9.6 Lịch làm mẫu cho tuần này và tuần sau (thứ 2 -> chủ nhật), chỉ cho nhân viên đang hoạt động; đã có thì bỏ qua.
--     tuần bắt đầu từ thứ 2, không phụ thuộc SET DATEFIRST của máy.
DECLARE @thu2 DATE = DATEADD(DAY, -((DATEPART(WEEKDAY, GETDATE()) + @@DATEFIRST - 2) % 7), CAST(GETDATE() AS DATE));
INSERT INTO lich_lam_viec (id_nhan_vien, id_ca_lam_viec, ngay_lam, trang_thai)
SELECT nv.id, c.id, DATEADD(DAY, t.tuan + v.thu, @thu2), 1
FROM (VALUES
    ('NV03', 'CA01', 0), ('NV03', 'CA01', 1), ('NV03', 'CA01', 2), ('NV03', 'CA01', 3), ('NV03', 'CA01', 4), ('NV03', 'CA01', 5),
    ('NV06', 'CA01', 0), ('NV06', 'CA01', 1), ('NV06', 'CA01', 2), ('NV06', 'CA01', 3), ('NV06', 'CA01', 4), ('NV06', 'CA01', 5),
    ('NV04', 'CA02', 0), ('NV04', 'CA02', 1), ('NV04', 'CA02', 2), ('NV04', 'CA02', 3), ('NV04', 'CA02', 4), ('NV04', 'CA02', 5),
    ('NV07', 'CA02', 0), ('NV07', 'CA02', 1), ('NV07', 'CA02', 2), ('NV07', 'CA02', 3), ('NV07', 'CA02', 4), ('NV07', 'CA02', 5),
    ('NV05', 'CA03', 0), ('NV05', 'CA03', 1), ('NV05', 'CA03', 2), ('NV05', 'CA03', 3), ('NV05', 'CA03', 4), ('NV05', 'CA03', 5),
    ('NV08', 'CA03', 0), ('NV08', 'CA03', 1), ('NV08', 'CA03', 2), ('NV08', 'CA03', 3), ('NV08', 'CA03', 4), ('NV08', 'CA03', 5),
    ('NV09', 'CA01', 6), ('NV09', 'CA02', 6), ('NV03', 'CA03', 6)
) AS v(ma_nv, ma_ca, thu)
CROSS JOIN (VALUES (0), (7)) AS t(tuan)
JOIN nhan_vien nv ON nv.ma_nhan_vien = v.ma_nv AND nv.trang_thai = 1
JOIN ca_lam_viec c ON c.ma_ca = v.ma_ca
WHERE NOT EXISTS (SELECT 1 FROM lich_lam_viec l
                  WHERE l.id_nhan_vien = nv.id AND l.id_ca_lam_viec = c.id AND l.ngay_lam = DATEADD(DAY, t.tuan + v.thu, @thu2));
GO

-- Kiểm tra: phải ra đủ 6 bảng mới
SELECT name AS [Bảng mới] FROM sys.tables
WHERE name IN ('ca_lam_viec', 'lich_lam_viec', 'giao_ca', 'hoi_thoai', 'thanh_vien_hoi_thoai', 'tin_nhan')
ORDER BY name;
GO

-- =====================================================================
-- 5. DỮ LIỆU MẪU GIAO CA (ĐỂ KIỂM THỬ ĐỐI SOÁT TIỀN MẶT VÀ LỊCH SỬ CA)
-- =====================================================================
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
IF NOT EXISTS (SELECT 1 FROM giao_ca WHERE ma_giao_ca = 'GC001')
BEGIN
    INSERT INTO giao_ca (ma_giao_ca, id_nhan_vien, id_lich_lam_viec, thoi_gian_nhan_ca, tien_dau_ca, ghi_chu_nhan_ca, so_phut_di_muon, thoi_gian_ket_ca, so_hoa_don, doanh_thu, tien_mat_thu, chuyen_khoan_thu, tien_mat_he_thong, tien_mat_thuc_te, chenh_lech, tien_ban_giao, id_nhan_vien_nhan_ban_giao, ghi_chu_ket_ca, trang_thai)
    VALUES
    ('GC001', 1, NULL, '2026-10-18 07:00:00', 1000000, N'Nhận ca sáng đầy đủ', 0, '2026-10-18 12:05:00', 4, 3450000, 1850000, 1600000, 2850000, 2850000, 0, 1000000, 2, N'Ca sáng đông khách, kiểm tiền khớp 100%', 2),
    ('GC002', 2, NULL, '2026-10-18 12:05:00', 1000000, N'Nhận bàn giao từ Long', 0, '2026-10-18 17:10:00', 6, 5200000, 2700000, 2500000, 3700000, 3700000, 0, 1000000, 3, N'Kiểm tiền đầy đủ, bàn giao Bảo', 2),
    ('GC003', 3, 13, '2026-10-19 06:55:00', 1000000, N'Đúng giờ', 0, '2026-10-19 12:00:00', 3, 2800000, 1300000, 1500000, 2300000, 2300000, 0, 1000000, 2, N'Bàn giao cho Hà ca chiều', 2),
    ('GC004', 2, 29, '2026-10-19 12:18:00', 1000000, N'Đi muộn 18 phút do kẹt xe', 18, '2026-10-19 17:05:00', 5, 4600000, 2200000, 2400000, 3200000, 3150000, -50000, 1000000, 1, N'Thiếu 50.000đ do nhầm tiền thối đơn hàng, nhân viên tự bù', 2),
    ('GC005', 1, 15, '2026-10-20 07:00:00', 1000000, N'Nhận ca sáng', 0, '2026-10-20 12:00:00', 4, 3100000, 1600000, 1500000, 2600000, 2600000, 0, 1000000, 2, N'Hoàn thành ca làm việc sáng', 2);

    UPDATE TOP (2) hoa_don SET id_giao_ca = (SELECT id FROM giao_ca WHERE ma_giao_ca = 'GC005') WHERE loai_hoa_don = 1;
END
GO

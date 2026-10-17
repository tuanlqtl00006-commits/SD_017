-- =================================================================================
-- ERD XÂY DỰNG WEBSITE BÁN VỢT CẦU LÔNG FOOTSTYLE - FILE SQL HOÀN CHỈNH (FULL)
-- Hệ quản trị CSDL: Microsoft SQL Server
-- Bao gồm:
--   1. Tạo Database (shop_cau_long_footstyle) & toàn bộ 26 bảng chuẩn theo sơ đồ ERD
--      (cùng các cột mở rộng & bảng phụ trợ để Backend Spring Boot chạy không lỗi).
--   2. Dữ liệu mẫu đầy đủ cho TẤT CẢ các bảng:
--      - vai_tro, nhan_vien, dia_chi_nhan_vien
--      - khach_hang, dia_chi_khach_hang
--      - danh_muc, thuong_hieu, xuat_xu, chat_lieu, do_cung, diem_can_bang
--      - mau_sac, trong_luong, chu_vi
--      - san_pham, hinh_anh_san_pham, san_pham_chi_tiet
--      - phieu_giam_gia, phieu_giam_gia_khach_hang
--      - dot_giam_gia, dot_giam_gia_chi_tiet (có sẵn dữ liệu giao/trùng đợt giảm giá)
--      - hinh_thuc_thanh_toan, phuong_thuc_thanh_toan
--      - hoa_don, hoa_don_chi_tiet, lich_su_hoa_don, lich_su_thanh_toan
-- =================================================================================

USE master;
GO

IF DB_ID('shop_cau_long_footstyle') IS NOT NULL
BEGIN
    ALTER DATABASE shop_cau_long_footstyle SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE shop_cau_long_footstyle;
END
GO

CREATE DATABASE shop_cau_long_footstyle;
GO

ALTER AUTHORIZATION ON DATABASE::shop_cau_long_footstyle TO sa;
GO

USE shop_cau_long_footstyle;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- =================================================================================
-- PHẦN 1: TẠO BẢNG THEO SƠ ĐỒ ERD (26 BẢNG CHÍNH + BẢNG PHỤ TRỢ BACKEND)
-- =================================================================================

-- 1. Bảng vai_tro
CREATE TABLE vai_tro (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ten_vai_tro NVARCHAR(100) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 2. Bảng nhan_vien
CREATE TABLE nhan_vien (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_vai_tro INT NOT NULL,
    ma_nhan_vien VARCHAR(50) UNIQUE NOT NULL,
    ho_ten NVARCHAR(255) NOT NULL,
    gioi_tinh TINYINT,
    ngay_sinh DATE,
    sdt VARCHAR(15),
    email VARCHAR(255),
    mat_khau VARCHAR(255) NOT NULL,
    dia_chi NVARCHAR(500),
    can_cuoc_cong_dan VARCHAR(12),
    ngay_vao_lam DATE,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_vai_tro) REFERENCES vai_tro(id)
);
GO

-- Bảng phụ trợ địa chỉ & vị trí nhân viên (để tương thích Entity Backend)
CREATE TABLE dia_chi_nhan_vien (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_nhan_vien INT NOT NULL,
    tinh_thanh NVARCHAR(100),
    phuong_xa NVARCHAR(100),
    dia_chi_chi_tiet NVARCHAR(500) NOT NULL,
    la_mac_dinh BIT NOT NULL DEFAULT 0,
    trang_thai TINYINT DEFAULT 1,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id)
);
GO

CREATE TABLE vi_tri (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ten_vi_tri NVARCHAR(100) NOT NULL UNIQUE,
    trang_thai TINYINT DEFAULT 1
);
GO

CREATE TABLE nhan_vien_vi_tri (
    id_nhan_vien INT NOT NULL,
    id_vi_tri INT NOT NULL,
    PRIMARY KEY (id_nhan_vien, id_vi_tri),
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    FOREIGN KEY (id_vi_tri) REFERENCES vi_tri(id)
);
GO

-- 3. Bảng khach_hang
CREATE TABLE khach_hang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_khach_hang VARCHAR(50) UNIQUE NOT NULL,
    ho_ten NVARCHAR(255) NOT NULL,
    ngay_sinh DATE,
    gioi_tinh TINYINT,
    sdt VARCHAR(15),
    email VARCHAR(255),
    mat_khau VARCHAR(255),
    anh_dai_dien NVARCHAR(500),
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 4. Bảng dia_chi_khach_hang
CREATE TABLE dia_chi_khach_hang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang INT NOT NULL,
    ho_ten_nguoi_nhan NVARCHAR(255) NOT NULL,
    sdt_nguoi_nhan VARCHAR(15) NOT NULL,
    dia_chi_chi_tiet NVARCHAR(500) NOT NULL,
    phuong_xa NVARCHAR(100),
    quan_huyen NVARCHAR(100),
    tinh_thanh NVARCHAR(100),
    la_mac_dinh BIT DEFAULT 0,
    trang_thai TINYINT DEFAULT 1,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id)
);
GO

-- 5. Bảng phieu_giam_gia
CREATE TABLE phieu_giam_gia (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_phieu VARCHAR(50) UNIQUE NOT NULL,
    ten_phieu NVARCHAR(255) NOT NULL,
    doi_tuong_ap_dung TINYINT,
    loai_giam TINYINT,
    gia_tri_giam DECIMAL(18,0) NOT NULL,
    giam_toi_da DECIMAL(18,0),
    gia_tri_don_hang_toi_thieu DECIMAL(18,0),
    so_luong INT,
    so_luong_da_dung INT DEFAULT 0,
    gioi_han_moi_khach INT,
    ngay_bat_dau DATETIME NOT NULL,
    ngay_ket_thuc DATETIME NOT NULL,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 6. Bảng phieu_giam_gia_khach_hang
CREATE TABLE phieu_giam_gia_khach_hang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang INT NOT NULL,
    id_phieu_giam_gia INT NOT NULL,
    ngay_su_dung DATETIME,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id)
);
GO

-- 7. Bảng danh_muc
CREATE TABLE danh_muc (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_danh_muc VARCHAR(50) UNIQUE NOT NULL,
    ten_danh_muc NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 8. Bảng thuong_hieu
CREATE TABLE thuong_hieu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_thuong_hieu VARCHAR(50) UNIQUE NOT NULL,
    ten_thuong_hieu NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 9. Bảng xuat_xu
CREATE TABLE xuat_xu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_xuat_xu VARCHAR(50) UNIQUE NOT NULL,
    ten_xuat_xu NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 10. Bảng chat_lieu
CREATE TABLE chat_lieu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_chat_lieu VARCHAR(50) UNIQUE NOT NULL,
    ten_chat_lieu NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1,
    nhom_ap_dung VARCHAR(100) NULL
);
GO

-- 11. Bảng do_cung
CREATE TABLE do_cung (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_do_cung VARCHAR(50) UNIQUE NOT NULL,
    ten_do_cung NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 12. Bảng diem_can_bang
CREATE TABLE diem_can_bang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_diem_can_bang VARCHAR(50) UNIQUE NOT NULL,
    ten_diem_can_bang NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 13. Bảng mau_sac
CREATE TABLE mau_sac (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_mau_sac VARCHAR(50) UNIQUE NOT NULL,
    ten_mau_sac NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 14. Bảng trong_luong
CREATE TABLE trong_luong (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_trong_luong VARCHAR(50) UNIQUE NOT NULL,
    ten_trong_luong NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 15. Bảng chu_vi
CREATE TABLE chu_vi (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_chu_vi VARCHAR(50) UNIQUE NOT NULL,
    ten_chu_vi NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 16. Bảng san_pham
CREATE TABLE san_pham (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_danh_muc INT NOT NULL,
    id_thuong_hieu INT NOT NULL,
    id_xuat_xu INT NOT NULL,
    id_chat_lieu INT NOT NULL,
    id_do_cung INT NOT NULL,
    id_diem_can_bang INT NOT NULL,
    ma_san_pham VARCHAR(50) UNIQUE NOT NULL,
    ten_san_pham NVARCHAR(255) NOT NULL,
    mo_ta NVARCHAR(MAX),
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_danh_muc) REFERENCES danh_muc(id),
    FOREIGN KEY (id_thuong_hieu) REFERENCES thuong_hieu(id),
    FOREIGN KEY (id_xuat_xu) REFERENCES xuat_xu(id),
    FOREIGN KEY (id_chat_lieu) REFERENCES chat_lieu(id),
    FOREIGN KEY (id_do_cung) REFERENCES do_cung(id),
    FOREIGN KEY (id_diem_can_bang) REFERENCES diem_can_bang(id)
);
GO

-- 17. Bảng hinh_anh_san_pham
CREATE TABLE hinh_anh_san_pham (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham INT NOT NULL,
    duong_dan_hinh_anh VARCHAR(500) NOT NULL,
    la_anh_chinh BIT DEFAULT 0,
    trang_thai TINYINT DEFAULT 1,
    id_mau_sac INT NULL,
    FOREIGN KEY (id_san_pham) REFERENCES san_pham(id),
    CONSTRAINT fk_hasp_mau_sac FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id)
);
GO

-- 18. Bảng san_pham_chi_tiet
CREATE TABLE san_pham_chi_tiet (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_mau_sac INT NOT NULL,
    id_trong_luong INT NOT NULL,
    id_chu_vi INT NOT NULL,
    id_san_pham INT NOT NULL,
    ma_spct VARCHAR(50) UNIQUE NOT NULL,
    gia_ban DECIMAL(18,0) NOT NULL,
    so_luong_ton INT NOT NULL DEFAULT 0,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_mau_sac) REFERENCES mau_sac(id),
    FOREIGN KEY (id_trong_luong) REFERENCES trong_luong(id),
    FOREIGN KEY (id_chu_vi) REFERENCES chu_vi(id),
    FOREIGN KEY (id_san_pham) REFERENCES san_pham(id)
);
GO

-- 19. Bảng dot_giam_gia
CREATE TABLE dot_giam_gia (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_dot VARCHAR(50) UNIQUE NOT NULL,
    ten_dot NVARCHAR(255) NOT NULL,
    phan_tram_giam_dot INT NOT NULL,
    ngay_bat_dau DATETIME NOT NULL,
    ngay_ket_thuc DATETIME NOT NULL,
    mo_ta NVARCHAR(500) NULL,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 20. Bảng dot_giam_gia_chi_tiet
CREATE TABLE dot_giam_gia_chi_tiet (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham_chi_tiet INT NOT NULL,
    id_dot_giam_gia INT NOT NULL,
    phan_tram_giam_bien_the INT NOT NULL,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id),
    FOREIGN KEY (id_dot_giam_gia) REFERENCES dot_giam_gia(id)
);
GO

-- 21. Bảng hinh_thuc_thanh_toan
CREATE TABLE hinh_thuc_thanh_toan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_hinh_thuc VARCHAR(50) UNIQUE NOT NULL,
    ten_hinh_thuc NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- 22. Bảng phuong_thuc_thanh_toan
CREATE TABLE phuong_thuc_thanh_toan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hinh_thuc_thanh_toan INT NOT NULL,
    ma_phuong_thuc VARCHAR(50) UNIQUE NOT NULL,
    ten_phuong_thuc NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_hinh_thuc_thanh_toan) REFERENCES hinh_thuc_thanh_toan(id)
);
GO

-- 23. Bảng hoa_don
CREATE TABLE hoa_don (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_nhan_vien INT,
    id_khach_hang INT,
    id_phieu_giam_gia INT,
    id_phuong_thuc_thanh_toan INT,
    ma_hoa_don VARCHAR(50) UNIQUE NOT NULL,
    loai_hoa_don TINYINT,
    tong_tien DECIMAL(18,0) NOT NULL,
    so_tien_giam DECIMAL(18,0) DEFAULT 0,
    phi_van_chuyen DECIMAL(18,0) DEFAULT 0,
    thanh_tien DECIMAL(18,0) NOT NULL,
    don_vi_van_chuyen NVARCHAR(255),
    ho_ten_nguoi_nhan NVARCHAR(255),
    sdt_nguoi_nhan VARCHAR(15),
    dia_chi_giao_hang NVARCHAR(500),
    ghi_chu NVARCHAR(MAX),
    ngay_giao_du_kien DATETIME,
    ngay_giao_thuc_te DATETIME,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id),
    FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
);
GO

-- 24. Bảng hoa_don_chi_tiet
CREATE TABLE hoa_don_chi_tiet (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don INT NOT NULL,
    id_san_pham_chi_tiet INT NOT NULL,
    so_luong INT NOT NULL,
    don_gia DECIMAL(18,0) NOT NULL,
    thanh_tien DECIMAL(18,0) NOT NULL,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id)
);
GO

-- 25. Bảng lich_su_thanh_toan
CREATE TABLE lich_su_thanh_toan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don INT NOT NULL,
    so_tien DECIMAL(18,0) NOT NULL,
    ma_giao_dich VARCHAR(100),
    thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1,
    mo_ta NVARCHAR(MAX),
    FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id)
);
GO

-- 26. Bảng lich_su_hoa_don
CREATE TABLE lich_su_hoa_don (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don INT NOT NULL,
    nguoi_thao_tac NVARCHAR(255),
    hanh_dong NVARCHAR(255) NOT NULL,
    thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP,
    mo_ta NVARCHAR(MAX),
    FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id)
);
GO

-- Các chỉ mục ràng buộc duy nhất (Unique Index)
CREATE UNIQUE INDEX ux_nhan_vien_email ON nhan_vien (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX ux_nhan_vien_sdt ON nhan_vien (sdt) WHERE sdt IS NOT NULL;
CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (can_cuoc_cong_dan) WHERE can_cuoc_cong_dan IS NOT NULL;
CREATE UNIQUE INDEX ux_khach_hang_email ON khach_hang (email) WHERE email IS NOT NULL AND email <> '';
CREATE UNIQUE INDEX ux_khach_hang_sdt ON khach_hang (sdt) WHERE sdt IS NOT NULL AND sdt <> '';
CREATE UNIQUE INDEX ux_spct_san_pham_mau_tl_cv ON san_pham_chi_tiet (id_san_pham, id_mau_sac, id_trong_luong, id_chu_vi);
CREATE INDEX ix_hasp_san_pham_mau ON hinh_anh_san_pham (id_san_pham, id_mau_sac);
GO

-- =================================================================================
-- PHẦN 2: CHÈN DỮ LIỆU ĐẦY ĐỦ CHO TẤT CẢ CÁC BẢNG
-- =================================================================================

-- 1. Dữ liệu vai_tro
INSERT INTO vai_tro (ten_vai_tro, trang_thai) VALUES
(N'Quản lý', 1),
(N'Nhân viên', 1);
GO

-- 2. Dữ liệu nhan_vien (Mật khẩu mặc định: Footstyle@123)
INSERT INTO nhan_vien (id_vai_tro, ma_nhan_vien, ho_ten, gioi_tinh, ngay_sinh, sdt, email, mat_khau, dia_chi, can_cuoc_cong_dan, ngay_vao_lam, trang_thai) VALUES
(1, 'NV01', N'Nguyễn Hoàng Long', 1, '1992-03-14', '0912000001', 'long.nh@footstyle.vn', 'Footstyle@123', N'Số 15 ngõ 120 Trần Phú, Phường Hà Đông, Thành phố Hà Nội', '001092001001', '2022-01-10', 1),
(1, 'NV02', N'Trần Thu Hà',       0, '1994-07-22', '0912000002', 'ha.tt@footstyle.vn',   'Footstyle@123', N'Số 22 Nguyễn Trãi, Phường Thanh Xuân, Thành phố Hà Nội', '001194001002', '2022-03-01', 1),
(2, 'NV03', N'Lê Quốc Bảo',       1, '1999-11-05', '0912000003', 'bao.lq@footstyle.vn',  'Footstyle@123', N'Số 8 Lê Văn Lương, Phường Thanh Xuân, Thành phố Hà Nội', '001099001003', '2023-02-15', 1),
(2, 'NV04', N'Phạm Minh Anh',     0, '2000-01-18', '0912000004', 'anh.pm@footstyle.vn',  'Footstyle@123', N'Số 31 phố Kim Mã, Phường Ba Đình, Thành phố Hà Nội',     '001200001004', '2023-05-20', 1),
(2, 'NV05', N'Vũ Hoàng Nam',      1, '1998-09-30', '0912000005', 'nam.vh@footstyle.vn',  'Footstyle@123', N'Thị trấn Quốc Oai, Xã Quốc Oai, Thành phố Hà Nội',        '001098001005', '2023-08-01', 1),
(2, 'NV06', N'Đỗ Thùy Trang',     0, '2001-05-09', '0912000006', 'trang.dt@footstyle.vn','Footstyle@123', N'Số 45 Trần Thái Tông, Phường Cầu Giấy, Thành phố Hà Nội', '001301001006', '2024-01-08', 1),
(2, 'NV07', N'Bùi Đức Mạnh',      1, '1997-12-02', '0912000007', 'manh.bd@footstyle.vn', 'Footstyle@123', N'Số 3 ngõ 66 Nguyễn Văn Cừ, Phường Long Biên, Hà Nội',     '001097001007', '2023-10-12', 0),
(2, 'NV08', N'Ngô Linh Chi',      0, '2002-04-27', '0912000008', 'chi.nl@footstyle.vn',  'Footstyle@123', N'Số 19 Phạm Văn Đồng, Phường Bắc Từ Liêm, Hà Nội',         '001302001008', '2024-06-03', 1),
(2, 'NV09', N'Hoàng Tuấn Anh',    1, '1996-08-11', '0912000009', 'anh.ht@footstyle.vn',  'Footstyle@123', N'Số 7 Hoàng Quốc Việt, Phường Cầu Giấy, Hà Nội',           '001096001009', '2024-09-16', 1),
(2, 'NV10', N'Đặng Mai Hương',    0, '1995-02-25', '0912000010', 'huong.dm@footstyle.vn','Footstyle@123', N'Số 52 phố Minh Khai, Phường Hai Bà Trưng, Hà Nội',        '001195001010', '2022-11-21', 0);
GO

INSERT INTO dia_chi_nhan_vien (id_nhan_vien, tinh_thanh, phuong_xa, dia_chi_chi_tiet, la_mac_dinh, trang_thai)
SELECT id, N'Thành phố Hà Nội', N'Phường Cầu Giấy', dia_chi, 1, 1 FROM nhan_vien;
GO

-- 3. Dữ liệu khach_hang
INSERT INTO khach_hang (ma_khach_hang, ho_ten, ngay_sinh, gioi_tinh, sdt, email, mat_khau, trang_thai) VALUES
('KH01', N'Nguyễn Thị Lan',  '1995-03-12', 0, '0901000001', 'lan.nt@gmail.com',   '123456', 1),
('KH02', N'Trần Văn Hùng',   '1992-07-25', 1, '0901000002', 'hung.tv@gmail.com',  '123456', 1),
('KH03', N'Lê Minh Tuấn',    '1998-11-02', 1, '0901000003', 'tuan.lm@gmail.com',  '123456', 1),
('KH04', N'Phạm Thu Hằng',   '1996-05-20', 0, '0901000004', 'hang.pt@gmail.com',  '123456', 1),
('KH05', N'Hoàng Đức Anh',   '1994-09-08', 1, '0901000005', 'anh.hd@gmail.com',   '123456', 1),
('KH06', N'Vũ Khánh Linh',   '2000-01-15', 0, '0901000006', 'linh.vk@gmail.com',  '123456', 1),
('KH07', N'Đặng Quốc Khánh', '1997-04-18', 1, '0901000007', 'khanh.dq@gmail.com', '123456', 1),
('KH08', N'Bùi Phương Thảo', '2001-08-29', 0, '0901000008', 'thao.bp@gmail.com',  '123456', 1),
('KH09', N'Đỗ Quang Huy',    '1993-12-10', 1, '0901000009', 'huy.dq@gmail.com',   '123456', 1),
('KH10', N'Ngô Thanh Tâm',   '1999-06-05', 0, '0901000010', 'tam.nt@gmail.com',   '123456', 0);
GO

-- 4. Dữ liệu dia_chi_khach_hang
INSERT INTO dia_chi_khach_hang (id_khach_hang, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_chi_tiet, phuong_xa, quan_huyen, tinh_thanh, la_mac_dinh, trang_thai)
SELECT k.id, a.nguoi_nhan, a.sdt_nhan, a.dia_chi, a.phuong, a.quan, a.tinh, a.mac_dinh, 1
FROM (VALUES
    ('KH01', N'Nguyễn Thị Lan',  '0901000001', N'Số 12 Nguyễn Trãi',       N'Phường Thượng Đình', N'Quận Thanh Xuân',  N'Thành phố Hà Nội',        1),
    ('KH01', N'Nguyễn Thị Mai',  '0988111222', N'Số 88 Cầu Giấy',          N'Phường Quan Hoa',    N'Quận Cầu Giấy',    N'Thành phố Hà Nội',        0),
    ('KH02', N'Trần Văn Hùng',   '0901000002', N'Số 45 Lê Lợi',            N'Phường Bến Thành',   N'Quận 1',           N'Thành phố Hồ Chí Minh',   1),
    ('KH03', N'Lê Minh Tuấn',    '0901000003', N'Số 8 Trần Phú',           N'Phường Hải Châu 1',  N'Quận Hải Châu',    N'Thành phố Đà Nẵng',       1),
    ('KH04', N'Phạm Thu Hằng',   '0901000004', N'Số 25 Hoàng Quốc Việt',   N'Phường Nghĩa Đô',    N'Quận Cầu Giấy',    N'Thành phố Hà Nội',        1),
    ('KH05', N'Hoàng Đức Anh',   '0901000005', N'Số 102 Nguyễn Văn Cừ',    N'Phường Hồng Bàng',   N'Quận Hồng Bàng',   N'Thành phố Hải Phòng',     1),
    ('KH06', N'Vũ Khánh Linh',   '0901000006', N'Số 5 Lê Duẩn',            N'Phường Bến Thành',   N'Quận 1',           N'Thành phố Hồ Chí Minh',   1),
    ('KH07', N'Đặng Quốc Khánh', '0901000007', N'Số 19 Đại lộ Hòa Bình',   N'Phường Tân An',      N'Quận Ninh Kiều',   N'Thành phố Cần Thơ',       1),
    ('KH08', N'Bùi Phương Thảo', '0901000008', N'Số 33 Giải Phóng',        N'Phường Đồng Tâm',    N'Quận Hai Bà Trưng',N'Thành phố Hà Nội',        1),
    ('KH09', N'Đỗ Quang Huy',    '0901000009', N'Số 70 Nguyễn Văn Linh',   N'Phường Nam Dương',   N'Quận Hải Châu',    N'Thành phố Đà Nẵng',       1),
    ('KH10', N'Ngô Thanh Tâm',   '0901000010', N'Số 14 Lý Thường Kiệt',    N'Phường Phan Chu Trinh', N'Quận Hoàn Kiếm',N'Thành phố Hà Nội',        1)
) AS a(ma_kh, nguoi_nhan, sdt_nhan, dia_chi, phuong, quan, tinh, mac_dinh)
JOIN khach_hang k ON k.ma_khach_hang = a.ma_kh;
GO

-- 5. Dữ liệu 9 bảng thuộc tính sản phẩm
INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, trang_thai) VALUES
('DM01', N'Vợt cầu lông', 1),
('DM02', N'Vợt cầu lông người mới', 1),
('DM03', N'Quần áo thể thao', 1),
('DM04', N'Túi - balo', 1),
('DM05', N'Phụ kiện', 1),
('DM06', N'Quả cầu lông', 1);
GO

INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, trang_thai) VALUES
('TH01', N'Yonex', 1),
('TH02', N'Lining', 1),
('TH03', N'Victor', 1),
('TH04', N'Mizuno', 1),
('TH05', N'Kumpoo', 1);
GO

INSERT INTO xuat_xu (ma_xuat_xu, ten_xuat_xu, trang_thai) VALUES
('XX01', N'Nhật Bản', 1),
('XX02', N'Trung Quốc', 1),
('XX03', N'Đài Loan', 1),
('XX04', N'Việt Nam', 1);
GO

INSERT INTO chat_lieu (ma_chat_lieu, ten_chat_lieu, trang_thai, nhom_ap_dung) VALUES
('CL01', N'Carbon', 1, 'VOT'),
('CL02', N'Graphite', 1, 'VOT'),
('CL03', N'Nhôm', 1, 'VOT'),
('CL04', N'Titanium', 1, 'VOT'),
('CL05', N'Da PU', 1, 'TUI_BALO,PHU_KIEN'),
('CL06', N'Cotton', 1, 'QUAN_AO,PHU_KIEN'),
('CL07', N'Polyester', 1, 'QUAN_AO,TUI_BALO,PHU_KIEN'),
('CL08', N'Thun lạnh', 1, 'QUAN_AO'),
('CL09', N'Spandex', 1, 'QUAN_AO'),
('CL10', N'Nylon', 1, 'TUI_BALO,PHU_KIEN'),
('CL11', N'Vải dù', 1, 'TUI_BALO'),
('CL12', N'Cao su', 1, 'PHU_KIEN'),
('CL13', N'Lông ngỗng', 1, 'QUA_CAU'),
('CL14', N'Lông vịt', 1, 'QUA_CAU'),
('CL15', N'Nhựa', 1, 'QUA_CAU,PHU_KIEN');
GO

INSERT INTO do_cung (ma_do_cung, ten_do_cung, trang_thai) VALUES
('DC01', N'Mềm', 1),
('DC02', N'Trung bình', 1),
('DC03', N'Cứng', 1),
('DC04', N'Rất cứng', 1),
('DC00', N'Không áp dụng', 1);
GO

INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai) VALUES
('CB01', N'Nặng đầu', 1),
('CB02', N'Cân bằng', 1),
('CB03', N'Nhẹ đầu', 1),
('CB00', N'Không áp dụng', 1);
GO

INSERT INTO mau_sac (ma_mau_sac, ten_mau_sac, trang_thai) VALUES
('MS01', N'Đen', 1),
('MS02', N'Trắng', 1),
('MS03', N'Đỏ', 1),
('MS04', N'Xanh dương', 1),
('MS05', N'Vàng', 1);
GO

INSERT INTO trong_luong (ma_trong_luong, ten_trong_luong, trang_thai) VALUES
('TL01', N'2U (90-94g)', 1),
('TL02', N'3U (85-89g)', 1),
('TL03', N'4U (80-84g)', 1),
('TL04', N'5U (75-79g)', 1),
('TL00', N'Không áp dụng', 1);
GO

INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai) VALUES
('CV01', N'G4', 1),
('CV02', N'G5', 1),
('CV03', N'G6', 1),
('CV00', N'Không áp dụng', 1);
GO

-- 6. Dữ liệu san_pham
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, trang_thai)
SELECT dm.id, th.id, xx.id, cl.id, dc.id, cb.id, s.ma, s.ten, s.mo_ta, 1
FROM (VALUES
    ('SP01', N'Yonex Astrox 99 Pro',    'DM01', 'TH01', 'XX01', 'CL01', 'DC03', 'CB01', N'Vợt thiên công cao cấp, khung carbon Namd kết hợp trục siêu mảnh.'),
    ('SP02', N'Lining Axforce 80',      'DM01', 'TH02', 'XX02', 'CL01', 'DC04', 'CB01', N'Vợt chuyên công dành cho người chơi có lực cổ tay tốt.'),
    ('SP03', N'Victor Thruster K 9900', 'DM01', 'TH03', 'XX03', 'CL02', 'DC03', 'CB01', N'Vợt thiên công, nặng đầu, hỗ trợ những cú smash uy lực.'),
    ('SP04', N'Yonex Nanoflare 800 Pro','DM01', 'TH01', 'XX01', 'CL01', 'DC02', 'CB02', N'Vợt thiên tốc độ, khung vát mỏng giảm sức cản không khí.'),
    ('SP05', N'Mizuno Fortius 10 Quick','DM01', 'TH04', 'XX01', 'CL01', 'DC02', 'CB02', N'Vợt nhẹ, linh hoạt, phản tạt và điều cầu chuẩn xác.'),
    ('SP06', N'Kumpoo Power Control',   'DM02', 'TH05', 'XX03', 'CL02', 'DC02', 'CB02', N'Vợt công thủ toàn diện, trợ lực tốt, phù hợp người mới chơi.')
) AS s(ma, ten, dm, th, xx, cl, dc, cb, mo_ta)
JOIN danh_muc dm ON dm.ma_danh_muc = s.dm
JOIN thuong_hieu th ON th.ma_thuong_hieu = s.th
JOIN xuat_xu xx ON xx.ma_xuat_xu = s.xx
JOIN chat_lieu cl ON cl.ma_chat_lieu = s.cl
JOIN do_cung dc ON dc.ma_do_cung = s.dc
JOIN diem_can_bang cb ON cb.ma_diem_can_bang = s.cb;
GO

-- 7. Dữ liệu hinh_anh_san_pham (đầy đủ cho cả 6 sản phẩm theo màu sắc)
INSERT INTO hinh_anh_san_pham (id_san_pham, id_mau_sac, duong_dan_hinh_anh, la_anh_chinh, trang_thai)
SELECT p.id, ms.id, a.url, a.chinh, 1
FROM (VALUES
    ('SP01', 'MS01', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-yonex-astrox-99-pro-do-chinh-hang_1694735015.webp', 1),
    ('SP01', 'MS03', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-yonex-astrox-99-pro-do-chinh-hang_1694735015.webp', 1),
    ('SP01', 'MS05', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-yonex-astrox-99-pro-trang-chinh-hang_1694735210.webp', 1),
    ('SP02', 'MS01', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-lining-axforce-80-chinh-hang_1694735300.webp', 1),
    ('SP02', 'MS04', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-lining-axforce-80-chinh-hang_1694735300.webp', 1),
    ('SP03', 'MS01', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-victor-thruster-ryuga-ii-chinh-hang_1694735400.webp', 1),
    ('SP03', 'MS03', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-victor-thruster-ryuga-ii-chinh-hang_1694735400.webp', 1),
    ('SP04', 'MS01', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-yonex-nanoflare-800-pro-chinh-hang_1699235000.webp', 1),
    ('SP05', 'MS02', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-mizuno-fortius-10-quick-chinh-hang_1694735600.webp', 1),
    ('SP06', 'MS01', 'https://cdn.shopvnb.com/uploads/gallery/vot-cau-long-kumpoo-power-control-k520-pro_1694735700.webp', 1)
) AS a(ma_sp, ma_ms, url, chinh)
JOIN san_pham p ON p.ma_san_pham = a.ma_sp
JOIN mau_sac ms ON ms.ma_mau_sac = a.ma_ms;
GO

-- 8. Dữ liệu san_pham_chi_tiet (14 biến thể)
INSERT INTO san_pham_chi_tiet (id_san_pham, id_mau_sac, id_trong_luong, id_chu_vi, ma_spct, gia_ban, so_luong_ton, trang_thai)
SELECT p.id, ms.id, tl.id, cv.id, b.ma, b.gia, b.ton, 1
FROM (VALUES
    ('SP01-DEN-4U-G5',  'SP01', 'MS01', 'TL03', 'CV02', 4290000, 20),
    ('SP01-DEN-4U-G4',  'SP01', 'MS01', 'TL03', 'CV01', 4290000, 10),
    ('SP01-DEN-3U-G5',  'SP01', 'MS01', 'TL02', 'CV02', 4290000, 15),
    ('SP01-DO-4U-G5',   'SP01', 'MS03', 'TL03', 'CV02', 4290000,  8),
    ('SP01-DO-3U-G5',   'SP01', 'MS03', 'TL02', 'CV02', 4290000,  5),
    ('SP01-VANG-4U-G6', 'SP01', 'MS05', 'TL03', 'CV03', 4290000, 12),
    ('SP02-DEN-4U-G5',  'SP02', 'MS01', 'TL03', 'CV02', 2850000, 14),
    ('SP02-DEN-4U-G4',  'SP02', 'MS01', 'TL03', 'CV01', 2850000, 11),
    ('SP02-XDG-3U-G5',  'SP02', 'MS04', 'TL02', 'CV02', 2850000,  6),
    ('SP03-DEN-4U-G5',  'SP03', 'MS01', 'TL03', 'CV02', 3150000, 10),
    ('SP03-DO-3U-G5',   'SP03', 'MS03', 'TL02', 'CV02', 3150000,  7),
    ('SP04-DEN-4U-G5',  'SP04', 'MS01', 'TL03', 'CV02', 2790000, 12),
    ('SP05-TRG-4U-G5',  'SP05', 'MS02', 'TL03', 'CV02', 2350000,  9),
    ('SP06-DEN-4U-G5',  'SP06', 'MS01', 'TL03', 'CV02', 1250000, 18)
) AS b(ma, sp, ms, tl, cv, gia, ton)
JOIN san_pham p ON p.ma_san_pham = b.sp
JOIN mau_sac ms ON ms.ma_mau_sac = b.ms
JOIN trong_luong tl ON tl.ma_trong_luong = b.tl
JOIN chu_vi cv ON cv.ma_chu_vi = b.cv;
GO

-- 9. Dữ liệu phieu_giam_gia & phieu_giam_gia_khach_hang
INSERT INTO phieu_giam_gia (ma_phieu, ten_phieu, doi_tuong_ap_dung, loai_giam, gia_tri_giam, giam_toi_da,
                            gia_tri_don_hang_toi_thieu, so_luong, so_luong_da_dung, gioi_han_moi_khach,
                            ngay_bat_dau, ngay_ket_thuc, trang_thai)
SELECT p.ma, p.ten, p.doi_tuong, p.loai, p.gia_tri, p.toi_da, p.don_min, p.so_luong, 0, p.gioi_han,
       CAST(DATEADD(DAY, p.bd, CAST(GETDATE() AS DATE)) AS DATETIME),
       DATEADD(SECOND, 86399, CAST(DATEADD(DAY, p.kt, CAST(GETDATE() AS DATE)) AS DATETIME)),
       p.trang_thai
FROM (VALUES
    ('VCH7QX2YN', N'Giảm 10% vợt Yonex',                 1, 1, 10,    200000, 1500000, 100, 2, -10,  20, 1),
    ('VCHFS500K', N'Hỗ trợ phí vận chuyển đơn từ 500K',  1, 2, 30000, NULL,    500000, 300, 1,  -5,  25, 1),
    ('VCH8DK3SO', N'Giảm sốc phụ kiện xả kho',           1, 1, 60,    500000, 2000000,  20, NULL,-45,-29, 0),
    ('VCHTT50KH', N'Tri ân khách hàng thân thiết',       2, 2, 50000, NULL,    300000,   3, 1,  -2,  60, 1),
    ('VCHSN26FS', N'Sinh nhật FootStyle',                1, 1, 15,    300000, 1000000, 200, NULL, 7,  14, 1),
    ('VCHPK20QC', N'Giảm 20% phụ kiện, quả cầu',         1, 1, 20,    100000,  200000, 150, 3, -60, -20, 1),
    ('VCHDOI100', N'Tặng khách mua vợt đôi',             2, 2, 100000,NULL,   3000000,   2, 1, -15,  45, 0),
    ('VCHNEWMB1', N'Chào thành viên mới',                2, 2, 40000, NULL,    250000,   1, 1, -30,  90, 1),
    ('VCHHE12XN', N'Hè xanh - giảm 12%',                 1, 1, 12,    150000,  800000, 120, NULL,-90,-61, 1),
    ('VCHG30K01', N'Giảm 30K cho đơn từ 600K',           1, 2, 30000, NULL,    600000,  80, 1,   3,  30, 1),
    ('VCHBF25SM', N'Black Friday sớm',                   1, 1, 25,    400000, 2500000,  60, 1,  40,  45, 0)
) AS p(ma, ten, doi_tuong, loai, gia_tri, toi_da, don_min, so_luong, gioi_han, bd, kt, trang_thai);
GO

INSERT INTO phieu_giam_gia_khach_hang (id_khach_hang, id_phieu_giam_gia, trang_thai)
SELECT k.id, p.id, 1
FROM (VALUES
    ('VCHTT50KH', 'KH01'),
    ('VCHTT50KH', 'KH02'),
    ('VCHTT50KH', 'KH03'),
    ('VCHDOI100', 'KH04'),
    ('VCHDOI100', 'KH05'),
    ('VCHNEWMB1', 'KH06')
) AS t(ma_phieu, ma_khach_hang)
JOIN phieu_giam_gia p ON p.ma_phieu = t.ma_phieu
JOIN khach_hang k ON k.ma_khach_hang = t.ma_khach_hang;
GO

-- 10. Dữ liệu dot_giam_gia & dot_giam_gia_chi_tiet
-- Đặc biệt: DGG01 (10%, -5 ngày -> +5 ngày) và DGG02 (15%, -2 ngày -> +15 ngày) giao thời gian nhau trên SP01-DEN-4U-G5
-- đúng kịch bản trùng đợt giảm giá của giảng viên.
INSERT INTO dot_giam_gia (ma_dot, ten_dot, phan_tram_giam_dot, ngay_bat_dau, ngay_ket_thuc, mo_ta, ngay_tao, trang_thai)
SELECT d.ma, d.ten, d.pt,
       CAST(DATEADD(DAY, d.bd, CAST(GETDATE() AS DATE)) AS DATETIME),
       DATEADD(SECOND, 86399, CAST(DATEADD(DAY, d.kt, CAST(GETDATE() AS DATE)) AS DATETIME)),
       d.mo_ta,
       CAST(DATEADD(DAY, d.bd - 3, CAST(GETDATE() AS DATE)) AS DATETIME),
       d.trang_thai
FROM (VALUES
    ('DGG01', N'Sale vợt Yonex & Lining đợt 1',  10,  -5,   5, N'Giảm 10% cho biến thể chọn lọc SP01 và SP02', 1),
    ('DGG02', N'Siêu hội cầu lông tháng 10',     15,  -2,  15, N'Giảm 15% giao thời gian với DGG01 để kiểm tra trùng đợt', 1),
    ('DGG03', N'Black Friday vợt Lining',        20,  30,  37, N'Khuyến mãi lớn cuối năm cho dòng Lining', 1),
    ('DGG04', N'Tuần lễ vàng Victor',            12,  -3,   4, N'Ưu đãi dòng vợt Victor Thruster', 1),
    ('DGG05', N'Mừng khai trương FootStyle',      8, -120, -90, N'Đợt giảm giá mừng khai trương cửa hàng', 1),
    ('DGG06', N'Ưu đãi người mới chơi',           5,  -15,  45, N'Tạm ngừng áp dụng', 0),
    ('DGG07', N'Hè sôi động',                    18,  -75, -50, N'Chương trình khuyến mãi mùa hè', 1),
    ('DGG08', N'Tết rộn ràng - sale lớn',        25,   80,  95, N'Chương trình khuyến mãi dịp Tết', 1)
) AS d(ma, ten, pt, bd, kt, mo_ta, trang_thai);
GO

INSERT INTO dot_giam_gia_chi_tiet (id_san_pham_chi_tiet, id_dot_giam_gia, phan_tram_giam_bien_the, trang_thai)
SELECT c.id, d.id, d.phan_tram_giam_dot, 1
FROM (VALUES
    -- DGG01 (10%): SPCT1 (SP01-DEN-4U-G5) và SPCT5 (SP02-DEN-4U-G4)
    ('DGG01', 'SP01-DEN-4U-G5'), ('DGG01', 'SP02-DEN-4U-G4'),
    -- DGG02 (15%): SPCT1 (SP01-DEN-4U-G5), SPCT3 (SP01-DEN-3U-G5), SPCT4 (SP02-DEN-4U-G5)
    ('DGG02', 'SP01-DEN-4U-G5'), ('DGG02', 'SP01-DEN-3U-G5'), ('DGG02', 'SP02-DEN-4U-G5'),
    -- Các đợt khác
    ('DGG03', 'SP02-DEN-4U-G5'), ('DGG03', 'SP02-DEN-4U-G4'), ('DGG03', 'SP02-XDG-3U-G5'),
    ('DGG04', 'SP03-DEN-4U-G5'), ('DGG04', 'SP03-DO-3U-G5'),
    ('DGG05', 'SP06-DEN-4U-G5'), ('DGG05', 'SP01-DEN-4U-G5'),
    ('DGG06', 'SP06-DEN-4U-G5'),
    ('DGG07', 'SP02-DEN-4U-G5'), ('DGG07', 'SP04-DEN-4U-G5'),
    ('DGG08', 'SP01-DEN-4U-G5'), ('DGG08', 'SP04-DEN-4U-G5'), ('DGG08', 'SP05-TRG-4U-G5')
) AS v(ma_dot, ma_spct)
JOIN dot_giam_gia d ON d.ma_dot = v.ma_dot
JOIN san_pham_chi_tiet c ON c.ma_spct = v.ma_spct;
GO

-- 11. Dữ liệu hinh_thuc_thanh_toan & phuong_thuc_thanh_toan
INSERT INTO hinh_thuc_thanh_toan (ma_hinh_thuc, ten_hinh_thuc, trang_thai) VALUES
('HT01', N'Tiền mặt', 1),
('HT02', N'Chuyển khoản', 1);
GO

INSERT INTO phuong_thuc_thanh_toan (id_hinh_thuc_thanh_toan, ma_phuong_thuc, ten_phuong_thuc, trang_thai)
SELECT h.id, v.ma, v.ten, 1
FROM (VALUES
    ('PT01', 'HT01', N'Tiền mặt'),
    ('PT02', 'HT02', N'Chuyển khoản ngân hàng'),
    ('PT03', 'HT01', N'Thanh toán khi nhận hàng (COD)'),
    ('PT04', 'HT02', N'Ví điện tử VNPay')
) AS v(ma, ht, ten)
JOIN hinh_thuc_thanh_toan h ON h.ma_hinh_thuc = v.ht;
GO

-- 12. Dữ liệu hoa_don, hoa_don_chi_tiet, lich_su_hoa_don, lich_su_thanh_toan
IF OBJECT_ID('tempdb..#hd') IS NOT NULL DROP TABLE #hd;
CREATE TABLE #hd (
    ma VARCHAR(50), ma_nv VARCHAR(50), ma_kh VARCHAR(50), ma_phieu VARCHAR(50), ma_pt VARCHAR(50),
    loai TINYINT, phi_ship DECIMAL(18,0), don_vi NVARCHAR(255), nguoi_nhan NVARCHAR(255), sdt VARCHAR(15),
    dia_chi NVARCHAR(500), ghi_chu NVARCHAR(500), phut INT, trang_thai TINYINT, buoc_huy TINYINT, ly_do_huy NVARCHAR(500)
);
INSERT INTO #hd VALUES
    ('HD01', 'NV03', NULL,   NULL,        'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -120, 6, NULL, NULL),
    ('HD02', 'NV04', 'KH02', 'VCH7QX2YN', 'PT02', 1, 0,     NULL, NULL, NULL, NULL, NULL, -1500, 6, NULL, NULL),
    ('HD03', 'NV03', 'KH01', 'VCHTT50KH', 'PT03', 2, 30000, N'Giao Hàng Nhanh', N'Nguyễn Thị Lan', '0901000001',
        N'Số 12 Nguyễn Trãi, Phường Thượng Đình, Quận Thanh Xuân, Hà Nội', N'Gọi trước khi giao', -60, 1, NULL, NULL),
    ('HD04', 'NV06', 'KH03', NULL,        'PT04', 2, 35000, N'Giao Hàng Tiết Kiệm', N'Lê Minh Tuấn', '0901000003',
        N'Số 8 Trần Phú, Phường Hải Châu 1, Quận Hải Châu, Đà Nẵng', NULL, -1440, 2, NULL, NULL),
    ('HD05', 'NV09', 'KH05', 'VCHFS500K', 'PT03', 2, 30000, N'Giao Hàng Nhanh', N'Hoàng Đức Anh', '0901000005',
        N'Số 102 Nguyễn Văn Cừ, Phường Hồng Bàng, Hải Phòng', N'Giao giờ hành chính', -2880, 3, NULL, NULL),
    ('HD06', 'NV03', 'KH04', 'VCH7QX2YN', 'PT02', 2, 40000, N'Viettel Post', N'Phạm Thu Hằng', '0901000004',
        N'Số 25 Hoàng Quốc Việt, Phường Nghĩa Đô, Quận Cầu Giấy, Hà Nội', NULL, -4320, 4, NULL, NULL),
    ('HD07', 'NV04', 'KH06', NULL,        'PT03', 2, 30000, N'Giao Hàng Tiết Kiệm', N'Vũ Khánh Linh', '0901000006',
        N'Số 5 Lê Duẩn, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh', NULL, -5760, 5, NULL, NULL),
    ('HD08', 'NV06', 'KH02', 'VCH7QX2YN', 'PT03', 2, 0,     N'Giao Hàng Nhanh', N'Trần Văn Hùng', '0901000002',
        N'Số 45 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh', N'Miễn phí vận chuyển', -8640, 6, NULL, NULL),
    ('HD09', 'NV09', 'KH03', NULL,        'PT03', 2, 30000, N'Giao Hàng Nhanh', N'Lê Minh Tuấn', '0901000003',
        N'Số 8 Trần Phú, Phường Hải Châu 1, Quận Hải Châu, Đà Nẵng', NULL, -2700, 0, 2, N'Khách đổi ý, muốn đổi sang mẫu vợt khác'),
    ('HD10', 'NV03', 'KH05', NULL,        'PT03', 2, 30000, N'Viettel Post', N'Hoàng Đức Anh', '0901000005',
        N'Số 102 Nguyễn Văn Cừ, Phường Hồng Bàng, Hải Phòng', NULL, -7200, 0, 1, N'Không liên lạc được với khách hàng'),
    ('HD11', 'NV01', NULL,   'VCH7QX2YN', 'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -11520, 6, NULL, NULL),
    ('HD12', 'NV04', 'KH01', 'VCHPK20QC', 'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -43200, 6, NULL, NULL),
    ('HD13', 'NV06', 'KH04', NULL,        'PT02', 2, 0,     N'Giao Hàng Nhanh', N'Phạm Thu Hằng', '0901000004',
        N'Số 25 Hoàng Quốc Việt, Phường Nghĩa Đô, Quận Cầu Giấy, Hà Nội', N'Mua 2 cây tặng bạn', -50400, 6, NULL, NULL),
    ('HD14', 'NV09', 'KH06', 'VCHHE12XN', 'PT02', 1, 0,     NULL, NULL, NULL, NULL, NULL, -93600, 6, NULL, NULL),
    ('HD15', 'NV03', NULL,   NULL,        'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -144000, 6, NULL, NULL),
    ('HD16', 'NV01', NULL,   NULL,        'PT01', 1, 0,     NULL, NULL, NULL, NULL, N'Khách đang chờ tại quầy', -10, 1, NULL, NULL),
    ('HD17', 'NV04', 'KH02', 'VCHFS500K', 'PT04', 2, 0,     N'Giao Hàng Nhanh', N'Trần Văn Hùng', '0901000002',
        N'Số 45 Lê Lợi, Phường Bến Thành, Quận 1, TP. Hồ Chí Minh', NULL, -180, 1, NULL, NULL),
    ('HD18', 'NV09', 'KH03', NULL,        'PT03', 2, 30000, N'Giao Hàng Tiết Kiệm', N'Lê Minh Tuấn', '0901000003',
        N'Số 8 Trần Phú, Phường Hải Châu 1, Quận Hải Châu, Đà Nẵng', NULL, -21600, 6, NULL, NULL);

INSERT INTO hoa_don (id_nhan_vien, id_khach_hang, id_phieu_giam_gia, id_phuong_thuc_thanh_toan, ma_hoa_don, loai_hoa_don,
                     tong_tien, so_tien_giam, phi_van_chuyen, thanh_tien, don_vi_van_chuyen, ho_ten_nguoi_nhan, sdt_nguoi_nhan,
                     dia_chi_giao_hang, ghi_chu, ngay_giao_du_kien, ngay_giao_thuc_te, ngay_tao, ngay_cap_nhat, trang_thai)
SELECT nv.id, kh.id, p.id, pt.id, h.ma, h.loai,
       0, 0, h.phi_ship, 0, h.don_vi, h.nguoi_nhan, h.sdt, h.dia_chi, h.ghi_chu,
       CASE WHEN h.loai = 2 THEN DATEADD(DAY, 4, DATEADD(MINUTE, h.phut, GETDATE())) END,
       CASE WHEN h.loai = 2 AND h.trang_thai IN (5, 6) THEN DATEADD(MINUTE, h.phut + 4320, GETDATE()) END,
       DATEADD(MINUTE, h.phut, GETDATE()),
       DATEADD(MINUTE, h.phut, GETDATE()),
       h.trang_thai
FROM #hd h
JOIN nhan_vien nv ON nv.ma_nhan_vien = h.ma_nv
JOIN phuong_thuc_thanh_toan pt ON pt.ma_phuong_thuc = h.ma_pt
LEFT JOIN khach_hang kh ON kh.ma_khach_hang = h.ma_kh
LEFT JOIN phieu_giam_gia p ON p.ma_phieu = h.ma_phieu;

INSERT INTO hoa_don_chi_tiet (id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia, thanh_tien, trang_thai)
SELECT hd.id, c.id, v.sl,
       ROUND(c.gia_ban * (100 - v.giam) / 100.0, 0),
       ROUND(c.gia_ban * (100 - v.giam) / 100.0, 0) * v.sl,
       1
FROM (VALUES
    ('HD01', 'SP06-DEN-4U-G5', 1, 0),
    ('HD02', 'SP01-DEN-4U-G5', 1, 15),
    ('HD03', 'SP02-XDG-3U-G5', 1, 0),
    ('HD03', 'SP06-DEN-4U-G5', 1, 0),
    ('HD04', 'SP03-DEN-4U-G5', 1, 12),
    ('HD05', 'SP04-DEN-4U-G5', 1, 0),
    ('HD05', 'SP06-DEN-4U-G5', 2, 0),
    ('HD06', 'SP01-DO-4U-G5',  1, 10),
    ('HD07', 'SP05-TRG-4U-G5', 1, 0),
    ('HD08', 'SP02-DEN-4U-G5', 1, 15),
    ('HD08', 'SP03-DO-3U-G5',  1, 0),
    ('HD09', 'SP06-DEN-4U-G5', 1, 0),
    ('HD10', 'SP05-TRG-4U-G5', 1, 0),
    ('HD11', 'SP03-DEN-4U-G5', 1, 0),
    ('HD11', 'SP06-DEN-4U-G5', 1, 0),
    ('HD12', 'SP04-DEN-4U-G5', 1, 15),
    ('HD12', 'SP05-TRG-4U-G5', 1, 15),
    ('HD13', 'SP02-XDG-3U-G5', 2, 0),
    ('HD14', 'SP02-DEN-4U-G5', 1, 18),
    ('HD15', 'SP06-DEN-4U-G5', 2, 8),
    ('HD15', 'SP01-DEN-4U-G5', 1, 8),
    ('HD16', 'SP01-DEN-3U-G5', 1, 15),
    ('HD17', 'SP01-DEN-4U-G5', 1, 15),
    ('HD17', 'SP03-DO-3U-G5',  1, 12),
    ('HD18', 'SP06-DEN-4U-G5', 1, 0)
) AS v(ma_hd, ma_spct, sl, giam)
JOIN hoa_don hd ON hd.ma_hoa_don = v.ma_hd
JOIN san_pham_chi_tiet c ON c.ma_spct = v.ma_spct;

;WITH tong AS (
    SELECT ct.id_hoa_don, SUM(ct.thanh_tien) AS tong_tien
    FROM hoa_don_chi_tiet ct
    GROUP BY ct.id_hoa_don
), tinh AS (
    SELECT hd.id, t.tong_tien,
           CASE
               WHEN p.id IS NULL OR t.tong_tien < ISNULL(p.gia_tri_don_hang_toi_thieu, 0) THEN 0
               WHEN p.loai_giam = 1 THEN
                   CASE WHEN p.giam_toi_da IS NOT NULL AND ROUND(t.tong_tien * p.gia_tri_giam / 100.0, 0) > p.giam_toi_da
                        THEN p.giam_toi_da
                        ELSE ROUND(t.tong_tien * p.gia_tri_giam / 100.0, 0) END
               ELSE CASE WHEN p.gia_tri_giam > t.tong_tien THEN t.tong_tien ELSE p.gia_tri_giam END
           END AS so_tien_giam
    FROM hoa_don hd
    JOIN #hd h ON h.ma = hd.ma_hoa_don
    JOIN tong t ON t.id_hoa_don = hd.id
    LEFT JOIN phieu_giam_gia p ON p.id = hd.id_phieu_giam_gia
)
UPDATE hd
SET tong_tien = tinh.tong_tien,
    so_tien_giam = tinh.so_tien_giam,
    thanh_tien = tinh.tong_tien - tinh.so_tien_giam + ISNULL(hd.phi_van_chuyen, 0)
FROM hoa_don hd
JOIN tinh ON tinh.id = hd.id;

IF OBJECT_ID('tempdb..#buoc') IS NOT NULL DROP TABLE #buoc;
CREATE TABLE #buoc (code TINYINT, hanh_dong NVARCHAR(100), phut INT, mo_ta NVARCHAR(255));
INSERT INTO #buoc VALUES
    (1, N'Tạo đơn hàng',   0,    N'Khách đặt hàng online'),
    (2, N'Đã xác nhận',    30,   N'Nhân viên đã gọi xác nhận đơn với khách'),
    (3, N'Chờ giao hàng',  180,  N'Đã đóng gói, chờ đơn vị vận chuyển đến lấy'),
    (4, N'Đang giao hàng', 1440, N'Đã bàn giao cho đơn vị vận chuyển'),
    (5, N'Đã giao hàng',   4320, N'Giao hàng thành công'),
    (6, N'Đã hoàn thành',  4380, N'Đơn hàng hoàn thành');

INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_thao_tac, hanh_dong, thoi_gian, mo_ta)
SELECT hd.id,
       CASE WHEN b.code = 1 THEN N'Khách hàng' ELSE nv.ho_ten END,
       b.hanh_dong,
       DATEADD(MINUTE, b.phut, hd.ngay_tao),
       CASE WHEN b.code = 4 AND hd.don_vi_van_chuyen IS NOT NULL THEN N'Đã bàn giao cho ' + hd.don_vi_van_chuyen ELSE b.mo_ta END
FROM hoa_don hd
JOIN #hd h ON h.ma = hd.ma_hoa_don
LEFT JOIN nhan_vien nv ON nv.id = hd.id_nhan_vien
JOIN #buoc b ON b.code <= CASE WHEN h.trang_thai = 0 THEN h.buoc_huy ELSE h.trang_thai END
WHERE h.loai = 2;

INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_thao_tac, hanh_dong, thoi_gian, mo_ta)
SELECT hd.id, nv.ho_ten, N'Đã hủy', DATEADD(MINUTE, b.phut + 60, hd.ngay_tao), h.ly_do_huy
FROM hoa_don hd
JOIN #hd h ON h.ma = hd.ma_hoa_don
LEFT JOIN nhan_vien nv ON nv.id = hd.id_nhan_vien
JOIN #buoc b ON b.code = h.buoc_huy
WHERE h.trang_thai = 0;

INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_thao_tac, hanh_dong, thoi_gian, mo_ta)
SELECT hd.id, nv.ho_ten, s.hanh_dong, DATEADD(MINUTE, s.phut, hd.ngay_tao), s.mo_ta
FROM hoa_don hd
JOIN #hd h ON h.ma = hd.ma_hoa_don
LEFT JOIN nhan_vien nv ON nv.id = hd.id_nhan_vien
JOIN (VALUES
    (1, N'Tạo đơn hàng',  0, N'Tạo hóa đơn tại quầy'),
    (6, N'Đã hoàn thành', 3, N'Khách đã thanh toán, nhận hàng tại cửa hàng')
) AS s(code, hanh_dong, phut, mo_ta) ON s.code <= h.trang_thai
WHERE h.loai = 1;

INSERT INTO lich_su_thanh_toan (id_hoa_don, so_tien, ma_giao_dich, thoi_gian, trang_thai, mo_ta)
SELECT hd.id, hd.thanh_tien,
       CASE h.ma_pt WHEN 'PT02' THEN 'CK' + REPLACE(h.ma, 'HD', '') + '01' WHEN 'PT04' THEN 'VNP' + REPLACE(h.ma, 'HD', '') + '88' END,
       CASE
           WHEN h.loai = 1 THEN DATEADD(MINUTE, 3, hd.ngay_tao)
           WHEN h.ma_pt IN ('PT02', 'PT04') THEN DATEADD(MINUTE, 5, hd.ngay_tao)
           ELSE DATEADD(MINUTE, 4380, hd.ngay_tao)
       END,
       1,
       CASE
           WHEN h.loai = 1 THEN N'Thanh toán đơn tại cửa hàng'
           WHEN h.ma_pt IN ('PT02', 'PT04') THEN N'Khách thanh toán trước khi giao hàng'
           ELSE N'Thu tiền khi giao hàng (COD)'
       END
FROM hoa_don hd
JOIN #hd h ON h.ma = hd.ma_hoa_don
WHERE (h.loai = 1 AND h.trang_thai = 6)
   OR (h.loai = 2 AND h.ma_pt IN ('PT02', 'PT04') AND h.trang_thai <> 0)
   OR (h.loai = 2 AND h.ma_pt = 'PT03' AND h.trang_thai = 6);

UPDATE p
SET so_luong_da_dung = (SELECT COUNT(*) FROM hoa_don x WHERE x.id_phieu_giam_gia = p.id AND x.trang_thai <> 0)
FROM phieu_giam_gia p
WHERE p.ma_phieu IN (SELECT ma_phieu FROM #hd WHERE ma_phieu IS NOT NULL);

UPDATE l
SET ngay_su_dung = hd.ngay_tao
FROM phieu_giam_gia_khach_hang l
JOIN hoa_don hd ON hd.id_phieu_giam_gia = l.id_phieu_giam_gia AND hd.id_khach_hang = l.id_khach_hang
JOIN #hd h ON h.ma = hd.ma_hoa_don
WHERE l.ngay_su_dung IS NULL AND hd.trang_thai <> 0;

DROP TABLE #buoc;
DROP TABLE #hd;
GO

-- Hóa đơn mẫu HD19 "Giao hàng thất bại": đơn COD của KH05, giao qua Giao Hàng Tiết Kiệm, khách không nghe máy
IF NOT EXISTS (SELECT 1 FROM hoa_don WHERE ma_hoa_don = 'HD19')
BEGIN
    DECLARE @tao DATETIME = DATEADD(MINUTE, -3000, GETDATE());
    DECLARE @gia DECIMAL(18,0) = (SELECT TOP 1 ROUND(gia_ban, 0) FROM san_pham_chi_tiet WHERE ma_spct = 'SP04-DEN-4U-G5');
    DECLARE @id INT;

    INSERT INTO hoa_don (id_nhan_vien, id_khach_hang, id_phieu_giam_gia, id_phuong_thuc_thanh_toan, ma_hoa_don, loai_hoa_don,
                         tong_tien, so_tien_giam, phi_van_chuyen, thanh_tien, don_vi_van_chuyen, ho_ten_nguoi_nhan, sdt_nguoi_nhan,
                         dia_chi_giao_hang, ghi_chu, ngay_giao_du_kien, ngay_tao, ngay_cap_nhat, trang_thai)
    SELECT nv.id, kh.id, NULL, pt.id, 'HD19', 2,
           @gia, 0, 30000, @gia + 30000, N'Giao Hàng Tiết Kiệm', N'Hoàng Đức Anh', '0901000005',
           N'Số 102 Nguyễn Văn Cừ, Phường Hồng Bàng, Hải Phòng', N'Gọi trước khi giao',
           DATEADD(DAY, 4, @tao), @tao, DATEADD(MINUTE, 2000, @tao), 8
    FROM nhan_vien nv, khach_hang kh, phuong_thuc_thanh_toan pt
    WHERE nv.ma_nhan_vien = 'NV04' AND kh.ma_khach_hang = 'KH05' AND pt.ma_phuong_thuc = 'PT03';

    SELECT @id = id FROM hoa_don WHERE ma_hoa_don = 'HD19';

    INSERT INTO hoa_don_chi_tiet (id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia, thanh_tien, trang_thai)
    SELECT @id, c.id, 1, @gia, @gia, 1
    FROM san_pham_chi_tiet c
    WHERE c.ma_spct = 'SP04-DEN-4U-G5';

    INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_thao_tac, hanh_dong, thoi_gian, mo_ta)
    SELECT @id, CASE WHEN s.code = 1 THEN N'Khách hàng' ELSE nv.ho_ten END, s.hanh_dong, DATEADD(MINUTE, s.phut, @tao), s.mo_ta
    FROM (VALUES
        (1, N'Tạo đơn hàng',       0,    N'Khách đặt hàng online'),
        (2, N'Đã xác nhận',        30,   N'Nhân viên đã gọi xác nhận đơn với khách'),
        (3, N'Chờ giao hàng',      180,  N'Đã đóng gói, chờ đơn vị vận chuyển đến lấy'),
        (4, N'Đang giao hàng',     1440, N'Đã bàn giao cho Giao Hàng Tiết Kiệm'),
        (8, N'Giao hàng thất bại', 2000, N'Khách không nghe máy sau 3 lần gọi')
    ) AS s(code, hanh_dong, phut, mo_ta)
    CROSS JOIN (SELECT ho_ten FROM nhan_vien WHERE ma_nhan_vien = 'NV04') nv;
END
GO


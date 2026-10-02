-- =================================================================
-- TẠO CƠ SỞ DỮ LIỆU: SHOP CẦU LÔNG FOOTSTYLE (SQL SERVER)
-- =================================================================

CREATE DATABASE shop_cau_long_footstyle;
GO

USE shop_cau_long_footstyle;
GO

-- =================================================================
-- PHẦN 1: CÁC BẢNG DANH MỤC & THUỘC TÍNH (KHÔNG CÓ KHÓA NGOẠI)
-- =================================================================

CREATE TABLE khach_hang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_khach_hang VARCHAR(50) UNIQUE NOT NULL,
    ho_ten NVARCHAR(255) NOT NULL,
    ngay_sinh DATE,
    gioi_tinh TINYINT, -- 0: Nữ, 1: Nam
    sdt VARCHAR(15),
    email VARCHAR(255),
    mat_khau VARCHAR(255),
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE vai_tro (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ten_vai_tro NVARCHAR(100) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE phieu_giam_gia (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_phieu VARCHAR(50) UNIQUE NOT NULL,
    ten_phieu NVARCHAR(255) NOT NULL,
    doi_tuong_ap_dung TINYINT, -- 1: Tất cả, 2: Khách hàng cụ thể
    loai_giam TINYINT, -- 1: Phần trăm, 2: Số tiền
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

CREATE TABLE dot_giam_gia (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_dot VARCHAR(50) UNIQUE NOT NULL,
    ten_dot NVARCHAR(255) NOT NULL,
    phan_tram_giam_dot INT NOT NULL,
    ngay_bat_dau DATETIME NOT NULL,
    ngay_ket_thuc DATETIME NOT NULL,
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE danh_muc (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_danh_muc VARCHAR(50) UNIQUE NOT NULL,
    ten_danh_muc NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE thuong_hieu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_thuong_hieu VARCHAR(50) UNIQUE NOT NULL,
    ten_thuong_hieu NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE xuat_xu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_xuat_xu VARCHAR(50) UNIQUE NOT NULL,
    ten_xuat_xu NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE chat_lieu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_chat_lieu VARCHAR(50) UNIQUE NOT NULL,
    ten_chat_lieu NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE do_cung (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_do_cung VARCHAR(50) UNIQUE NOT NULL,
    ten_do_cung NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE diem_can_bang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_diem_can_bang VARCHAR(50) UNIQUE NOT NULL,
    ten_diem_can_bang NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE mau_sac (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_mau_sac VARCHAR(50) UNIQUE NOT NULL,
    ten_mau_sac NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE trong_luong (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_trong_luong VARCHAR(50) UNIQUE NOT NULL,
    ten_trong_luong NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE chu_vi (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_chu_vi VARCHAR(50) UNIQUE NOT NULL,
    ten_chu_vi NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE hinh_thuc_thanh_toan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_hinh_thuc VARCHAR(50) UNIQUE NOT NULL,
    ten_hinh_thuc NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);

-- =================================================================
-- PHẦN 2: CÁC BẢNG PHỤ THUỘC LEVEL 1 (CÓ KHÓA NGOẠI)
-- =================================================================

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
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id)
);

CREATE TABLE phieu_giam_gia_khach_hang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_khach_hang INT NOT NULL,
    id_phieu_giam_gia INT NOT NULL,
    ngay_su_dung DATETIME,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id)
);

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
    ngay_vao_lam DATE,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_vai_tro) REFERENCES vai_tro(id)
);

CREATE TABLE phuong_thuc_thanh_toan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hinh_thuc_thanh_toan INT NOT NULL,
    ma_phuong_thuc VARCHAR(50) UNIQUE NOT NULL,
    ten_phuong_thuc NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_hinh_thuc_thanh_toan) REFERENCES hinh_thuc_thanh_toan(id)
);

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

-- =================================================================
-- PHẦN 3: CÁC BẢNG PHỤ THUỘC LEVEL 2 (SẢN PHẨM CHI TIẾT, HÓA ĐƠN)
-- =================================================================

CREATE TABLE hinh_anh_san_pham (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham INT NOT NULL,
    duong_dan_hinh_anh VARCHAR(500) NOT NULL,
    la_anh_chinh BIT DEFAULT 0,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_san_pham) REFERENCES san_pham(id)
);

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

CREATE TABLE hoa_don (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_nhan_vien INT,
    id_khach_hang INT,
    id_phieu_giam_gia INT,
    id_phuong_thuc_thanh_toan INT,
    ma_hoa_don VARCHAR(50) UNIQUE NOT NULL,
    loai_hoa_don TINYINT, -- 1: Tại quầy, 2: Giao hàng
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

-- =================================================================
-- PHẦN 4: CÁC BẢNG PHỤ THUỘC LEVEL 3 (LỊCH SỬ VÀ CHI TIẾT)
-- =================================================================

CREATE TABLE dot_giam_gia_chi_tiet (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_san_pham_chi_tiet INT NOT NULL,
    id_dot_giam_gia INT NOT NULL,
    phan_tram_giam_bien_the INT NOT NULL,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES san_pham_chi_tiet(id),
    FOREIGN KEY (id_dot_giam_gia) REFERENCES dot_giam_gia(id)
);

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

CREATE TABLE lich_su_hoa_don (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hoa_don INT NOT NULL,
    nguoi_thao_tac NVARCHAR(255),
    hanh_dong NVARCHAR(255) NOT NULL,
    thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP,
    mo_ta NVARCHAR(MAX),
    FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id)
);
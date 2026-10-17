-- =================================================================
-- FOOTSTYLE (SHOP CẦU LÔNG) - FILE 1/2: CẤU TRÚC CƠ SỞ DỮ LIỆU (SQL SERVER)
-- =================================================================
-- Chỉ gồm phần TẠO: database, bảng, cột, khóa chính, khóa ngoại, ràng buộc UNIQUE, chỉ mục. KHÔNG có dữ liệu.
-- Dữ liệu mẫu + sửa dữ liệu cũ nằm ở file 2: database/SD_17_2_du_lieu.sql.
--
-- Cách chạy (SSMS: mở file, bấm Execute / F5):
--   1. Chạy file này.
--   2. Chạy database/SD_17_2_du_lieu.sql.
-- Dùng được cho CẢ CSDL MỚI lẫn CSDL ĐÃ CÓ DỮ LIỆU: database / bảng / cột / chỉ mục đã có thì bỏ qua, chỉ thêm phần còn
-- thiếu (không xóa, không sửa dữ liệu). Chạy lại nhiều lần không lỗi. Backend cũng tự thêm phần còn thiếu khi khởi động
-- (SchemaUpgradeRunner).
--
-- Bố cục:
--   PHẦN 1: bảng danh mục & thuộc tính (không có khóa ngoại)
--   PHẦN 2: bảng phụ thuộc cấp 1 (nhân viên, địa chỉ, sản phẩm...)
--   PHẦN 3: bảng phụ thuộc cấp 2 (biến thể, ảnh, hóa đơn)
--   PHẦN 4: bảng phụ thuộc cấp 3 (chi tiết, lịch sử)
--   PHẦN 5: nâng cấp CSDL tạo từ bản cũ (thêm cột còn thiếu)
--   PHẦN 6: chỉ mục (không trùng email / SĐT / căn cước, mỗi biến thể 1 bộ màu - trọng lượng - chu vi...)
-- =================================================================

IF DB_ID('shop_cau_long_footstyle') IS NULL
    CREATE DATABASE shop_cau_long_footstyle;
GO

USE shop_cau_long_footstyle;
GO

-- Bắt buộc bật để tạo / ghi bảng có chỉ mục lọc (filtered index) ở bên dưới.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- =================================================================
-- PHẦN 1: CÁC BẢNG DANH MỤC & THUỘC TÍNH (KHÔNG CÓ KHÓA NGOẠI)
-- =================================================================

-- gioi_tinh: 0 = Nữ, 1 = Nam. anh_dai_dien: đường dẫn /api/uploads/... hoặc link http, có thể để trống.
IF OBJECT_ID('khach_hang', 'U') IS NULL
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

IF OBJECT_ID('vai_tro', 'U') IS NULL
CREATE TABLE vai_tro (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ten_vai_tro NVARCHAR(100) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- doi_tuong_ap_dung: 1 = Tất cả (công khai), 2 = Khách hàng cụ thể (cá nhân). loai_giam: 1 = Phần trăm, 2 = Số tiền.
IF OBJECT_ID('phieu_giam_gia', 'U') IS NULL
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

IF OBJECT_ID('dot_giam_gia', 'U') IS NULL
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

IF OBJECT_ID('danh_muc', 'U') IS NULL
CREATE TABLE danh_muc (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_danh_muc VARCHAR(50) UNIQUE NOT NULL,
    ten_danh_muc NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

IF OBJECT_ID('thuong_hieu', 'U') IS NULL
CREATE TABLE thuong_hieu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_thuong_hieu VARCHAR(50) UNIQUE NOT NULL,
    ten_thuong_hieu NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

IF OBJECT_ID('xuat_xu', 'U') IS NULL
CREATE TABLE xuat_xu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_xuat_xu VARCHAR(50) UNIQUE NOT NULL,
    ten_xuat_xu NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- nhom_ap_dung: các nhóm danh mục dùng chất liệu này (VOT, QUAN_AO, TUI_BALO, PHU_KIEN, QUA_CAU, cách nhau dấu phẩy); NULL = mọi danh mục.
IF OBJECT_ID('chat_lieu', 'U') IS NULL
CREATE TABLE chat_lieu (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_chat_lieu VARCHAR(50) UNIQUE NOT NULL,
    ten_chat_lieu NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1,
    nhom_ap_dung VARCHAR(100) NULL
);
GO

IF OBJECT_ID('do_cung', 'U') IS NULL
CREATE TABLE do_cung (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_do_cung VARCHAR(50) UNIQUE NOT NULL,
    ten_do_cung NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

IF OBJECT_ID('diem_can_bang', 'U') IS NULL
CREATE TABLE diem_can_bang (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_diem_can_bang VARCHAR(50) UNIQUE NOT NULL,
    ten_diem_can_bang NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

IF OBJECT_ID('mau_sac', 'U') IS NULL
CREATE TABLE mau_sac (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_mau_sac VARCHAR(50) UNIQUE NOT NULL,
    ten_mau_sac NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

IF OBJECT_ID('trong_luong', 'U') IS NULL
CREATE TABLE trong_luong (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_trong_luong VARCHAR(50) UNIQUE NOT NULL,
    ten_trong_luong NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

IF OBJECT_ID('chu_vi', 'U') IS NULL
CREATE TABLE chu_vi (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_chu_vi VARCHAR(50) UNIQUE NOT NULL,
    ten_chu_vi NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

IF OBJECT_ID('hinh_thuc_thanh_toan', 'U') IS NULL
CREATE TABLE hinh_thuc_thanh_toan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ma_hinh_thuc VARCHAR(50) UNIQUE NOT NULL,
    ten_hinh_thuc NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1
);
GO

-- =================================================================
-- PHẦN 2: CÁC BẢNG PHỤ THUỘC CẤP 1 (CÓ KHÓA NGOẠI)
-- =================================================================

IF OBJECT_ID('dia_chi_khach_hang', 'U') IS NULL
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

IF OBJECT_ID('phieu_giam_gia_khach_hang', 'U') IS NULL
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

-- dia_chi: địa chỉ chính (chép từ địa chỉ được chọn trong bảng dia_chi_nhan_vien).
-- can_cuoc_cong_dan: số căn cước công dân 12 số, nhập tay hoặc quét mã QR trên thẻ.
IF OBJECT_ID('nhan_vien', 'U') IS NULL
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

-- Vị trí làm việc của nhân viên: hệ thống KHÔNG còn dùng (đã bỏ "Vị trí làm việc"); giữ 2 bảng để CSDL mới và cũ giống nhau.
IF OBJECT_ID('vi_tri', 'U') IS NULL
CREATE TABLE vi_tri (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ten_vi_tri NVARCHAR(100) NOT NULL UNIQUE,
    trang_thai TINYINT DEFAULT 1
);
GO

IF OBJECT_ID('nhan_vien_vi_tri', 'U') IS NULL
CREATE TABLE nhan_vien_vi_tri (
    id_nhan_vien INT NOT NULL,
    id_vi_tri INT NOT NULL,
    PRIMARY KEY (id_nhan_vien, id_vi_tri),
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    FOREIGN KEY (id_vi_tri) REFERENCES vi_tri(id)
);
GO

-- Một nhân viên có thể có NHIỀU địa chỉ (chọn 1 địa chỉ chính). Không xóa địa chỉ, chỉ thêm / sửa / chọn lại.
-- Địa chỉ theo đơn vị hành chính sau sáp nhập 07/2025: Tỉnh/Thành phố -> Phường/Xã (không còn Quận/Huyện).
IF OBJECT_ID('dia_chi_nhan_vien', 'U') IS NULL
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

IF OBJECT_ID('phuong_thuc_thanh_toan', 'U') IS NULL
CREATE TABLE phuong_thuc_thanh_toan (
    id INT IDENTITY(1,1) PRIMARY KEY,
    id_hinh_thuc_thanh_toan INT NOT NULL,
    ma_phuong_thuc VARCHAR(50) UNIQUE NOT NULL,
    ten_phuong_thuc NVARCHAR(255) NOT NULL,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_hinh_thuc_thanh_toan) REFERENCES hinh_thuc_thanh_toan(id)
);
GO

IF OBJECT_ID('san_pham', 'U') IS NULL
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

-- =================================================================
-- PHẦN 3: CÁC BẢNG PHỤ THUỘC CẤP 2 (ẢNH, BIẾN THỂ, HÓA ĐƠN)
-- =================================================================

-- Ảnh sản phẩm. id_mau_sac: NULL = ảnh chung của sản phẩm; có giá trị = ảnh của màu đó (dùng cho mọi biến thể cùng màu,
-- mỗi màu có 1 ảnh la_anh_chinh = 1).
IF OBJECT_ID('hinh_anh_san_pham', 'U') IS NULL
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

IF OBJECT_ID('san_pham_chi_tiet', 'U') IS NULL
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

-- loai_hoa_don: 1 = Tại quầy, 2 = Giao hàng.
-- trang_thai: 0 Đã hủy, 1 Chờ xác nhận, 2 Đã xác nhận, 3 Chờ giao hàng, 4 Đang giao hàng, 5 Đã giao hàng, 6 Đã hoàn thành,
--             7 Hoàn tiền, 8 Giao hàng thất bại.
IF OBJECT_ID('hoa_don', 'U') IS NULL
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

-- =================================================================
-- PHẦN 4: CÁC BẢNG PHỤ THUỘC CẤP 3 (CHI TIẾT VÀ LỊCH SỬ)
-- =================================================================

IF OBJECT_ID('dot_giam_gia_chi_tiet', 'U') IS NULL
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

IF OBJECT_ID('hoa_don_chi_tiet', 'U') IS NULL
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

IF OBJECT_ID('lich_su_thanh_toan', 'U') IS NULL
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

IF OBJECT_ID('lich_su_hoa_don', 'U') IS NULL
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

-- =================================================================
-- PHẦN 5: NÂNG CẤP CSDL TẠO TỪ BẢN CŨ - thêm cột còn thiếu vào bảng đã có
-- (CSDL vừa tạo ở trên đã đủ cột nên các lệnh này không làm gì.)
-- Không có bước này thì backend báo "Invalid column name ..." (vd 'can_cuoc_cong_dan', 'id_mau_sac').
-- =================================================================

-- Khách hàng: ảnh đại diện (trang Thêm / Sửa khách hàng có ô tải ảnh).
IF COL_LENGTH('khach_hang', 'anh_dai_dien') IS NULL
    ALTER TABLE khach_hang ADD anh_dai_dien NVARCHAR(500) NULL;
GO

-- Địa chỉ khách hàng: ngày tạo / ngày cập nhật.
IF COL_LENGTH('dia_chi_khach_hang', 'ngay_tao') IS NULL
    ALTER TABLE dia_chi_khach_hang ADD ngay_tao DATETIME NULL CONSTRAINT df_dckh_ngay_tao DEFAULT CURRENT_TIMESTAMP;
GO

IF COL_LENGTH('dia_chi_khach_hang', 'ngay_cap_nhat') IS NULL
    ALTER TABLE dia_chi_khach_hang ADD ngay_cap_nhat DATETIME NULL CONSTRAINT df_dckh_ngay_cap_nhat DEFAULT CURRENT_TIMESTAMP;
GO

-- Đợt giảm giá: mô tả.
IF COL_LENGTH('dot_giam_gia', 'mo_ta') IS NULL
    ALTER TABLE dot_giam_gia ADD mo_ta NVARCHAR(500) NULL;
GO

-- Chất liệu theo danh mục.
IF COL_LENGTH('chat_lieu', 'nhom_ap_dung') IS NULL
    ALTER TABLE chat_lieu ADD nhom_ap_dung VARCHAR(100) NULL;
GO

-- Nhân viên: số căn cước công dân. Bản cũ từng dùng tên cột cccd: bỏ chỉ mục cũ trên cột cccd (chỉ mục mới tạo ở PHẦN 6;
-- số căn cước đã nhập ở cột cccd được chép sang cột mới trong file dữ liệu SD_17_2_du_lieu.sql). Cột cccd cũ vẫn giữ, không xóa.
IF COL_LENGTH('nhan_vien', 'can_cuoc_cong_dan') IS NULL
    ALTER TABLE nhan_vien ADD can_cuoc_cong_dan VARCHAR(12) NULL;
GO

IF EXISTS (SELECT 1 FROM sys.indexes i
           JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
           JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
           WHERE i.name = 'ux_nhan_vien_cccd' AND i.object_id = OBJECT_ID('nhan_vien') AND c.name = 'cccd')
    DROP INDEX ux_nhan_vien_cccd ON nhan_vien;
GO

-- Ảnh sản phẩm theo màu (NULL = ảnh chung, toàn bộ ảnh cũ giữ nguyên).
IF COL_LENGTH('hinh_anh_san_pham', 'id_mau_sac') IS NULL
    ALTER TABLE hinh_anh_san_pham ADD id_mau_sac INT NULL
        CONSTRAINT fk_hasp_mau_sac FOREIGN KEY REFERENCES mau_sac(id);
GO

-- =================================================================
-- PHẦN 6: CHỈ MỤC - lớp bảo vệ cuối ở mức CSDL (backend đã kiểm tra trước khi lưu), chạy lại nhiều lần được
-- =================================================================

-- Nhân viên: email, số điện thoại, số căn cước không trùng (bỏ qua giá trị NULL).
-- Báo lỗi "duplicate key" nghĩa là CSDL cũ đang có nhân viên trùng: sửa dữ liệu trùng rồi chạy lại.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_email' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_email ON nhan_vien (email) WHERE email IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_sdt' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_sdt ON nhan_vien (sdt) WHERE sdt IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_cccd' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (can_cuoc_cong_dan) WHERE can_cuoc_cong_dan IS NOT NULL;
GO

-- Khách hàng: email và số điện thoại không trùng. Chỉ tạo khi dữ liệu hiện có chưa trùng; nếu trùng thì in danh sách
-- trùng ra để sửa dữ liệu trước rồi chạy lại file này.
IF EXISTS (SELECT 1 FROM khach_hang WHERE email IS NOT NULL AND email <> '' GROUP BY email HAVING COUNT(*) > 1)
BEGIN
    PRINT N'Không tạo được chỉ mục email khách hàng vì đang có email bị trùng:';
    SELECT email, COUNT(*) AS so_khach FROM khach_hang WHERE email IS NOT NULL AND email <> '' GROUP BY email HAVING COUNT(*) > 1;
END
ELSE IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_khach_hang_email' AND object_id = OBJECT_ID('khach_hang'))
    EXEC('CREATE UNIQUE INDEX ux_khach_hang_email ON khach_hang (email) WHERE email IS NOT NULL AND email <> ''''');
GO

IF EXISTS (SELECT 1 FROM khach_hang WHERE sdt IS NOT NULL AND sdt <> '' GROUP BY sdt HAVING COUNT(*) > 1)
BEGIN
    PRINT N'Không tạo được chỉ mục số điện thoại khách hàng vì đang có số bị trùng:';
    SELECT sdt, COUNT(*) AS so_khach FROM khach_hang WHERE sdt IS NOT NULL AND sdt <> '' GROUP BY sdt HAVING COUNT(*) > 1;
END
ELSE IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_khach_hang_sdt' AND object_id = OBJECT_ID('khach_hang'))
    EXEC('CREATE UNIQUE INDEX ux_khach_hang_sdt ON khach_hang (sdt) WHERE sdt IS NOT NULL AND sdt <> ''''');
GO

-- Mỗi sản phẩm chỉ có 1 biến thể cho mỗi bộ (màu sắc, trọng lượng, chu vi).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_spct_san_pham_mau_tl_cv' AND object_id = OBJECT_ID('san_pham_chi_tiet'))
    CREATE UNIQUE INDEX ux_spct_san_pham_mau_tl_cv ON san_pham_chi_tiet (id_san_pham, id_mau_sac, id_trong_luong, id_chu_vi);
GO

-- Ảnh theo sản phẩm + màu (tìm ảnh của từng màu nhanh hơn).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_hasp_san_pham_mau' AND object_id = OBJECT_ID('hinh_anh_san_pham'))
    CREATE INDEX ix_hasp_san_pham_mau ON hinh_anh_san_pham (id_san_pham, id_mau_sac);
GO

-- Kiểm tra: CSDL mới phải ra 29 bảng (kể cả 2 bảng vị trí không còn dùng)
SELECT COUNT(*) AS so_bang FROM sys.tables WHERE is_ms_shipped = 0;
GO

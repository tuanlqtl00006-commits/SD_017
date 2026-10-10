-- =================================================================
-- SD_17_tong.sql: FILE SQL TỔNG của shop cầu lông FootStyle (SQL Server)
-- Chạy MỘT LẦN trong SSMS (Execute / F5) để có CSDL đầy đủ: cấu trúc + dữ liệu mẫu.
-- Gồm lần lượt (theo đúng thứ tự chạy):
--   1. Cấu trúc CSDL (tạo database + toàn bộ bảng)
--   2. Dữ liệu mẫu: vai trò, nhân viên, khách hàng, phiếu giảm giá
--   3. Dữ liệu mẫu: thuộc tính, sản phẩm, biến thể (kèm mục "Không áp dụng")
--   4. Dữ liệu mẫu: địa chỉ khách hàng, đợt giảm giá
--   5. Dữ liệu mẫu: phương thức thanh toán, hóa đơn
--   6. Nâng cấp CSDL cũ (chạy lại nhiều lần được, CSDL mới tạo thì không làm gì)
--
-- Tạo CSDL mới: chạy cả file. Phần 6 chạy lại nhiều lần được và không làm gì với CSDL vừa tạo.
-- Đã có CSDL từ trước (không chạy được từ đầu vì database đã tồn tại): chỉ chạy phần 6 ở cuối file
-- (từ dòng "6. Nâng cấp CSDL cũ" đến hết), hoặc cứ khởi động backend: SchemaUpgradeRunner tự nâng cấp.
-- =================================================================


-- #################################################################
-- PHẦN 1. Cấu trúc CSDL (tạo database + toàn bộ bảng)
-- (nội dung giữ nguyên từ SD_17.sql)
-- #################################################################

-- =================================================================
-- TẠO CƠ SỞ DỮ LIỆU: SHOP CẦU LÔNG FOOTSTYLE (SQL SERVER)
-- =================================================================

CREATE DATABASE shop_cau_long_footstyle;
GO

USE shop_cau_long_footstyle;
GO

-- Bắt buộc bật để tạo/ghi bảng có chỉ mục lọc (filtered index) ở bên dưới.
SET QUOTED_IDENTIFIER ON;
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
    mo_ta NVARCHAR(500) NULL,
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
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
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
    dia_chi NVARCHAR(500), -- địa chỉ chính (chép từ địa chỉ được chọn trong bảng dia_chi_nhan_vien)
    cccd VARCHAR(12), -- số căn cước công dân (12 số), nhập tay hoặc quét mã QR trên thẻ
    ngay_vao_lam DATE,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_vai_tro) REFERENCES vai_tro(id)
);

-- Email và số điện thoại nhân viên không được trùng (bỏ qua giá trị NULL). Backend đã kiểm tra; đây là lớp bảo vệ cuối ở mức CSDL.
CREATE UNIQUE INDEX ux_nhan_vien_email ON nhan_vien (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX ux_nhan_vien_sdt ON nhan_vien (sdt) WHERE sdt IS NOT NULL;
CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (cccd) WHERE cccd IS NOT NULL;

-- Một nhân viên có thể có NHIỀU địa chỉ (chọn 1 địa chỉ chính). Không xóa địa chỉ, chỉ thêm / sửa / chọn lại.
-- Địa chỉ theo đơn vị hành chính sau sáp nhập 07/2025: Tỉnh/Thành phố -> Phường/Xã (không còn Quận/Huyện).
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

-- Vị trí làm việc của nhân viên: một nhân viên có nhiều vị trí. Vị trí chỉ thêm / chọn, không xóa.
CREATE TABLE vi_tri (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ten_vi_tri NVARCHAR(100) NOT NULL UNIQUE,
    trang_thai TINYINT DEFAULT 1
);

CREATE TABLE nhan_vien_vi_tri (
    id_nhan_vien INT NOT NULL,
    id_vi_tri INT NOT NULL,
    PRIMARY KEY (id_nhan_vien, id_vi_tri),
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    FOREIGN KEY (id_vi_tri) REFERENCES vi_tri(id)
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

-- Mỗi sản phẩm chỉ có 1 biến thể cho mỗi bộ (màu sắc, trọng lượng, chu vi).
CREATE UNIQUE INDEX ux_spct_san_pham_mau_tl_cv ON san_pham_chi_tiet (id_san_pham, id_mau_sac, id_trong_luong, id_chu_vi);

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


-- #################################################################
-- PHẦN 2. Dữ liệu mẫu: vai trò, nhân viên, khách hàng, phiếu giảm giá
-- (nội dung giữ nguyên từ SD_17_du_lieu_mau.sql)
-- #################################################################

-- =================================================================
-- DỮ LIỆU MẪU: vai trò, nhân viên, khách hàng, phiếu giảm giá
-- Chạy SAU khi đã chạy SD_17.sql. Có thể chạy lại nhiều lần (bản ghi đã có mã thì bỏ qua).
-- Mật khẩu của mọi nhân viên mẫu: Footstyle@123 (đã băm BCrypt, đăng nhập sẽ dùng cùng thuật toán).
-- =================================================================

USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON; -- cần khi ghi bảng có chỉ mục lọc (nhan_vien)
GO

-- 0. Bổ sung cấu trúc còn thiếu nếu CSDL được tạo từ bản SD_17.sql CŨ (CSDL mới đã đủ thì các lệnh dưới không làm gì).
--    Không có bước này thì script báo "Invalid object name 'vi_tri'" và backend báo "Invalid column name 'cccd'".
IF COL_LENGTH('nhan_vien', 'cccd') IS NULL
    ALTER TABLE nhan_vien ADD cccd VARCHAR(12) NULL;
GO

-- Bản trước đặt tên cột số căn cước là can_cuoc_cong_dan: nếu CSDL đang có cột đó thì chép số sang cột cccd rồi bỏ chỉ mục cũ
-- (cột cũ được giữ lại, không xóa dữ liệu; backend chỉ đọc / ghi cột cccd).
IF COL_LENGTH('nhan_vien', 'can_cuoc_cong_dan') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.indexes i
               JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
               JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
               WHERE i.name = 'ux_nhan_vien_cccd' AND i.object_id = OBJECT_ID('nhan_vien') AND c.name = 'can_cuoc_cong_dan')
        DROP INDEX ux_nhan_vien_cccd ON nhan_vien;
    EXEC('UPDATE nhan_vien SET cccd = can_cuoc_cong_dan WHERE cccd IS NULL AND can_cuoc_cong_dan IS NOT NULL');
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_cccd' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (cccd) WHERE cccd IS NOT NULL;
GO

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

IF COL_LENGTH('dia_chi_khach_hang', 'ngay_tao') IS NULL
    ALTER TABLE dia_chi_khach_hang ADD ngay_tao DATETIME NULL CONSTRAINT df_dckh_ngay_tao DEFAULT CURRENT_TIMESTAMP;
GO

IF COL_LENGTH('dia_chi_khach_hang', 'ngay_cap_nhat') IS NULL
    ALTER TABLE dia_chi_khach_hang ADD ngay_cap_nhat DATETIME NULL CONSTRAINT df_dckh_ngay_cap_nhat DEFAULT CURRENT_TIMESTAMP;
GO

IF COL_LENGTH('dot_giam_gia', 'mo_ta') IS NULL
    ALTER TABLE dot_giam_gia ADD mo_ta NVARCHAR(500) NULL;
GO

-- 1. Vai trò (tên "Quản lý" phải khớp VaiTro.TEN_QUAN_LY ở backend)
INSERT INTO vai_tro (ten_vai_tro, trang_thai)
SELECT v.ten, 1 FROM (VALUES (N'Quản lý'), (N'Nhân viên')) AS v(ten)
WHERE NOT EXISTS (SELECT 1 FROM vai_tro r WHERE r.ten_vai_tro = v.ten);
GO

-- 1b. Vị trí làm việc (nhân viên có thể giữ nhiều vị trí; thêm vị trí mới ngay trên form nhân viên)
INSERT INTO vi_tri (ten_vi_tri, trang_thai)
SELECT v.ten, 1 FROM (VALUES (N'Bán hàng tại quầy'), (N'Thu ngân'), (N'Kho'), (N'Giao hàng'), (N'Chăm sóc khách hàng')) AS v(ten)
WHERE NOT EXISTS (SELECT 1 FROM vi_tri t WHERE t.ten_vi_tri = v.ten);
GO

-- 2. Nhân viên
INSERT INTO nhan_vien (id_vai_tro, ma_nhan_vien, ho_ten, gioi_tinh, ngay_sinh, sdt, email, mat_khau, dia_chi, ngay_vao_lam, trang_thai)
SELECT r.id, s.ma, s.ho_ten, s.gioi_tinh, s.ngay_sinh, s.sdt, s.email, s.mat_khau, s.dia_chi, s.ngay_vao_lam, s.trang_thai
FROM (VALUES
    (N'Quản lý', 'NV0001', N'Nguyễn Hoàng Long', 1, '1992-03-14', '0912000001', 'long.nh@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 15 ngõ 120 Trần Phú, phường Hà Đông, Hà Nội', '2022-01-10', 1),
    (N'Quản lý', 'NV0002', N'Trần Thu Hà', 0, '1994-07-22', '0912000002', 'ha.tt@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 22 Nguyễn Trãi, quận Thanh Xuân, Hà Nội', '2022-03-01', 1),
    (N'Nhân viên', 'NV0003', N'Lê Quốc Bảo', 1, '1999-11-05', '0912000003', 'bao.lq@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 8 đường Lê Văn Lương, quận Thanh Xuân, Hà Nội', '2023-02-15', 1),
    (N'Nhân viên', 'NV0004', N'Phạm Minh Anh', 0, '2000-01-18', '0912000004', 'anh.pm@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 31 phố Kim Mã, quận Ba Đình, Hà Nội', '2023-05-20', 1),
    (N'Nhân viên', 'NV0005', N'Vũ Hoàng Nam', 1, '1998-09-30', '0912000005', 'nam.vh@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Thị trấn Quốc Oai, huyện Quốc Oai, Hà Nội', '2023-08-01', 1),
    (N'Nhân viên', 'NV0006', N'Đỗ Thùy Trang', 0, '2001-05-09', '0912000006', 'trang.dt@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 45 phố Trần Thái Tông, phường Cầu Giấy, Hà Nội', '2024-01-08', 1),
    (N'Nhân viên', 'NV0007', N'Bùi Đức Mạnh', 1, '1997-12-02', '0912000007', 'manh.bd@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 3 ngõ 66 Nguyễn Văn Cừ, quận Long Biên, Hà Nội', '2023-10-12', 0),
    (N'Nhân viên', 'NV0008', N'Ngô Linh Chi', 0, '2002-04-27', '0912000008', 'chi.nl@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 19 đường Phạm Văn Đồng, quận Bắc Từ Liêm, Hà Nội', '2024-06-03', 1),
    (N'Nhân viên', 'NV0009', N'Hoàng Tuấn Anh', 1, '1996-08-11', '0912000009', 'anh.ht@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 7 phố Hoàng Quốc Việt, quận Cầu Giấy, Hà Nội', '2024-09-16', 1),
    (N'Nhân viên', 'NV0010', N'Đặng Mai Hương', 0, '1995-02-25', '0912000010', 'huong.dm@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 52 phố Minh Khai, quận Hai Bà Trưng, Hà Nội', '2022-11-21', 0)
) AS s(vai_tro, ma, ho_ten, gioi_tinh, ngay_sinh, sdt, email, mat_khau, dia_chi, ngay_vao_lam, trang_thai)
JOIN vai_tro r ON r.ten_vai_tro = s.vai_tro
WHERE NOT EXISTS (SELECT 1 FROM nhan_vien n WHERE n.ma_nhan_vien = s.ma);
GO

-- 2b. Địa chỉ nhân viên: mỗi nhân viên mẫu có 1 địa chỉ chính (chép từ cột dia_chi), thêm nhiều địa chỉ khác trên giao diện.
INSERT INTO dia_chi_nhan_vien (id_nhan_vien, dia_chi_chi_tiet, la_mac_dinh)
SELECT n.id, n.dia_chi, 1
FROM nhan_vien n
WHERE n.dia_chi IS NOT NULL AND LTRIM(RTRIM(n.dia_chi)) <> ''
  AND NOT EXISTS (SELECT 1 FROM dia_chi_nhan_vien d WHERE d.id_nhan_vien = n.id);
GO

-- 3. Khách hàng (để chọn khi tặng phiếu giảm giá cá nhân)
INSERT INTO khach_hang (ma_khach_hang, ho_ten, gioi_tinh, sdt, email, trang_thai)
SELECT c.ma, c.ho_ten, c.gioi_tinh, c.sdt, c.email, 1
FROM (VALUES
    ('KH0001', N'Nguyễn Thị Lan', '0901000001', 'lan.nt@gmail.com', 0),
    ('KH0002', N'Trần Văn Hùng', '0901000002', 'hung.tv@gmail.com', 1),
    ('KH0003', N'Lê Minh Tuấn', '0901000003', 'tuan.lm@gmail.com', 1),
    ('KH0004', N'Phạm Thu Hằng', '0901000004', 'hang.pt@gmail.com', 0),
    ('KH0005', N'Hoàng Đức Anh', '0901000005', 'anh.hd@gmail.com', 1),
    ('KH0006', N'Vũ Khánh Linh', '0901000006', 'linh.vk@gmail.com', 0)
) AS c(ma, ho_ten, sdt, email, gioi_tinh)
WHERE NOT EXISTS (SELECT 1 FROM khach_hang k WHERE k.ma_khach_hang = c.ma);
GO

-- 4. Phiếu giảm giá (ngày tính theo hôm nay để luôn có đủ: đang hoạt động / sắp diễn ra / đã kết thúc / ngưng)
--    doi_tuong_ap_dung: 1 = Tất cả (công khai), 2 = Khách hàng cụ thể (cá nhân); loai_giam: 1 = Phần trăm, 2 = Số tiền
--    Phiếu cá nhân: so_luong = số khách được tặng, gioi_han_moi_khach = 1.
INSERT INTO phieu_giam_gia (ma_phieu, ten_phieu, doi_tuong_ap_dung, loai_giam, gia_tri_giam, giam_toi_da,
                            gia_tri_don_hang_toi_thieu, so_luong, so_luong_da_dung, gioi_han_moi_khach,
                            ngay_bat_dau, ngay_ket_thuc, trang_thai)
SELECT p.ma, p.ten, p.doi_tuong, p.loai, p.gia_tri, p.toi_da, p.don_min, p.so_luong, 0, p.gioi_han,
       CAST(DATEADD(DAY, p.bd, CAST(GETDATE() AS DATE)) AS DATETIME),
       DATEADD(SECOND, 86399, CAST(DATEADD(DAY, p.kt, CAST(GETDATE() AS DATE)) AS DATETIME)),
       p.trang_thai
FROM (VALUES
    ('VCH7QX2YN', N'Giảm 10% vợt Yonex', 1, 1, 10, 200000, 1500000, 100, 2, -10, 20, 1),
    ('VCHFS500K', N'Hỗ trợ phí vận chuyển đơn từ 500K', 1, 2, 30000, NULL, 500000, 300, 1, -5, 25, 1),
    ('VCH8DK3SO', N'Giảm sốc phụ kiện xả kho', 1, 1, 60, 500000, 2000000, 20, NULL, -45, -29, 0),
    ('VCHTT50KH', N'Tri ân khách hàng thân thiết', 2, 2, 50000, NULL, 300000, 3, 1, -2, 60, 1),
    ('VCHSN26FS', N'Sinh nhật FootStyle', 1, 1, 15, 300000, 1000000, 200, NULL, 7, 14, 1),
    ('VCHPK20QC', N'Giảm 20% phụ kiện, quả cầu', 1, 1, 20, 100000, 200000, 150, 3, -60, -20, 1),
    ('VCHDOI100', N'Tặng khách mua vợt đôi', 2, 2, 100000, NULL, 3000000, 2, 1, -15, 45, 0),
    ('VCHNEWMB1', N'Chào thành viên mới', 2, 2, 40000, NULL, 250000, 1, 1, -30, 90, 1),
    ('VCHHE12XN', N'Hè xanh - giảm 12%', 1, 1, 12, 150000, 800000, 120, NULL, -90, -61, 1),
    ('VCHG30K01', N'Giảm 30K cho đơn từ 600K', 1, 2, 30000, NULL, 600000, 80, 1, 3, 30, 1),
    ('VCHBF25SM', N'Black Friday sớm', 1, 1, 25, 400000, 2500000, 60, 1, 40, 45, 0)
) AS p(ma, ten, doi_tuong, loai, gia_tri, toi_da, don_min, so_luong, gioi_han, bd, kt, trang_thai)
WHERE NOT EXISTS (SELECT 1 FROM phieu_giam_gia x WHERE x.ma_phieu = p.ma);
GO

-- 5. Khách hàng được tặng phiếu cá nhân
INSERT INTO phieu_giam_gia_khach_hang (id_khach_hang, id_phieu_giam_gia, trang_thai)
SELECT k.id, p.id, 1
FROM (VALUES
    ('VCHTT50KH', 'KH0001'),
    ('VCHTT50KH', 'KH0002'),
    ('VCHTT50KH', 'KH0003'),
    ('VCHDOI100', 'KH0004'),
    ('VCHDOI100', 'KH0005'),
    ('VCHNEWMB1', 'KH0006')
) AS t(ma_phieu, ma_khach_hang)
JOIN phieu_giam_gia p ON p.ma_phieu = t.ma_phieu
JOIN khach_hang k ON k.ma_khach_hang = t.ma_khach_hang
WHERE NOT EXISTS (SELECT 1 FROM phieu_giam_gia_khach_hang l WHERE l.id_phieu_giam_gia = p.id AND l.id_khach_hang = k.id);
GO


-- #################################################################
-- PHẦN 3. Dữ liệu mẫu: thuộc tính, sản phẩm, biến thể (kèm mục "Không áp dụng")
-- (nội dung giữ nguyên từ SD_17_du_lieu_san_pham.sql)
-- #################################################################

-- =================================================================
-- DỮ LIỆU MẪU PHẦN QUẢN LÝ SẢN PHẨM: 9 bảng thuộc tính, sản phẩm, biến thể
-- Chạy SAU SD_17.sql (có thể chạy trước hoặc sau SD_17_du_lieu_mau.sql). Chạy lại nhiều lần được (bản ghi đã có mã thì bỏ qua).
-- Dữ liệu trùng với dữ liệu mẫu cũ của giao diện: 6 sản phẩm, 14 biến thể.
-- =================================================================

USE shop_cau_long_footstyle;
GO

-- 1. Danh mục
INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('DM001', N'Vợt cầu lông'),
    ('DM002', N'Vợt cầu lông người mới'),
    ('DM003', N'Quần áo thể thao'),
    ('DM004', N'Túi - balo'),
    ('DM005', N'Phụ kiện'),
    ('DM006', N'Quả cầu lông')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM danh_muc t WHERE t.ma_danh_muc = v.ma);
GO

-- 2. Thương hiệu
INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('TH001', N'Yonex'),
    ('TH002', N'Lining'),
    ('TH003', N'Victor'),
    ('TH004', N'Mizuno'),
    ('TH005', N'Kumpoo')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM thuong_hieu t WHERE t.ma_thuong_hieu = v.ma);
GO

-- 3. Xuất xứ
INSERT INTO xuat_xu (ma_xuat_xu, ten_xuat_xu, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('XX001', N'Nhật Bản'),
    ('XX002', N'Trung Quốc'),
    ('XX003', N'Đài Loan'),
    ('XX004', N'Việt Nam')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM xuat_xu t WHERE t.ma_xuat_xu = v.ma);
GO

-- 4. Chất liệu
INSERT INTO chat_lieu (ma_chat_lieu, ten_chat_lieu, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('CL001', N'Carbon'),
    ('CL002', N'Graphite'),
    ('CL003', N'Nhôm'),
    ('CL004', N'Titanium'),
    ('CL005', N'Da PU')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM chat_lieu t WHERE t.ma_chat_lieu = v.ma);
GO

-- 5. Độ cứng
INSERT INTO do_cung (ma_do_cung, ten_do_cung, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('DC001', N'Mềm'),
    ('DC002', N'Trung bình'),
    ('DC003', N'Cứng'),
    ('DC004', N'Rất cứng')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM do_cung t WHERE t.ma_do_cung = v.ma);
GO

-- 6. Điểm cân bằng
INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('CB001', N'Nặng đầu'),
    ('CB002', N'Cân bằng'),
    ('CB003', N'Nhẹ đầu')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM diem_can_bang t WHERE t.ma_diem_can_bang = v.ma);
GO

-- 7. Màu sắc
INSERT INTO mau_sac (ma_mau_sac, ten_mau_sac, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('MS001', N'Đen'),
    ('MS002', N'Trắng'),
    ('MS003', N'Đỏ'),
    ('MS004', N'Xanh dương'),
    ('MS005', N'Vàng')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM mau_sac t WHERE t.ma_mau_sac = v.ma);
GO

-- 8. Trọng lượng
INSERT INTO trong_luong (ma_trong_luong, ten_trong_luong, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('TL001', N'2U (90-94g)'),
    ('TL002', N'3U (85-89g)'),
    ('TL003', N'4U (80-84g)'),
    ('TL004', N'5U (75-79g)')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM trong_luong t WHERE t.ma_trong_luong = v.ma);
GO

-- 9. Chu vi
INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('CV001', N'G4'),
    ('CV002', N'G5'),
    ('CV003', N'G6')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM chu_vi t WHERE t.ma_chu_vi = v.ma);
GO

-- 9b. Mục "Không áp dụng": danh mục không phải vợt (Phụ kiện, Túi - balo, Quần áo thể thao, Quả cầu lông...) không có
--     độ cứng / điểm cân bằng / trọng lượng / chu vi, nên hệ thống tự gán mục này (không hiện trong ô chọn và danh sách thuộc tính).
INSERT INTO do_cung (ma_do_cung, ten_do_cung, trang_thai)
SELECT 'DC000', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM do_cung t WHERE t.ma_do_cung = 'DC000');
INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai)
SELECT 'CB000', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM diem_can_bang t WHERE t.ma_diem_can_bang = 'CB000');
INSERT INTO trong_luong (ma_trong_luong, ten_trong_luong, trang_thai)
SELECT 'TL000', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM trong_luong t WHERE t.ma_trong_luong = 'TL000');
INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai)
SELECT 'CV000', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM chu_vi t WHERE t.ma_chu_vi = 'CV000');
GO

-- 10. Sản phẩm (trỏ tới thuộc tính bằng mã)
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang,
                      ma_san_pham, ten_san_pham, mo_ta, trang_thai)
SELECT dm.id, th.id, xx.id, cl.id, dc.id, cb.id, s.ma, s.ten, s.mo_ta, 1
FROM (VALUES
    ('SP001', N'Yonex Astrox 99 Pro', 'DM001', 'TH001', 'XX001', 'CL001', 'DC003', 'CB001', N'Vợt thiên công cao cấp, khung carbon.'),
    ('SP002', N'Lining Axforce 80', 'DM001', 'TH002', 'XX002', 'CL001', 'DC004', 'CB001', N'Vợt dành cho người chơi lực tay tốt.'),
    ('SP003', N'Victor Thruster K 9900', 'DM001', 'TH003', 'XX003', 'CL002', 'DC003', 'CB001', N'Vợt thiên công, nặng đầu, đập cầu mạnh.'),
    ('SP004', N'Yonex Nanoflare 800 Pro', 'DM001', 'TH001', 'XX001', 'CL001', 'DC002', 'CB002', N'Vợt thiên tốc độ, khung nhẹ, phản hồi nhanh.'),
    ('SP005', N'Mizuno Fortius 10 Quick', 'DM001', 'TH004', 'XX001', 'CL001', 'DC002', 'CB002', N'Vợt nhẹ, linh hoạt, dễ điều cầu.'),
    ('SP006', N'Kumpoo Power Control', 'DM001', 'TH005', 'XX003', 'CL002', 'DC002', 'CB002', N'Vợt công thủ toàn diện, phù hợp người mới chơi.')
) AS s(ma, ten, dm, th, xx, cl, dc, cb, mo_ta)
JOIN danh_muc dm ON dm.ma_danh_muc = s.dm
JOIN thuong_hieu th ON th.ma_thuong_hieu = s.th
JOIN xuat_xu xx ON xx.ma_xuat_xu = s.xx
JOIN chat_lieu cl ON cl.ma_chat_lieu = s.cl
JOIN do_cung dc ON dc.ma_do_cung = s.dc
JOIN diem_can_bang cb ON cb.ma_diem_can_bang = s.cb
WHERE NOT EXISTS (SELECT 1 FROM san_pham p WHERE p.ma_san_pham = s.ma);
GO

-- 11. Biến thể = sản phẩm + màu sắc + trọng lượng + chu vi
INSERT INTO san_pham_chi_tiet (id_san_pham, id_mau_sac, id_trong_luong, id_chu_vi, ma_spct, gia_ban, so_luong_ton, trang_thai)
SELECT p.id, ms.id, tl.id, cv.id, b.ma, b.gia, b.ton, 1
FROM (VALUES
    ('SP001-DEN-4U-G5', 'SP001', 'MS001', 'TL003', 'CV002', 4290000, 20),
    ('SP001-DEN-4U-G4', 'SP001', 'MS001', 'TL003', 'CV001', 4290000, 10),
    ('SP001-DEN-3U-G5', 'SP001', 'MS001', 'TL002', 'CV002', 4290000, 15),
    ('SP001-DO-4U-G5', 'SP001', 'MS003', 'TL003', 'CV002', 4290000, 8),
    ('SP001-DO-3U-G5', 'SP001', 'MS003', 'TL002', 'CV002', 4290000, 5),
    ('SP001-VANG-4U-G6', 'SP001', 'MS005', 'TL003', 'CV003', 4290000, 0),
    ('SP002-DEN-4U-G5', 'SP002', 'MS001', 'TL003', 'CV002', 2850000, 14),
    ('SP002-DEN-4U-G4', 'SP002', 'MS001', 'TL003', 'CV001', 2850000, 11),
    ('SP002-XDG-3U-G5', 'SP002', 'MS004', 'TL002', 'CV002', 2850000, 6),
    ('SP003-DEN-4U-G5', 'SP003', 'MS001', 'TL003', 'CV002', 3150000, 10),
    ('SP003-DO-3U-G5', 'SP003', 'MS003', 'TL002', 'CV002', 3150000, 7),
    ('SP004-DEN-4U-G5', 'SP004', 'MS001', 'TL003', 'CV002', 2790000, 12),
    ('SP005-TRG-4U-G5', 'SP005', 'MS002', 'TL003', 'CV002', 2350000, 9),
    ('SP006-DEN-4U-G5', 'SP006', 'MS001', 'TL003', 'CV002', 1250000, 18)
) AS b(ma, sp, ms, tl, cv, gia, ton)
JOIN san_pham p ON p.ma_san_pham = b.sp
JOIN mau_sac ms ON ms.ma_mau_sac = b.ms
JOIN trong_luong tl ON tl.ma_trong_luong = b.tl
JOIN chu_vi cv ON cv.ma_chu_vi = b.cv
WHERE NOT EXISTS (SELECT 1 FROM san_pham_chi_tiet c WHERE c.ma_spct = b.ma);
GO

-- 12. Mỗi sản phẩm chỉ có 1 biến thể cho mỗi bộ (màu sắc, trọng lượng, chu vi).
--     Backend đã kiểm tra trùng; chỉ mục này là lớp bảo vệ cuối ở mức CSDL (vd khi 2 người cùng bấm lưu một lúc).
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_spct_san_pham_mau_tl_cv' AND object_id = OBJECT_ID('san_pham_chi_tiet'))
    CREATE UNIQUE INDEX ux_spct_san_pham_mau_tl_cv
    ON san_pham_chi_tiet (id_san_pham, id_mau_sac, id_trong_luong, id_chu_vi);
GO


-- #################################################################
-- PHẦN 4. Dữ liệu mẫu: địa chỉ khách hàng, đợt giảm giá
-- (nội dung giữ nguyên từ SD_17_du_lieu_khach_hang_dot_giam_gia.sql)
-- #################################################################

-- =================================================================
-- DỮ LIỆU MẪU: địa chỉ khách hàng, 8 đợt giảm giá + biến thể áp dụng
-- Chạy SAU SD_17.sql, SD_17_du_lieu_mau.sql và SD_17_du_lieu_san_pham.sql.
-- Có thể chạy lại nhiều lần (bản ghi đã có thì bỏ qua).
-- =================================================================

USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON;
GO

-- 1. Bổ sung ngày sinh cho khách hàng mẫu (nếu chưa có)
UPDATE khach_hang SET ngay_sinh = '1995-03-12' WHERE ma_khach_hang = 'KH0001' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1992-07-25' WHERE ma_khach_hang = 'KH0002' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1998-11-02' WHERE ma_khach_hang = 'KH0003' AND ngay_sinh IS NULL;
GO

-- 2. Địa chỉ giao hàng mặc định cho khách hàng mẫu
INSERT INTO dia_chi_khach_hang (id_khach_hang, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_chi_tiet, phuong_xa, quan_huyen, tinh_thanh, la_mac_dinh, trang_thai)
SELECT k.id, k.ho_ten, k.sdt, a.dia_chi, a.phuong, a.quan, a.tinh, 1, 1
FROM (VALUES
    ('KH0001', N'12 Nguyễn Trãi',  N'Phường Thượng Đình', N'Quận Thanh Xuân', N'Thành phố Hà Nội'),
    ('KH0002', N'45 Lê Lợi',       N'Phường Bến Thành',   N'Quận 1',          N'Thành phố Hồ Chí Minh'),
    ('KH0003', N'8 Trần Phú',      N'Phường Hải Châu 1',  N'Quận Hải Châu',   N'Thành phố Đà Nẵng')
) AS a(ma, dia_chi, phuong, quan, tinh)
JOIN khach_hang k ON k.ma_khach_hang = a.ma
WHERE NOT EXISTS (SELECT 1 FROM dia_chi_khach_hang d WHERE d.id_khach_hang = k.id);
GO

-- =================================================================
-- 3. ĐỢT GIẢM GIÁ  (trang_thai: 1 = Đang hoạt động, 0 = Ngừng hoạt động)
--    bd / kt = số ngày so với hôm nay (âm = đã qua)
-- =================================================================
INSERT INTO dot_giam_gia (ma_dot, ten_dot, phan_tram_giam_dot, ngay_bat_dau, ngay_ket_thuc, ngay_tao, trang_thai)
SELECT d.ma, d.ten, d.pt,
       CAST(DATEADD(DAY, d.bd, CAST(GETDATE() AS DATE)) AS DATETIME),
       DATEADD(SECOND, 86399, CAST(DATEADD(DAY, d.kt, CAST(GETDATE() AS DATE)) AS DATETIME)),
       CAST(DATEADD(DAY, d.bd - 3, CAST(GETDATE() AS DATE)) AS DATETIME),
       d.trang_thai
FROM (VALUES
    ('DGG001', N'Sale vợt Yonex Astrox', 10, -7, 14, 1),
    ('DGG002', N'Xả hàng vợt cầu lông', 15, -40, -25, 1),
    ('DGG003', N'Black Friday vợt Lining', 20, 30, 37, 1),
    ('DGG004', N'Tuần lễ vàng Victor', 12, -3, 4, 1),
    ('DGG005', N'Mừng khai trương FootStyle', 8, -120, -90, 1),
    ('DGG006', N'Ưu đãi người mới chơi', 5, -15, 45, 0),
    ('DGG007', N'Hè sôi động', 18, -75, -50, 1),
    ('DGG008', N'Tết rộn ràng - sale lớn', 25, 80, 95, 1)
) AS d(ma, ten, pt, bd, kt, trang_thai)
WHERE NOT EXISTS (SELECT 1 FROM dot_giam_gia x WHERE x.ma_dot = d.ma);
GO

-- 4. Biến thể được giảm trong từng đợt
INSERT INTO dot_giam_gia_chi_tiet (id_san_pham_chi_tiet, id_dot_giam_gia, phan_tram_giam_bien_the, trang_thai)
SELECT c.id, d.id, d.phan_tram_giam_dot, 1
FROM (VALUES
    ('DGG001', 'SP001-DEN-4U-G5'), ('DGG001', 'SP001-DEN-3U-G5'), ('DGG001', 'SP001-DO-4U-G5'), ('DGG001', 'SP001-DO-3U-G5'),
    ('DGG002', 'SP004-DEN-4U-G5'), ('DGG002', 'SP005-TRG-4U-G5'),
    ('DGG003', 'SP002-DEN-4U-G5'), ('DGG003', 'SP002-DEN-4U-G4'), ('DGG003', 'SP002-XDG-3U-G5'),
    ('DGG004', 'SP003-DEN-4U-G5'), ('DGG004', 'SP003-DO-3U-G5'),
    ('DGG005', 'SP006-DEN-4U-G5'), ('DGG005', 'SP001-DEN-4U-G5'),
    ('DGG006', 'SP006-DEN-4U-G5'),
    ('DGG007', 'SP002-DEN-4U-G5'), ('DGG007', 'SP004-DEN-4U-G5'),
    ('DGG008', 'SP001-DEN-4U-G5'), ('DGG008', 'SP004-DEN-4U-G5'), ('DGG008', 'SP005-TRG-4U-G5')
) AS v(ma_dot, ma_spct)
JOIN dot_giam_gia d ON d.ma_dot = v.ma_dot
JOIN san_pham_chi_tiet c ON c.ma_spct = v.ma_spct
WHERE NOT EXISTS (SELECT 1 FROM dot_giam_gia_chi_tiet x WHERE x.id_dot_giam_gia = d.id AND x.id_san_pham_chi_tiet = c.id);
GO


-- #################################################################
-- PHẦN 5. Dữ liệu mẫu: phương thức thanh toán, hóa đơn
-- (nội dung giữ nguyên từ SD_17_du_lieu_hoa_don.sql)
-- #################################################################

-- =================================================================
-- DỮ LIỆU MẪU: phương thức thanh toán, hóa đơn (+ chi tiết, lịch sử, thanh toán)
-- Chạy SAU: SD_17.sql -> SD_17_du_lieu_mau.sql -> SD_17_du_lieu_san_pham.sql
-- (cần sẵn nhân viên, khách hàng, phiếu giảm giá, biến thể sản phẩm).
-- Chạy lại nhiều lần được: bản ghi đã có mã thì bỏ qua.
-- Ngày tháng tính theo hôm nay để luôn có đủ: đợt đang chạy / sắp diễn ra / đã kết thúc, hóa đơn mới / cũ.
-- =================================================================

USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON;
GO

-- 1. Hình thức thanh toán
INSERT INTO hinh_thuc_thanh_toan (ma_hinh_thuc, ten_hinh_thuc, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('HT01', N'Tiền mặt'),
    ('HT02', N'Chuyển khoản')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM hinh_thuc_thanh_toan t WHERE t.ma_hinh_thuc = v.ma);
GO

-- 2. Phương thức thanh toán
INSERT INTO phuong_thuc_thanh_toan (id_hinh_thuc_thanh_toan, ma_phuong_thuc, ten_phuong_thuc, trang_thai)
SELECT h.id, v.ma, v.ten, 1
FROM (VALUES
    ('PT01', 'HT01', N'Tiền mặt'),
    ('PT02', 'HT02', N'Chuyển khoản ngân hàng'),
    ('PT03', 'HT01', N'Thanh toán khi nhận hàng (COD)'),
    ('PT04', 'HT02', N'Ví điện tử VNPay')
) AS v(ma, ht, ten)
JOIN hinh_thuc_thanh_toan h ON h.ma_hinh_thuc = v.ht
WHERE NOT EXISTS (SELECT 1 FROM phuong_thuc_thanh_toan t WHERE t.ma_phuong_thuc = v.ma);
GO

-- =================================================================
-- 5. HÓA ĐƠN
--    loai: 1 = Tại quầy, 2 = Giao hàng
--    trang_thai: 0 Đã hủy, 1 Chờ xác nhận, 2 Đã xác nhận, 3 Chờ giao hàng, 4 Đang giao hàng, 5 Đã giao hàng, 6 Đã hoàn thành
--    phut = thời điểm tạo, tính bằng phút so với lúc chạy file (âm = trước đó)
--    buoc_huy = đơn hủy: đã đi tới bước nào trước khi hủy
--    Tổng tiền, tiền giảm, thành tiền được tính lại ở bước 7 từ các dòng sản phẩm.
-- =================================================================
IF OBJECT_ID('tempdb..#hd') IS NOT NULL DROP TABLE #hd;
CREATE TABLE #hd (
    ma VARCHAR(50), ma_nv VARCHAR(50), ma_kh VARCHAR(50), ma_phieu VARCHAR(50), ma_pt VARCHAR(50),
    loai TINYINT, phi_ship DECIMAL(18,0), don_vi NVARCHAR(255), nguoi_nhan NVARCHAR(255), sdt VARCHAR(15),
    dia_chi NVARCHAR(500), ghi_chu NVARCHAR(500), phut INT, trang_thai TINYINT, buoc_huy TINYINT, ly_do_huy NVARCHAR(500)
);
INSERT INTO #hd VALUES
    ('HD000001', 'NV0003', NULL,     NULL,        'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -120, 6, NULL, NULL),
    ('HD000002', 'NV0004', 'KH0002', 'VCH7QX2YN', 'PT02', 1, 0,     NULL, NULL, NULL, NULL, NULL, -1500, 6, NULL, NULL),
    ('HD000003', 'NV0003', 'KH0001', 'VCHTT50KH', 'PT03', 2, 30000, N'Giao Hàng Nhanh', N'Nguyễn Thị Lan', '0901000001',
        N'Số 12 ngõ 45 Nguyễn Chí Thanh, phường Láng Thượng, quận Đống Đa, Hà Nội', N'Gọi trước khi giao', -60, 1, NULL, NULL),
    ('HD000004', 'NV0006', 'KH0003', NULL,        'PT04', 2, 35000, N'Giao Hàng Tiết Kiệm', N'Lê Minh Tuấn', '0901000003',
        N'Số 88 Lê Lợi, phường Bến Nghé, quận 1, TP. Hồ Chí Minh', NULL, -1440, 2, NULL, NULL),
    ('HD000005', 'NV0009', 'KH0005', 'VCHFS500K', 'PT03', 2, 30000, N'Giao Hàng Nhanh', N'Hoàng Đức Anh', '0901000005',
        N'Số 5 Trần Đại Nghĩa, phường Bách Khoa, quận Hai Bà Trưng, Hà Nội', N'Giao giờ hành chính', -2880, 3, NULL, NULL),
    ('HD000006', 'NV0003', 'KH0004', 'VCH7QX2YN', 'PT02', 2, 40000, N'Viettel Post', N'Phạm Thu Hằng', '0901000004',
        N'Số 21 Hùng Vương, phường Thạch Thang, quận Hải Châu, Đà Nẵng', NULL, -4320, 4, NULL, NULL),
    ('HD000007', 'NV0004', 'KH0006', NULL,        'PT03', 2, 30000, N'Giao Hàng Tiết Kiệm', N'Vũ Khánh Linh', '0901000006',
        N'Số 7 Lạch Tray, phường Lạch Tray, quận Ngô Quyền, Hải Phòng', NULL, -5760, 5, NULL, NULL),
    ('HD000008', 'NV0006', 'KH0002', 'VCH7QX2YN', 'PT03', 2, 0,     N'Giao Hàng Nhanh', N'Trần Văn Hùng', '0901000002',
        N'Số 102 Cầu Giấy, phường Quan Hoa, quận Cầu Giấy, Hà Nội', N'Miễn phí vận chuyển', -8640, 6, NULL, NULL),
    ('HD000009', 'NV0009', 'KH0003', NULL,        'PT03', 2, 30000, N'Giao Hàng Nhanh', N'Lê Minh Tuấn', '0901000003',
        N'Số 88 Lê Lợi, phường Bến Nghé, quận 1, TP. Hồ Chí Minh', NULL, -2700, 0, 2, N'Khách đổi ý, muốn đổi sang mẫu vợt khác'),
    ('HD000010', 'NV0003', 'KH0005', NULL,        'PT03', 2, 30000, N'Viettel Post', N'Hoàng Đức Anh', '0901000005',
        N'Số 5 Trần Đại Nghĩa, phường Bách Khoa, quận Hai Bà Trưng, Hà Nội', NULL, -7200, 0, 1, N'Không liên lạc được với khách hàng'),
    ('HD000011', 'NV0001', NULL,     'VCH7QX2YN', 'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -11520, 6, NULL, NULL),
    ('HD000012', 'NV0004', 'KH0001', 'VCHPK20QC', 'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -43200, 6, NULL, NULL),
    ('HD000013', 'NV0006', 'KH0004', NULL,        'PT02', 2, 0,     N'Giao Hàng Nhanh', N'Phạm Thu Hằng', '0901000004',
        N'Số 21 Hùng Vương, phường Thạch Thang, quận Hải Châu, Đà Nẵng', N'Mua 2 cây tặng bạn', -50400, 6, NULL, NULL),
    ('HD000014', 'NV0009', 'KH0006', 'VCHHE12XN', 'PT02', 1, 0,     NULL, NULL, NULL, NULL, NULL, -93600, 6, NULL, NULL),
    ('HD000015', 'NV0003', NULL,     NULL,        'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -144000, 6, NULL, NULL),
    ('HD000016', 'NV0001', NULL,     NULL,        'PT01', 1, 0,     NULL, NULL, NULL, NULL, N'Khách đang chờ lấy tiền', -10, 1, NULL, NULL),
    ('HD000017', 'NV0004', 'KH0002', 'VCHFS500K', 'PT04', 2, 0,     N'Giao Hàng Nhanh', N'Trần Văn Hùng', '0901000002',
        N'Số 102 Cầu Giấy, phường Quan Hoa, quận Cầu Giấy, Hà Nội', NULL, -180, 1, NULL, NULL),
    ('HD000018', 'NV0009', 'KH0003', NULL,        'PT03', 2, 30000, N'Giao Hàng Tiết Kiệm', N'Lê Minh Tuấn', '0901000003',
        N'Số 88 Lê Lợi, phường Bến Nghé, quận 1, TP. Hồ Chí Minh', NULL, -21600, 6, NULL, NULL);

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
LEFT JOIN phieu_giam_gia p ON p.ma_phieu = h.ma_phieu
WHERE NOT EXISTS (SELECT 1 FROM hoa_don x WHERE x.ma_hoa_don = h.ma);

-- =================================================================
-- 6. Chi tiết hóa đơn. giam = % đợt giảm giá đang áp dụng lúc mua (0 = không giảm)
--    don_gia = giá bán biến thể sau khi trừ % đợt giảm giá.
-- =================================================================
INSERT INTO hoa_don_chi_tiet (id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia, thanh_tien, trang_thai)
SELECT hd.id, c.id, v.sl,
       ROUND(c.gia_ban * (100 - v.giam) / 100.0, 0),
       ROUND(c.gia_ban * (100 - v.giam) / 100.0, 0) * v.sl,
       1
FROM (VALUES
    ('HD000001', 'SP006-DEN-4U-G5', 1, 0),
    ('HD000002', 'SP001-DEN-4U-G5', 1, 10),
    ('HD000003', 'SP002-XDG-3U-G5', 1, 0),
    ('HD000003', 'SP006-DEN-4U-G5', 1, 0),
    ('HD000004', 'SP003-DEN-4U-G5', 1, 12),
    ('HD000005', 'SP004-DEN-4U-G5', 1, 0),
    ('HD000005', 'SP006-DEN-4U-G5', 2, 0),
    ('HD000006', 'SP001-DO-4U-G5', 1, 10),
    ('HD000007', 'SP005-TRG-4U-G5', 1, 0),
    ('HD000008', 'SP002-DEN-4U-G5', 1, 0),
    ('HD000008', 'SP003-DO-3U-G5', 1, 0),
    ('HD000009', 'SP006-DEN-4U-G5', 1, 0),
    ('HD000010', 'SP005-TRG-4U-G5', 1, 0),
    ('HD000011', 'SP003-DEN-4U-G5', 1, 0),
    ('HD000011', 'SP006-DEN-4U-G5', 1, 0),
    ('HD000012', 'SP004-DEN-4U-G5', 1, 15),
    ('HD000012', 'SP005-TRG-4U-G5', 1, 15),
    ('HD000013', 'SP002-XDG-3U-G5', 2, 0),
    ('HD000014', 'SP002-DEN-4U-G5', 1, 18),
    ('HD000015', 'SP006-DEN-4U-G5', 2, 8),
    ('HD000015', 'SP001-DEN-4U-G5', 1, 8),
    ('HD000016', 'SP001-DEN-3U-G5', 1, 10),
    ('HD000017', 'SP001-DEN-4U-G5', 1, 10),
    ('HD000017', 'SP003-DO-3U-G5', 1, 12),
    ('HD000018', 'SP006-DEN-4U-G5', 1, 0)
) AS v(ma_hd, ma_spct, sl, giam)
JOIN hoa_don hd ON hd.ma_hoa_don = v.ma_hd
JOIN san_pham_chi_tiet c ON c.ma_spct = v.ma_spct
WHERE NOT EXISTS (SELECT 1 FROM hoa_don_chi_tiet x WHERE x.id_hoa_don = hd.id AND x.id_san_pham_chi_tiet = c.id);

-- =================================================================
-- 7. Tính lại tổng tiền / tiền giảm (theo phiếu giảm giá) / thành tiền từ chi tiết
--    Phiếu %: giảm = tổng * % (không quá giam_toi_da); phiếu tiền: giảm = giá trị phiếu.
--    Đơn chưa đạt giá trị tối thiểu của phiếu thì không giảm.
-- =================================================================
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

-- =================================================================
-- 8. Lịch sử hóa đơn (dòng thời gian trên trang chi tiết)
--    Chữ ở cột hanh_dong phải khớp với backend: Tạo đơn hàng, Đã xác nhận, Chờ giao hàng,
--    Đang giao hàng, Đã giao hàng, Đã hoàn thành, Đã hủy.
-- =================================================================
IF OBJECT_ID('tempdb..#buoc') IS NOT NULL DROP TABLE #buoc;
CREATE TABLE #buoc (code TINYINT, hanh_dong NVARCHAR(100), phut INT, mo_ta NVARCHAR(255));
INSERT INTO #buoc VALUES
    (1, N'Tạo đơn hàng', 0, N'Khách đặt hàng online'),
    (2, N'Đã xác nhận', 30, N'Nhân viên đã gọi xác nhận đơn với khách'),
    (3, N'Chờ giao hàng', 180, N'Đã đóng gói, chờ đơn vị vận chuyển đến lấy'),
    (4, N'Đang giao hàng', 1440, N'Đã bàn giao cho đơn vị vận chuyển'),
    (5, N'Đã giao hàng', 4320, N'Giao hàng thành công'),
    (6, N'Đã hoàn thành', 4380, N'Đơn hàng hoàn thành');

-- Đơn giao hàng: các bước đã đi qua (đơn hủy: tới bước buoc_huy)
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
WHERE h.loai = 2
  AND NOT EXISTS (SELECT 1 FROM lich_su_hoa_don x WHERE x.id_hoa_don = hd.id);

-- Đơn giao hàng bị hủy: thêm bước "Đã hủy" (1 tiếng sau bước cuối)
INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_thao_tac, hanh_dong, thoi_gian, mo_ta)
SELECT hd.id, nv.ho_ten, N'Đã hủy', DATEADD(MINUTE, b.phut + 60, hd.ngay_tao), h.ly_do_huy
FROM hoa_don hd
JOIN #hd h ON h.ma = hd.ma_hoa_don
LEFT JOIN nhan_vien nv ON nv.id = hd.id_nhan_vien
JOIN #buoc b ON b.code = h.buoc_huy
WHERE h.trang_thai = 0
  AND NOT EXISTS (SELECT 1 FROM lich_su_hoa_don x WHERE x.id_hoa_don = hd.id AND x.hanh_dong = N'Đã hủy');

-- Đơn tại quầy: tạo đơn, (thanh toán xong) hoàn thành sau 3 phút
INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_thao_tac, hanh_dong, thoi_gian, mo_ta)
SELECT hd.id, nv.ho_ten, s.hanh_dong, DATEADD(MINUTE, s.phut, hd.ngay_tao), s.mo_ta
FROM hoa_don hd
JOIN #hd h ON h.ma = hd.ma_hoa_don
LEFT JOIN nhan_vien nv ON nv.id = hd.id_nhan_vien
JOIN (VALUES
    (1, N'Tạo đơn hàng', 0, N'Tạo hóa đơn tại quầy'),
    (6, N'Đã hoàn thành', 3, N'Khách đã thanh toán, nhận hàng tại cửa hàng')
) AS s(code, hanh_dong, phut, mo_ta) ON s.code <= h.trang_thai
WHERE h.loai = 1
  AND NOT EXISTS (SELECT 1 FROM lich_su_hoa_don x WHERE x.id_hoa_don = hd.id AND x.hanh_dong = s.hanh_dong);

-- Ngày cập nhật = lần thao tác cuối
UPDATE hd
SET ngay_cap_nhat = ls.thoi_gian_cuoi
FROM hoa_don hd
JOIN #hd h ON h.ma = hd.ma_hoa_don
JOIN (SELECT id_hoa_don, MAX(thoi_gian) AS thoi_gian_cuoi FROM lich_su_hoa_don GROUP BY id_hoa_don) ls ON ls.id_hoa_don = hd.id;

-- =================================================================
-- 9. Lịch sử thanh toán
-- =================================================================
-- Tại quầy đã hoàn thành: trả đủ ngay tại cửa hàng
-- Giao hàng trả trước (chuyển khoản / VNPay): trả đủ ngay sau khi đặt
-- Giao hàng COD đã hoàn thành: thu tiền lúc giao
INSERT INTO lich_su_thanh_toan (id_hoa_don, so_tien, ma_giao_dich, thoi_gian, trang_thai, mo_ta)
SELECT hd.id, hd.thanh_tien,
       CASE h.ma_pt WHEN 'PT02' THEN 'CK' + RIGHT(h.ma, 6) + '01' WHEN 'PT04' THEN 'VNP' + RIGHT(h.ma, 6) + '88' END,
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
WHERE ((h.loai = 1 AND h.trang_thai = 6)
       OR (h.loai = 2 AND h.ma_pt IN ('PT02', 'PT04') AND h.trang_thai <> 0)
       OR (h.loai = 2 AND h.ma_pt = 'PT03' AND h.trang_thai = 6))
  AND NOT EXISTS (SELECT 1 FROM lich_su_thanh_toan x WHERE x.id_hoa_don = hd.id);

-- =================================================================
-- 10. Cập nhật phiếu giảm giá đã dùng trong các hóa đơn trên
-- =================================================================
UPDATE p
SET so_luong_da_dung = (SELECT COUNT(*) FROM hoa_don x WHERE x.id_phieu_giam_gia = p.id AND x.trang_thai <> 0)
FROM phieu_giam_gia p
WHERE p.ma_phieu IN (SELECT ma_phieu FROM #hd WHERE ma_phieu IS NOT NULL);

-- Phiếu cá nhân: ghi ngày khách đã dùng phiếu
UPDATE l
SET ngay_su_dung = hd.ngay_tao
FROM phieu_giam_gia_khach_hang l
JOIN hoa_don hd ON hd.id_phieu_giam_gia = l.id_phieu_giam_gia AND hd.id_khach_hang = l.id_khach_hang
JOIN #hd h ON h.ma = hd.ma_hoa_don
WHERE l.ngay_su_dung IS NULL AND hd.trang_thai <> 0;

DROP TABLE #buoc;
DROP TABLE #hd;
GO


-- #################################################################
-- PHẦN 6. Nâng cấp CSDL cũ (chạy lại nhiều lần được, CSDL mới tạo thì không làm gì)
-- (nội dung giữ nguyên từ SD_17_nang_cap.sql)
-- #################################################################

-- =================================================================
-- NÂNG CẤP CSDL ĐÃ CHẠY TRƯỚC ĐÓ (không cần chạy nếu vừa tạo mới bằng SD_17.sql bản mới)
-- Có thể chạy lại nhiều lần. Thêm các chỉ mục chống trùng ở mức CSDL, cột số căn cước (nhan_vien.cccd), bảng vị trí làm việc
-- (vi_tri, nhan_vien_vi_tri) và bảng dia_chi_nhan_vien (nhân viên có nhiều địa chỉ, địa chỉ cũ được chuyển thành địa chỉ chính).
-- Nếu báo lỗi "duplicate key" nghĩa là đang có nhân viên trùng email / SĐT: sửa dữ liệu trùng rồi chạy lại.
-- =================================================================

USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_email' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_email ON nhan_vien (email) WHERE email IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_sdt' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_sdt ON nhan_vien (sdt) WHERE sdt IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_spct_san_pham_mau_tl_cv' AND object_id = OBJECT_ID('san_pham_chi_tiet'))
    CREATE UNIQUE INDEX ux_spct_san_pham_mau_tl_cv ON san_pham_chi_tiet (id_san_pham, id_mau_sac, id_trong_luong, id_chu_vi);
GO

-- Địa chỉ khách hàng: thêm ngày tạo / ngày cập nhật (module Khách hàng cần hai cột này).
IF COL_LENGTH('dia_chi_khach_hang', 'ngay_tao') IS NULL
    ALTER TABLE dia_chi_khach_hang ADD ngay_tao DATETIME NULL CONSTRAINT df_dckh_ngay_tao DEFAULT CURRENT_TIMESTAMP;
GO

IF COL_LENGTH('dia_chi_khach_hang', 'ngay_cap_nhat') IS NULL
    ALTER TABLE dia_chi_khach_hang ADD ngay_cap_nhat DATETIME NULL CONSTRAINT df_dckh_ngay_cap_nhat DEFAULT CURRENT_TIMESTAMP;
GO

-- Đợt giảm giá: thêm cột mô tả (trang "Thêm đợt giảm giá" có ô Mô tả).
IF COL_LENGTH('dot_giam_gia', 'mo_ta') IS NULL
    ALTER TABLE dot_giam_gia ADD mo_ta NVARCHAR(500) NULL;
GO

-- Cửa hàng chỉ bán vợt cầu lông: đổi dữ liệu mẫu giày cũ (nếu có) thành vợt. Không đổi giá, không đổi hóa đơn cũ.
UPDATE danh_muc SET ten_danh_muc = N'Vợt cầu lông người mới'
WHERE ma_danh_muc = 'DM002' AND ten_danh_muc = N'Giày cầu lông';
GO

UPDATE san_pham
SET ten_san_pham = N'Yonex Nanoflare 800 Pro',
    mo_ta = N'Vợt thiên tốc độ, khung nhẹ, phản hồi nhanh.',
    id_danh_muc = (SELECT id FROM danh_muc WHERE ma_danh_muc = 'DM001'),
    id_chat_lieu = (SELECT id FROM chat_lieu WHERE ma_chat_lieu = 'CL001')
WHERE ma_san_pham = 'SP004' AND ten_san_pham = N'Yonex Power Cushion 65Z3';
GO

UPDATE san_pham
SET ten_san_pham = N'Mizuno Fortius 10 Quick',
    mo_ta = N'Vợt nhẹ, linh hoạt, dễ điều cầu.',
    id_danh_muc = (SELECT id FROM danh_muc WHERE ma_danh_muc = 'DM001'),
    id_chat_lieu = (SELECT id FROM chat_lieu WHERE ma_chat_lieu = 'CL001')
WHERE ma_san_pham = 'SP005' AND ten_san_pham = N'Mizuno Wave Fang Pro';
GO

-- Chỉ đổi mã khi mã mới CHƯA có (nếu dữ liệu vợt mới đã được nạp trước đó thì mã mới đã tồn tại, đổi nữa sẽ trùng khóa UNIQUE).
UPDATE san_pham_chi_tiet
SET ma_spct = 'SP004-DEN-4U-G5',
    id_trong_luong = (SELECT id FROM trong_luong WHERE ma_trong_luong = 'TL003'),
    id_chu_vi = (SELECT id FROM chu_vi WHERE ma_chu_vi = 'CV002')
WHERE ma_spct = 'SP004-DEN-40'
  AND NOT EXISTS (SELECT 1 FROM san_pham_chi_tiet x WHERE x.ma_spct = 'SP004-DEN-4U-G5');
GO

-- Biến thể giày cũ còn sót lại khi mã mới đã có: ngưng hoạt động (giữ nguyên để không mất lịch sử hóa đơn cũ, không hiện trong danh sách đang bán).
UPDATE san_pham_chi_tiet SET trang_thai = 0
WHERE ma_spct = 'SP004-DEN-40'
  AND EXISTS (SELECT 1 FROM san_pham_chi_tiet x WHERE x.ma_spct = 'SP004-DEN-4U-G5');
GO

-- Chỉ đổi mã khi mã mới CHƯA có (nếu dữ liệu vợt mới đã được nạp trước đó thì mã mới đã tồn tại, đổi nữa sẽ trùng khóa UNIQUE).
UPDATE san_pham_chi_tiet
SET ma_spct = 'SP005-TRG-4U-G5',
    id_trong_luong = (SELECT id FROM trong_luong WHERE ma_trong_luong = 'TL003'),
    id_chu_vi = (SELECT id FROM chu_vi WHERE ma_chu_vi = 'CV002')
WHERE ma_spct = 'SP005-TRG-40'
  AND NOT EXISTS (SELECT 1 FROM san_pham_chi_tiet x WHERE x.ma_spct = 'SP005-TRG-4U-G5');
GO

-- Biến thể giày cũ còn sót lại khi mã mới đã có: ngưng hoạt động (giữ nguyên để không mất lịch sử hóa đơn cũ, không hiện trong danh sách đang bán).
UPDATE san_pham_chi_tiet SET trang_thai = 0
WHERE ma_spct = 'SP005-TRG-40'
  AND EXISTS (SELECT 1 FROM san_pham_chi_tiet x WHERE x.ma_spct = 'SP005-TRG-4U-G5');
GO

UPDATE dot_giam_gia SET ten_dot = N'Xả hàng vợt cầu lông'
WHERE ma_dot = 'DGG002' AND ten_dot = N'Xả hàng giày cầu lông';
GO

-- Chất liệu "Vải lưới" là của giày cũ, đổi thành chất liệu khung vợt (sản phẩm đang dùng chất liệu này vẫn giữ nguyên liên kết).
UPDATE chat_lieu SET ten_chat_lieu = N'Titanium'
WHERE ma_chat_lieu = 'CL004' AND ten_chat_lieu = N'Vải lưới';
GO

-- Nhân viên: thêm số căn cước công dân (quét QR trên thẻ CCCD hoặc nhập tay) + chống trùng.
IF COL_LENGTH('nhan_vien', 'cccd') IS NULL
    ALTER TABLE nhan_vien ADD cccd VARCHAR(12) NULL;
GO

-- Bản trước đặt tên cột số căn cước là can_cuoc_cong_dan: nếu CSDL đang có cột đó thì chép số sang cột cccd rồi bỏ chỉ mục cũ
-- (cột cũ được giữ lại, không xóa dữ liệu; backend chỉ đọc / ghi cột cccd).
IF COL_LENGTH('nhan_vien', 'can_cuoc_cong_dan') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.indexes i
               JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
               JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
               WHERE i.name = 'ux_nhan_vien_cccd' AND i.object_id = OBJECT_ID('nhan_vien') AND c.name = 'can_cuoc_cong_dan')
        DROP INDEX ux_nhan_vien_cccd ON nhan_vien;
    EXEC('UPDATE nhan_vien SET cccd = can_cuoc_cong_dan WHERE cccd IS NULL AND can_cuoc_cong_dan IS NOT NULL');
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_cccd' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (cccd) WHERE cccd IS NOT NULL;
GO

-- Nhân viên: vị trí làm việc (nhiều vị trí / nhân viên, chỉ thêm và chọn, không xóa).
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

INSERT INTO vi_tri (ten_vi_tri, trang_thai)
SELECT v.ten, 1 FROM (VALUES (N'Bán hàng tại quầy'), (N'Thu ngân'), (N'Kho'), (N'Giao hàng'), (N'Chăm sóc khách hàng')) AS v(ten)
WHERE NOT EXISTS (SELECT 1 FROM vi_tri t WHERE t.ten_vi_tri = v.ten);
GO

-- Nhân viên: nhiều địa chỉ (chọn 1 địa chỉ chính; không xóa, chỉ thêm / sửa / chọn lại).
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

-- Chuyển địa chỉ cũ (cột nhan_vien.dia_chi) của từng nhân viên thành địa chỉ chính trong bảng mới (chạy lại không bị nhân đôi).
INSERT INTO dia_chi_nhan_vien (id_nhan_vien, dia_chi_chi_tiet, la_mac_dinh)
SELECT n.id, n.dia_chi, 1
FROM nhan_vien n
WHERE n.dia_chi IS NOT NULL AND LTRIM(RTRIM(n.dia_chi)) <> ''
  AND NOT EXISTS (SELECT 1 FROM dia_chi_nhan_vien d WHERE d.id_nhan_vien = n.id);
GO

-- Danh mục không phải vợt: thêm mục "Không áp dụng" cho độ cứng / điểm cân bằng / trọng lượng / chu vi (chạy lại nhiều lần được).
INSERT INTO do_cung (ma_do_cung, ten_do_cung, trang_thai)
SELECT 'DC000', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM do_cung t WHERE t.ma_do_cung = 'DC000');
INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai)
SELECT 'CB000', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM diem_can_bang t WHERE t.ma_diem_can_bang = 'CB000');
INSERT INTO trong_luong (ma_trong_luong, ten_trong_luong, trang_thai)
SELECT 'TL000', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM trong_luong t WHERE t.ma_trong_luong = 'TL000');
INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai)
SELECT 'CV000', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM chu_vi t WHERE t.ma_chu_vi = 'CV000');
GO

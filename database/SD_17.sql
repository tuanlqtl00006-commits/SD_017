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
    anh_dai_dien NVARCHAR(500), -- ảnh đại diện (đường dẫn /api/uploads/... hoặc link http), có thể để trống
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
    can_cuoc_cong_dan VARCHAR(12), -- số căn cước công dân (12 số), nhập tay hoặc quét mã QR trên thẻ
    ngay_vao_lam DATE,
    ngay_cap_nhat DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai TINYINT DEFAULT 1,
    FOREIGN KEY (id_vai_tro) REFERENCES vai_tro(id)
);

-- Email và số điện thoại nhân viên không được trùng (bỏ qua giá trị NULL). Backend đã kiểm tra; đây là lớp bảo vệ cuối ở mức CSDL.
CREATE UNIQUE INDEX ux_nhan_vien_email ON nhan_vien (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX ux_nhan_vien_sdt ON nhan_vien (sdt) WHERE sdt IS NOT NULL;
CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (can_cuoc_cong_dan) WHERE can_cuoc_cong_dan IS NOT NULL;

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

-- Giai đoạn 3: ca làm việc, lịch làm việc, giao ca (hoa_don.id_giao_ca trỏ tới giao_ca nên phải tạo trước hoa_don)
-- Ca làm việc (định nghĩa ca: Ca sáng 07:00 - 12:00...). Giờ bắt đầu < giờ kết thúc (không có ca qua đêm).
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

-- Lịch làm việc: nhân viên nào làm ca nào ngày nào. Một người không có 2 dòng cùng ca cùng ngày.
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

-- Giao ca: một lần nhân viên nhận ca (tiền đầu ca) -> bán hàng -> kết ca (đếm tiền, bàn giao cho ca sau).
-- trang_thai: 1 = đang trong ca, 2 = đã giao ca. Mỗi nhân viên chỉ có 1 ca đang mở (chỉ mục ux_giao_ca_dang_mo).
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

CREATE UNIQUE INDEX ux_giao_ca_dang_mo ON giao_ca (id_nhan_vien) WHERE trang_thai = 1;

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
    id_giao_ca INT NULL, -- giai đoạn 3: hóa đơn bán tại quầy thuộc ca nào (để tổng kết khi giao ca)
    FOREIGN KEY (id_nhan_vien) REFERENCES nhan_vien(id),
    FOREIGN KEY (id_khach_hang) REFERENCES khach_hang(id),
    FOREIGN KEY (id_phieu_giam_gia) REFERENCES phieu_giam_gia(id),
    FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id),
    FOREIGN KEY (id_giao_ca) REFERENCES giao_ca(id)
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
    nguoi_nhan NVARCHAR(255) NULL,
    id_phuong_thuc_thanh_toan INT NULL, -- giai đoạn 3: lần trả tiền này bằng phương thức nào (tiền mặt / chuyển khoản)
    FOREIGN KEY (id_hoa_don) REFERENCES hoa_don(id),
    FOREIGN KEY (id_phuong_thuc_thanh_toan) REFERENCES phuong_thuc_thanh_toan(id)
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

-- =================================================================
-- PHẦN 5: CHAT (GIAI ĐOẠN 3) - khách hỏi cửa hàng (trợ lý ảo + nhân viên) và chat nội bộ giữa nhân viên
-- =================================================================

-- Chat. loai: 1 = khách hỏi cửa hàng (hỗ trợ trực tuyến), 2 = nội bộ 1-1, 3 = nội bộ nhóm.
-- che_do (chỉ hội thoại hỗ trợ): 1 = trợ lý ảo đang trả lời, 2 = chờ nhân viên, 3 = nhân viên đang hỗ trợ, 4 = đã đóng.
-- ma_truy_cap: chuỗi bí mật cấp cho khách khi bắt đầu chat (khách không đăng nhập vẫn chỉ đọc được hội thoại của mình).
-- khoa_rieng: hội thoại 1-1 giữa 2 nhân viên "idNho-idLon" để không tạo trùng.
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

CREATE UNIQUE INDEX ux_hoi_thoai_khoa_rieng ON hoi_thoai (khoa_rieng) WHERE khoa_rieng IS NOT NULL;

-- Thành viên của hội thoại nội bộ + mốc đã đọc (id tin nhắn cuối đã đọc) của từng nhân viên.
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

-- loai_nguoi_gui: 1 = khách, 2 = nhân viên, 3 = trợ lý ảo, 4 = thông báo hệ thống
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

CREATE INDEX ix_tin_nhan_hoi_thoai ON tin_nhan (id_hoi_thoai, id);


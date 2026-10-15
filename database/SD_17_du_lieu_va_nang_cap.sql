-- =================================================================
-- FILE SQL GỘP: DỮ LIỆU MẪU + NÂNG CẤP  (chạy SAU database/SD_17.sql)
-- =================================================================
-- Cách chạy (CSDL mới):
--   1. Mở và chạy SD_17.sql MỘT LẦN (tạo database shop_cau_long_footstyle + toàn bộ bảng). File đó giữ nguyên, không sửa.
--   2. Mở và chạy file này MỘT LẦN (F5). Có thể chạy lại nhiều lần: bản ghi đã có mã thì bỏ qua, lệnh nâng cấp tự bỏ qua nếu CSDL đã đủ.
-- CSDL đã có từ trước: không chạy lại SD_17.sql, chỉ cần chạy file này (hoặc khởi động lại backend, backend tự bổ sung phần còn thiếu).
--
-- Mã dùng dạng gọn: tiền tố + số tối thiểu 2 chữ số (HD01, NV01, SP01, DGG01 ... HD100 khi quá 99), không còn HD000001.
-- File này là 5 file cũ nối lại theo đúng thứ tự chạy, nội dung từng file giữ nguyên:
--   PHẦN 0 = rút gọn mã cũ nhiều số 0 (HD000001 -> HD01 ...), mới thêm, chạy trước các phần còn lại
--   PHẦN 1 = SD_17_du_lieu_mau.sql
--   PHẦN 2 = SD_17_du_lieu_san_pham.sql
--   PHẦN 3 = SD_17_du_lieu_khach_hang_dot_giam_gia.sql
--   PHẦN 4 = SD_17_du_lieu_hoa_don.sql
--   PHẦN 5 = SD_17_nang_cap.sql
--   PHẦN 6 = ảnh sản phẩm theo màu; PHẦN 7 = khách hàng không trùng email / số điện thoại
--   PHẦN 8 = hóa đơn: dọn phí vận chuyển của đơn tại quầy + hóa đơn mẫu "Giao hàng thất bại" (HD19)
-- Các dòng ghi chú bên trong từng phần còn nhắc tên file cũ ở trên: chỉ là chú thích, hiện chỉ dùng SD_17.sql + file này.
-- Mật khẩu của mọi nhân viên mẫu: Footstyle@123
-- =================================================================

USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON;
GO

-- #################################################################
-- PHẦN 0: RÚT GỌN MÃ CŨ NHIỀU SỐ 0 (HD000001 -> HD01, NV0001 -> NV01, SP001 -> SP01, DGG001 -> DGG01 ...)
-- Quy tắc mã: TIỀN TỐ + số thứ tự tối thiểu 2 chữ số, quá 99 thì viết thẳng (HD100), không đệm thêm số 0.
-- Chỉ đổi cột mã (các bảng liên kết với nhau bằng id nên không mất liên kết). Chạy lại nhiều lần vẫn an toàn:
-- mã đã gọn thì không đổi; mã gọn sẽ trùng mã đang có thì giữ nguyên mã cũ. Backend cũng tự làm việc này khi khởi động.
-- Phải chạy TRƯỚC các phần dữ liệu mẫu bên dưới để dữ liệu mẫu (mã gọn) không bị thêm trùng với dữ liệu cũ (mã dài).
-- #################################################################

IF OBJECT_ID('dbo.fn_rut_gon_ma', 'FN') IS NOT NULL DROP FUNCTION dbo.fn_rut_gon_ma;
GO

-- 'HD000001' -> 'HD01', 'NV0010' -> 'NV10', 'DC000' -> 'DC00'. Mã không có dạng "chữ cái + số" thì giữ nguyên.
CREATE FUNCTION dbo.fn_rut_gon_ma (@ma VARCHAR(50))
RETURNS VARCHAR(50)
AS
BEGIN
    DECLARE @vt INT = PATINDEX('%[0-9]%', @ma);
    IF @ma IS NULL OR @vt <= 1 RETURN @ma;
    DECLARE @chu VARCHAR(50) = LEFT(@ma, @vt - 1);
    DECLARE @so VARCHAR(50) = SUBSTRING(@ma, @vt, 50);
    IF @chu LIKE '%[^A-Za-z]%' OR @so LIKE '%[^0-9]%' OR LEN(@so) > 9 RETURN @ma;
    DECLARE @n INT = CAST(@so AS INT);
    RETURN @chu + CASE WHEN @n < 10 THEN '0' + CAST(@n AS VARCHAR(10)) ELSE CAST(@n AS VARCHAR(10)) END;
END
GO

UPDATE x SET x.ma_nhan_vien = dbo.fn_rut_gon_ma(x.ma_nhan_vien)
FROM nhan_vien x
WHERE dbo.fn_rut_gon_ma(x.ma_nhan_vien) <> x.ma_nhan_vien
  AND NOT EXISTS (SELECT 1 FROM nhan_vien y WHERE y.ma_nhan_vien = dbo.fn_rut_gon_ma(x.ma_nhan_vien))
  AND NOT EXISTS (SELECT 1 FROM nhan_vien z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_nhan_vien) = dbo.fn_rut_gon_ma(x.ma_nhan_vien));

UPDATE x SET x.ma_khach_hang = dbo.fn_rut_gon_ma(x.ma_khach_hang)
FROM khach_hang x
WHERE dbo.fn_rut_gon_ma(x.ma_khach_hang) <> x.ma_khach_hang
  AND NOT EXISTS (SELECT 1 FROM khach_hang y WHERE y.ma_khach_hang = dbo.fn_rut_gon_ma(x.ma_khach_hang))
  AND NOT EXISTS (SELECT 1 FROM khach_hang z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_khach_hang) = dbo.fn_rut_gon_ma(x.ma_khach_hang));

UPDATE x SET x.ma_hoa_don = dbo.fn_rut_gon_ma(x.ma_hoa_don)
FROM hoa_don x
WHERE dbo.fn_rut_gon_ma(x.ma_hoa_don) <> x.ma_hoa_don
  AND NOT EXISTS (SELECT 1 FROM hoa_don y WHERE y.ma_hoa_don = dbo.fn_rut_gon_ma(x.ma_hoa_don))
  AND NOT EXISTS (SELECT 1 FROM hoa_don z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_hoa_don) = dbo.fn_rut_gon_ma(x.ma_hoa_don));

UPDATE x SET x.ma_dot = dbo.fn_rut_gon_ma(x.ma_dot)
FROM dot_giam_gia x
WHERE dbo.fn_rut_gon_ma(x.ma_dot) <> x.ma_dot
  AND NOT EXISTS (SELECT 1 FROM dot_giam_gia y WHERE y.ma_dot = dbo.fn_rut_gon_ma(x.ma_dot))
  AND NOT EXISTS (SELECT 1 FROM dot_giam_gia z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_dot) = dbo.fn_rut_gon_ma(x.ma_dot));

UPDATE x SET x.ma_san_pham = dbo.fn_rut_gon_ma(x.ma_san_pham)
FROM san_pham x
WHERE dbo.fn_rut_gon_ma(x.ma_san_pham) <> x.ma_san_pham
  AND NOT EXISTS (SELECT 1 FROM san_pham y WHERE y.ma_san_pham = dbo.fn_rut_gon_ma(x.ma_san_pham))
  AND NOT EXISTS (SELECT 1 FROM san_pham z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_san_pham) = dbo.fn_rut_gon_ma(x.ma_san_pham));

UPDATE x SET x.ma_danh_muc = dbo.fn_rut_gon_ma(x.ma_danh_muc)
FROM danh_muc x
WHERE dbo.fn_rut_gon_ma(x.ma_danh_muc) <> x.ma_danh_muc
  AND NOT EXISTS (SELECT 1 FROM danh_muc y WHERE y.ma_danh_muc = dbo.fn_rut_gon_ma(x.ma_danh_muc))
  AND NOT EXISTS (SELECT 1 FROM danh_muc z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_danh_muc) = dbo.fn_rut_gon_ma(x.ma_danh_muc));

UPDATE x SET x.ma_thuong_hieu = dbo.fn_rut_gon_ma(x.ma_thuong_hieu)
FROM thuong_hieu x
WHERE dbo.fn_rut_gon_ma(x.ma_thuong_hieu) <> x.ma_thuong_hieu
  AND NOT EXISTS (SELECT 1 FROM thuong_hieu y WHERE y.ma_thuong_hieu = dbo.fn_rut_gon_ma(x.ma_thuong_hieu))
  AND NOT EXISTS (SELECT 1 FROM thuong_hieu z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_thuong_hieu) = dbo.fn_rut_gon_ma(x.ma_thuong_hieu));

UPDATE x SET x.ma_xuat_xu = dbo.fn_rut_gon_ma(x.ma_xuat_xu)
FROM xuat_xu x
WHERE dbo.fn_rut_gon_ma(x.ma_xuat_xu) <> x.ma_xuat_xu
  AND NOT EXISTS (SELECT 1 FROM xuat_xu y WHERE y.ma_xuat_xu = dbo.fn_rut_gon_ma(x.ma_xuat_xu))
  AND NOT EXISTS (SELECT 1 FROM xuat_xu z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_xuat_xu) = dbo.fn_rut_gon_ma(x.ma_xuat_xu));

UPDATE x SET x.ma_chat_lieu = dbo.fn_rut_gon_ma(x.ma_chat_lieu)
FROM chat_lieu x
WHERE dbo.fn_rut_gon_ma(x.ma_chat_lieu) <> x.ma_chat_lieu
  AND NOT EXISTS (SELECT 1 FROM chat_lieu y WHERE y.ma_chat_lieu = dbo.fn_rut_gon_ma(x.ma_chat_lieu))
  AND NOT EXISTS (SELECT 1 FROM chat_lieu z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_chat_lieu) = dbo.fn_rut_gon_ma(x.ma_chat_lieu));

UPDATE x SET x.ma_do_cung = dbo.fn_rut_gon_ma(x.ma_do_cung)
FROM do_cung x
WHERE dbo.fn_rut_gon_ma(x.ma_do_cung) <> x.ma_do_cung
  AND NOT EXISTS (SELECT 1 FROM do_cung y WHERE y.ma_do_cung = dbo.fn_rut_gon_ma(x.ma_do_cung))
  AND NOT EXISTS (SELECT 1 FROM do_cung z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_do_cung) = dbo.fn_rut_gon_ma(x.ma_do_cung));

UPDATE x SET x.ma_diem_can_bang = dbo.fn_rut_gon_ma(x.ma_diem_can_bang)
FROM diem_can_bang x
WHERE dbo.fn_rut_gon_ma(x.ma_diem_can_bang) <> x.ma_diem_can_bang
  AND NOT EXISTS (SELECT 1 FROM diem_can_bang y WHERE y.ma_diem_can_bang = dbo.fn_rut_gon_ma(x.ma_diem_can_bang))
  AND NOT EXISTS (SELECT 1 FROM diem_can_bang z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_diem_can_bang) = dbo.fn_rut_gon_ma(x.ma_diem_can_bang));

UPDATE x SET x.ma_mau_sac = dbo.fn_rut_gon_ma(x.ma_mau_sac)
FROM mau_sac x
WHERE dbo.fn_rut_gon_ma(x.ma_mau_sac) <> x.ma_mau_sac
  AND NOT EXISTS (SELECT 1 FROM mau_sac y WHERE y.ma_mau_sac = dbo.fn_rut_gon_ma(x.ma_mau_sac))
  AND NOT EXISTS (SELECT 1 FROM mau_sac z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_mau_sac) = dbo.fn_rut_gon_ma(x.ma_mau_sac));

UPDATE x SET x.ma_trong_luong = dbo.fn_rut_gon_ma(x.ma_trong_luong)
FROM trong_luong x
WHERE dbo.fn_rut_gon_ma(x.ma_trong_luong) <> x.ma_trong_luong
  AND NOT EXISTS (SELECT 1 FROM trong_luong y WHERE y.ma_trong_luong = dbo.fn_rut_gon_ma(x.ma_trong_luong))
  AND NOT EXISTS (SELECT 1 FROM trong_luong z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_trong_luong) = dbo.fn_rut_gon_ma(x.ma_trong_luong));

UPDATE x SET x.ma_chu_vi = dbo.fn_rut_gon_ma(x.ma_chu_vi)
FROM chu_vi x
WHERE dbo.fn_rut_gon_ma(x.ma_chu_vi) <> x.ma_chu_vi
  AND NOT EXISTS (SELECT 1 FROM chu_vi y WHERE y.ma_chu_vi = dbo.fn_rut_gon_ma(x.ma_chu_vi))
  AND NOT EXISTS (SELECT 1 FROM chu_vi z WHERE z.id < x.id AND dbo.fn_rut_gon_ma(z.ma_chu_vi) = dbo.fn_rut_gon_ma(x.ma_chu_vi));

-- Mã biến thể (SP001-DEN-4U-G5 -> SP01-DEN-4U-G5): đổi theo mã sản phẩm đã gọn ở trên
UPDATE c SET c.ma_spct = sp.ma_san_pham + SUBSTRING(c.ma_spct, CHARINDEX('-', c.ma_spct), 50)
FROM san_pham_chi_tiet c
JOIN san_pham sp ON sp.id = c.id_san_pham
WHERE CHARINDEX('-', c.ma_spct) > 1
  AND LEFT(c.ma_spct, CHARINDEX('-', c.ma_spct) - 1) <> sp.ma_san_pham
  AND dbo.fn_rut_gon_ma(LEFT(c.ma_spct, CHARINDEX('-', c.ma_spct) - 1)) = sp.ma_san_pham
  AND NOT EXISTS (SELECT 1 FROM san_pham_chi_tiet y WHERE y.ma_spct = sp.ma_san_pham + SUBSTRING(c.ma_spct, CHARINDEX('-', c.ma_spct), 50));
GO

DROP FUNCTION dbo.fn_rut_gon_ma;
GO


-- #################################################################
-- PHẦN 1: DỮ LIỆU MẪU - vai trò, nhân viên, khách hàng, phiếu giảm giá (có phần 0 bổ sung cấu trúc còn thiếu cho CSDL cũ)
-- (nguyên văn từ SD_17_du_lieu_mau.sql)
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

-- 0a. Chất liệu theo danh mục: cột nhom_ap_dung (các nhóm danh mục dùng chất liệu, NULL = mọi danh mục).
--     SD_17.sql giữ nguyên nên cột này được thêm ở đây (CSDL đã có cột thì không làm gì).
IF COL_LENGTH('chat_lieu', 'nhom_ap_dung') IS NULL
    ALTER TABLE chat_lieu ADD nhom_ap_dung VARCHAR(100) NULL;
GO

-- 0. Bổ sung cấu trúc còn thiếu nếu CSDL được tạo từ bản SD_17.sql CŨ (CSDL mới đã đủ thì các lệnh dưới không làm gì).
--    Không có bước này thì backend báo "Invalid column name 'can_cuoc_cong_dan'" hoặc "Invalid object name 'dia_chi_nhan_vien'".
IF COL_LENGTH('nhan_vien', 'can_cuoc_cong_dan') IS NULL
    ALTER TABLE nhan_vien ADD can_cuoc_cong_dan VARCHAR(12) NULL;
GO

IF EXISTS (SELECT 1 FROM sys.indexes i
           JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
           JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
           WHERE i.name = 'ux_nhan_vien_cccd' AND i.object_id = OBJECT_ID('nhan_vien') AND c.name = 'cccd')
    DROP INDEX ux_nhan_vien_cccd ON nhan_vien;
GO

IF COL_LENGTH('nhan_vien', 'cccd') IS NOT NULL
    EXEC('UPDATE nhan_vien SET can_cuoc_cong_dan = cccd WHERE can_cuoc_cong_dan IS NULL AND cccd IS NOT NULL');
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_cccd' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (can_cuoc_cong_dan) WHERE can_cuoc_cong_dan IS NOT NULL;
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

-- Khách hàng: ảnh đại diện (trang Thêm / Sửa khách hàng có ô tải ảnh).
IF COL_LENGTH('khach_hang', 'anh_dai_dien') IS NULL
    ALTER TABLE khach_hang ADD anh_dai_dien NVARCHAR(500) NULL;
GO

IF COL_LENGTH('dot_giam_gia', 'mo_ta') IS NULL
    ALTER TABLE dot_giam_gia ADD mo_ta NVARCHAR(500) NULL;
GO

-- 1. Vai trò (tên "Quản lý" phải khớp VaiTro.TEN_QUAN_LY ở backend)
INSERT INTO vai_tro (ten_vai_tro, trang_thai)
SELECT v.ten, 1 FROM (VALUES (N'Quản lý'), (N'Nhân viên')) AS v(ten)
WHERE NOT EXISTS (SELECT 1 FROM vai_tro r WHERE r.ten_vai_tro = v.ten);
GO

-- 2. Nhân viên
INSERT INTO nhan_vien (id_vai_tro, ma_nhan_vien, ho_ten, gioi_tinh, ngay_sinh, sdt, email, mat_khau, dia_chi, ngay_vao_lam, trang_thai)
SELECT r.id, s.ma, s.ho_ten, s.gioi_tinh, s.ngay_sinh, s.sdt, s.email, s.mat_khau, s.dia_chi, s.ngay_vao_lam, s.trang_thai
FROM (VALUES
    (N'Quản lý', 'NV01', N'Nguyễn Hoàng Long', 1, '1992-03-14', '0912000001', 'long.nh@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 15 ngõ 120 Trần Phú, phường Hà Đông, Hà Nội', '2022-01-10', 1),
    (N'Quản lý', 'NV02', N'Trần Thu Hà', 0, '1994-07-22', '0912000002', 'ha.tt@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 22 Nguyễn Trãi, quận Thanh Xuân, Hà Nội', '2022-03-01', 1),
    (N'Nhân viên', 'NV03', N'Lê Quốc Bảo', 1, '1999-11-05', '0912000003', 'bao.lq@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 8 đường Lê Văn Lương, quận Thanh Xuân, Hà Nội', '2023-02-15', 1),
    (N'Nhân viên', 'NV04', N'Phạm Minh Anh', 0, '2000-01-18', '0912000004', 'anh.pm@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 31 phố Kim Mã, quận Ba Đình, Hà Nội', '2023-05-20', 1),
    (N'Nhân viên', 'NV05', N'Vũ Hoàng Nam', 1, '1998-09-30', '0912000005', 'nam.vh@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Thị trấn Quốc Oai, huyện Quốc Oai, Hà Nội', '2023-08-01', 1),
    (N'Nhân viên', 'NV06', N'Đỗ Thùy Trang', 0, '2001-05-09', '0912000006', 'trang.dt@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 45 phố Trần Thái Tông, phường Cầu Giấy, Hà Nội', '2024-01-08', 1),
    (N'Nhân viên', 'NV07', N'Bùi Đức Mạnh', 1, '1997-12-02', '0912000007', 'manh.bd@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 3 ngõ 66 Nguyễn Văn Cừ, quận Long Biên, Hà Nội', '2023-10-12', 0),
    (N'Nhân viên', 'NV08', N'Ngô Linh Chi', 0, '2002-04-27', '0912000008', 'chi.nl@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 19 đường Phạm Văn Đồng, quận Bắc Từ Liêm, Hà Nội', '2024-06-03', 1),
    (N'Nhân viên', 'NV09', N'Hoàng Tuấn Anh', 1, '1996-08-11', '0912000009', 'anh.ht@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 7 phố Hoàng Quốc Việt, quận Cầu Giấy, Hà Nội', '2024-09-16', 1),
    (N'Nhân viên', 'NV10', N'Đặng Mai Hương', 0, '1995-02-25', '0912000010', 'huong.dm@footstyle.vn', '$2a$10$rngBEegtifzMhc1qfTrIvOGQXep69zUZBdSq5YFNVsPKZraiunq7m', N'Số 52 phố Minh Khai, quận Hai Bà Trưng, Hà Nội', '2022-11-21', 0)
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
    ('KH01', N'Nguyễn Thị Lan', '0901000001', 'lan.nt@gmail.com', 0),
    ('KH02', N'Trần Văn Hùng', '0901000002', 'hung.tv@gmail.com', 1),
    ('KH03', N'Lê Minh Tuấn', '0901000003', 'tuan.lm@gmail.com', 1),
    ('KH04', N'Phạm Thu Hằng', '0901000004', 'hang.pt@gmail.com', 0),
    ('KH05', N'Hoàng Đức Anh', '0901000005', 'anh.hd@gmail.com', 1),
    ('KH06', N'Vũ Khánh Linh', '0901000006', 'linh.vk@gmail.com', 0)
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
    ('VCHTT50KH', 'KH01'),
    ('VCHTT50KH', 'KH02'),
    ('VCHTT50KH', 'KH03'),
    ('VCHDOI100', 'KH04'),
    ('VCHDOI100', 'KH05'),
    ('VCHNEWMB1', 'KH06')
) AS t(ma_phieu, ma_khach_hang)
JOIN phieu_giam_gia p ON p.ma_phieu = t.ma_phieu
JOIN khach_hang k ON k.ma_khach_hang = t.ma_khach_hang
WHERE NOT EXISTS (SELECT 1 FROM phieu_giam_gia_khach_hang l WHERE l.id_phieu_giam_gia = p.id AND l.id_khach_hang = k.id);
GO

-- #################################################################
-- PHẦN 2: DỮ LIỆU MẪU SẢN PHẨM - 9 bảng thuộc tính, sản phẩm, biến thể
-- (nguyên văn từ SD_17_du_lieu_san_pham.sql)
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
    ('DM01', N'Vợt cầu lông'),
    ('DM02', N'Vợt cầu lông người mới'),
    ('DM03', N'Quần áo thể thao'),
    ('DM04', N'Túi - balo'),
    ('DM05', N'Phụ kiện'),
    ('DM06', N'Quả cầu lông')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM danh_muc t WHERE t.ma_danh_muc = v.ma);
GO

-- 2. Thương hiệu
INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('TH01', N'Yonex'),
    ('TH02', N'Lining'),
    ('TH03', N'Victor'),
    ('TH04', N'Mizuno'),
    ('TH05', N'Kumpoo')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM thuong_hieu t WHERE t.ma_thuong_hieu = v.ma);
GO

-- 3. Xuất xứ
INSERT INTO xuat_xu (ma_xuat_xu, ten_xuat_xu, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('XX01', N'Nhật Bản'),
    ('XX02', N'Trung Quốc'),
    ('XX03', N'Đài Loan'),
    ('XX04', N'Việt Nam')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM xuat_xu t WHERE t.ma_xuat_xu = v.ma);
GO

-- 4. Chất liệu
-- Mỗi chất liệu gắn với các NHÓM danh mục dùng nó (VOT, QUAN_AO, TUI_BALO, PHU_KIEN, QUA_CAU) để form sản phẩm
-- chỉ hiện chất liệu hợp với danh mục đã chọn (vd Quần áo thể thao không có Nhôm / Titanium).
INSERT INTO chat_lieu (ma_chat_lieu, ten_chat_lieu, trang_thai, nhom_ap_dung)
SELECT v.ma, v.ten, 1, v.nhom
FROM (VALUES
    ('CL01', N'Carbon', 'VOT'),
    ('CL02', N'Graphite', 'VOT'),
    ('CL03', N'Nhôm', 'VOT'),
    ('CL04', N'Titanium', 'VOT'),
    ('CL05', N'Da PU', 'TUI_BALO,PHU_KIEN'),
    ('CL06', N'Cotton', 'QUAN_AO,PHU_KIEN'),
    ('CL07', N'Polyester', 'QUAN_AO,TUI_BALO,PHU_KIEN'),
    ('CL08', N'Thun lạnh', 'QUAN_AO'),
    ('CL09', N'Spandex', 'QUAN_AO'),
    ('CL10', N'Nylon', 'TUI_BALO,PHU_KIEN'),
    ('CL11', N'Vải dù', 'TUI_BALO'),
    ('CL12', N'Cao su', 'PHU_KIEN'),
    ('CL13', N'Lông ngỗng', 'QUA_CAU'),
    ('CL14', N'Lông vịt', 'QUA_CAU'),
    ('CL15', N'Nhựa', 'QUA_CAU,PHU_KIEN')
) AS v(ma, ten, nhom)
WHERE NOT EXISTS (SELECT 1 FROM chat_lieu t WHERE t.ma_chat_lieu = v.ma)
  AND NOT EXISTS (SELECT 1 FROM chat_lieu t WHERE t.ten_chat_lieu = v.ten);
GO
UPDATE chat_lieu SET nhom_ap_dung = 'VOT'
WHERE nhom_ap_dung IS NULL AND ten_chat_lieu IN (N'Carbon', N'Graphite', N'Nhôm', N'Titanium');
UPDATE chat_lieu SET nhom_ap_dung = 'TUI_BALO,PHU_KIEN'
WHERE nhom_ap_dung IS NULL AND ten_chat_lieu = N'Da PU';
GO

-- 5. Độ cứng
INSERT INTO do_cung (ma_do_cung, ten_do_cung, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('DC01', N'Mềm'),
    ('DC02', N'Trung bình'),
    ('DC03', N'Cứng'),
    ('DC04', N'Rất cứng')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM do_cung t WHERE t.ma_do_cung = v.ma);
GO

-- 6. Điểm cân bằng
INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('CB01', N'Nặng đầu'),
    ('CB02', N'Cân bằng'),
    ('CB03', N'Nhẹ đầu')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM diem_can_bang t WHERE t.ma_diem_can_bang = v.ma);
GO

-- 7. Màu sắc
INSERT INTO mau_sac (ma_mau_sac, ten_mau_sac, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('MS01', N'Đen'),
    ('MS02', N'Trắng'),
    ('MS03', N'Đỏ'),
    ('MS04', N'Xanh dương'),
    ('MS05', N'Vàng')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM mau_sac t WHERE t.ma_mau_sac = v.ma);
GO

-- 8. Trọng lượng
INSERT INTO trong_luong (ma_trong_luong, ten_trong_luong, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('TL01', N'2U (90-94g)'),
    ('TL02', N'3U (85-89g)'),
    ('TL03', N'4U (80-84g)'),
    ('TL04', N'5U (75-79g)')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM trong_luong t WHERE t.ma_trong_luong = v.ma);
GO

-- 9. Chu vi
INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('CV01', N'G4'),
    ('CV02', N'G5'),
    ('CV03', N'G6')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM chu_vi t WHERE t.ma_chu_vi = v.ma);
GO

-- 9b. Mục "Không áp dụng": danh mục không phải vợt (Phụ kiện, Túi - balo, Quần áo thể thao, Quả cầu lông...) không có
--     độ cứng / điểm cân bằng / trọng lượng / chu vi, nên hệ thống tự gán mục này (không hiện trong ô chọn và danh sách thuộc tính).
INSERT INTO do_cung (ma_do_cung, ten_do_cung, trang_thai)
SELECT 'DC00', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM do_cung t WHERE t.ma_do_cung IN ('DC00', 'DC000') OR t.ten_do_cung = N'Không áp dụng');
INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai)
SELECT 'CB00', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM diem_can_bang t WHERE t.ma_diem_can_bang IN ('CB00', 'CB000') OR t.ten_diem_can_bang = N'Không áp dụng');
INSERT INTO trong_luong (ma_trong_luong, ten_trong_luong, trang_thai)
SELECT 'TL00', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM trong_luong t WHERE t.ma_trong_luong IN ('TL00', 'TL000') OR t.ten_trong_luong = N'Không áp dụng');
INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai)
SELECT 'CV00', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM chu_vi t WHERE t.ma_chu_vi IN ('CV00', 'CV000') OR t.ten_chu_vi = N'Không áp dụng');
GO

-- 10. Sản phẩm (trỏ tới thuộc tính bằng mã)
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang,
                      ma_san_pham, ten_san_pham, mo_ta, trang_thai)
SELECT dm.id, th.id, xx.id, cl.id, dc.id, cb.id, s.ma, s.ten, s.mo_ta, 1
FROM (VALUES
    ('SP01', N'Yonex Astrox 99 Pro', 'DM01', 'TH01', 'XX01', 'CL01', 'DC03', 'CB01', N'Vợt thiên công cao cấp, khung carbon.'),
    ('SP02', N'Lining Axforce 80', 'DM01', 'TH02', 'XX02', 'CL01', 'DC04', 'CB01', N'Vợt dành cho người chơi lực tay tốt.'),
    ('SP03', N'Victor Thruster K 9900', 'DM01', 'TH03', 'XX03', 'CL02', 'DC03', 'CB01', N'Vợt thiên công, nặng đầu, đập cầu mạnh.'),
    ('SP04', N'Yonex Nanoflare 800 Pro', 'DM01', 'TH01', 'XX01', 'CL01', 'DC02', 'CB02', N'Vợt thiên tốc độ, khung nhẹ, phản hồi nhanh.'),
    ('SP05', N'Mizuno Fortius 10 Quick', 'DM01', 'TH04', 'XX01', 'CL01', 'DC02', 'CB02', N'Vợt nhẹ, linh hoạt, dễ điều cầu.'),
    ('SP06', N'Kumpoo Power Control', 'DM01', 'TH05', 'XX03', 'CL02', 'DC02', 'CB02', N'Vợt công thủ toàn diện, phù hợp người mới chơi.')
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
    ('SP01-DEN-4U-G5', 'SP01', 'MS01', 'TL03', 'CV02', 4290000, 20),
    ('SP01-DEN-4U-G4', 'SP01', 'MS01', 'TL03', 'CV01', 4290000, 10),
    ('SP01-DEN-3U-G5', 'SP01', 'MS01', 'TL02', 'CV02', 4290000, 15),
    ('SP01-DO-4U-G5', 'SP01', 'MS03', 'TL03', 'CV02', 4290000, 8),
    ('SP01-DO-3U-G5', 'SP01', 'MS03', 'TL02', 'CV02', 4290000, 5),
    ('SP01-VANG-4U-G6', 'SP01', 'MS05', 'TL03', 'CV03', 4290000, 0),
    ('SP02-DEN-4U-G5', 'SP02', 'MS01', 'TL03', 'CV02', 2850000, 14),
    ('SP02-DEN-4U-G4', 'SP02', 'MS01', 'TL03', 'CV01', 2850000, 11),
    ('SP02-XDG-3U-G5', 'SP02', 'MS04', 'TL02', 'CV02', 2850000, 6),
    ('SP03-DEN-4U-G5', 'SP03', 'MS01', 'TL03', 'CV02', 3150000, 10),
    ('SP03-DO-3U-G5', 'SP03', 'MS03', 'TL02', 'CV02', 3150000, 7),
    ('SP04-DEN-4U-G5', 'SP04', 'MS01', 'TL03', 'CV02', 2790000, 12),
    ('SP05-TRG-4U-G5', 'SP05', 'MS02', 'TL03', 'CV02', 2350000, 9),
    ('SP06-DEN-4U-G5', 'SP06', 'MS01', 'TL03', 'CV02', 1250000, 18)
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
-- PHẦN 3: DỮ LIỆU MẪU - địa chỉ khách hàng, đợt giảm giá + biến thể áp dụng
-- (nguyên văn từ SD_17_du_lieu_khach_hang_dot_giam_gia.sql)
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
UPDATE khach_hang SET ngay_sinh = '1995-03-12' WHERE ma_khach_hang = 'KH01' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1992-07-25' WHERE ma_khach_hang = 'KH02' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1998-11-02' WHERE ma_khach_hang = 'KH03' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1996-05-20' WHERE ma_khach_hang = 'KH04' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1994-09-08' WHERE ma_khach_hang = 'KH05' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '2000-01-15' WHERE ma_khach_hang = 'KH06' AND ngay_sinh IS NULL;
GO

-- 2. Địa chỉ giao hàng mặc định cho khách hàng mẫu (cả 6 khách hàng KH01 - KH06 đều có địa chỉ)
INSERT INTO dia_chi_khach_hang (id_khach_hang, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_chi_tiet, phuong_xa, quan_huyen, tinh_thanh, la_mac_dinh, trang_thai)
SELECT k.id, k.ho_ten, k.sdt, a.dia_chi, a.phuong, a.quan, a.tinh, 1, 1
FROM (VALUES
    ('KH01', N'12 Nguyễn Trãi',  N'Phường Thượng Đình', N'Quận Thanh Xuân', N'Thành phố Hà Nội'),
    ('KH02', N'45 Lê Lợi',       N'Phường Bến Thành',   N'Quận 1',          N'Thành phố Hồ Chí Minh'),
    ('KH03', N'8 Trần Phú',      N'Phường Hải Châu 1',  N'Quận Hải Châu',   N'Thành phố Đà Nẵng'),
    -- KH04 - KH06: địa chỉ theo địa giới sau sáp nhập 07/2025 (Tỉnh/Thành phố -> Phường/Xã, không còn Quận/Huyện)
    ('KH04', N'25 Hoàng Quốc Việt', N'Phường Nghĩa Đô',  NULL,               N'Thành phố Hà Nội'),
    ('KH05', N'102 Nguyễn Văn Cừ',  N'Phường Hồng Bàng', NULL,               N'Thành phố Hải Phòng'),
    ('KH06', N'5 Lê Duẩn',          N'Phường Bến Thành', NULL,               N'Thành phố Hồ Chí Minh')
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
    ('DGG01', N'Sale vợt Yonex Astrox', 10, -7, 14, 1),
    ('DGG02', N'Xả hàng vợt cầu lông', 15, -40, -25, 1),
    ('DGG03', N'Black Friday vợt Lining', 20, 30, 37, 1),
    ('DGG04', N'Tuần lễ vàng Victor', 12, -3, 4, 1),
    ('DGG05', N'Mừng khai trương FootStyle', 8, -120, -90, 1),
    ('DGG06', N'Ưu đãi người mới chơi', 5, -15, 45, 0),
    ('DGG07', N'Hè sôi động', 18, -75, -50, 1),
    ('DGG08', N'Tết rộn ràng - sale lớn', 25, 80, 95, 1)
) AS d(ma, ten, pt, bd, kt, trang_thai)
WHERE NOT EXISTS (SELECT 1 FROM dot_giam_gia x WHERE x.ma_dot = d.ma);
GO

-- 4. Biến thể được giảm trong từng đợt
INSERT INTO dot_giam_gia_chi_tiet (id_san_pham_chi_tiet, id_dot_giam_gia, phan_tram_giam_bien_the, trang_thai)
SELECT c.id, d.id, d.phan_tram_giam_dot, 1
FROM (VALUES
    ('DGG01', 'SP01-DEN-4U-G5'), ('DGG01', 'SP01-DEN-3U-G5'), ('DGG01', 'SP01-DO-4U-G5'), ('DGG01', 'SP01-DO-3U-G5'),
    ('DGG02', 'SP04-DEN-4U-G5'), ('DGG02', 'SP05-TRG-4U-G5'),
    ('DGG03', 'SP02-DEN-4U-G5'), ('DGG03', 'SP02-DEN-4U-G4'), ('DGG03', 'SP02-XDG-3U-G5'),
    ('DGG04', 'SP03-DEN-4U-G5'), ('DGG04', 'SP03-DO-3U-G5'),
    ('DGG05', 'SP06-DEN-4U-G5'), ('DGG05', 'SP01-DEN-4U-G5'),
    ('DGG06', 'SP06-DEN-4U-G5'),
    ('DGG07', 'SP02-DEN-4U-G5'), ('DGG07', 'SP04-DEN-4U-G5'),
    ('DGG08', 'SP01-DEN-4U-G5'), ('DGG08', 'SP04-DEN-4U-G5'), ('DGG08', 'SP05-TRG-4U-G5')
) AS v(ma_dot, ma_spct)
JOIN dot_giam_gia d ON d.ma_dot = v.ma_dot
JOIN san_pham_chi_tiet c ON c.ma_spct = v.ma_spct
WHERE NOT EXISTS (SELECT 1 FROM dot_giam_gia_chi_tiet x WHERE x.id_dot_giam_gia = d.id AND x.id_san_pham_chi_tiet = c.id);
GO

-- #################################################################
-- PHẦN 4: DỮ LIỆU MẪU - phương thức thanh toán, hóa đơn (+ chi tiết, lịch sử, thanh toán)
-- (nguyên văn từ SD_17_du_lieu_hoa_don.sql)
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
    ('HD01', 'NV03', NULL,     NULL,        'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -120, 6, NULL, NULL),
    ('HD02', 'NV04', 'KH02', 'VCH7QX2YN', 'PT02', 1, 0,     NULL, NULL, NULL, NULL, NULL, -1500, 6, NULL, NULL),
    ('HD03', 'NV03', 'KH01', 'VCHTT50KH', 'PT03', 2, 30000, N'Giao Hàng Nhanh', N'Nguyễn Thị Lan', '0901000001',
        N'Số 12 ngõ 45 Nguyễn Chí Thanh, phường Láng Thượng, quận Đống Đa, Hà Nội', N'Gọi trước khi giao', -60, 1, NULL, NULL),
    ('HD04', 'NV06', 'KH03', NULL,        'PT04', 2, 35000, N'Giao Hàng Tiết Kiệm', N'Lê Minh Tuấn', '0901000003',
        N'Số 88 Lê Lợi, phường Bến Nghé, quận 1, TP. Hồ Chí Minh', NULL, -1440, 2, NULL, NULL),
    ('HD05', 'NV09', 'KH05', 'VCHFS500K', 'PT03', 2, 30000, N'Giao Hàng Nhanh', N'Hoàng Đức Anh', '0901000005',
        N'Số 5 Trần Đại Nghĩa, phường Bách Khoa, quận Hai Bà Trưng, Hà Nội', N'Giao giờ hành chính', -2880, 3, NULL, NULL),
    ('HD06', 'NV03', 'KH04', 'VCH7QX2YN', 'PT02', 2, 40000, N'Viettel Post', N'Phạm Thu Hằng', '0901000004',
        N'Số 21 Hùng Vương, phường Thạch Thang, quận Hải Châu, Đà Nẵng', NULL, -4320, 4, NULL, NULL),
    ('HD07', 'NV04', 'KH06', NULL,        'PT03', 2, 30000, N'Giao Hàng Tiết Kiệm', N'Vũ Khánh Linh', '0901000006',
        N'Số 7 Lạch Tray, phường Lạch Tray, quận Ngô Quyền, Hải Phòng', NULL, -5760, 5, NULL, NULL),
    ('HD08', 'NV06', 'KH02', 'VCH7QX2YN', 'PT03', 2, 0,     N'Giao Hàng Nhanh', N'Trần Văn Hùng', '0901000002',
        N'Số 102 Cầu Giấy, phường Quan Hoa, quận Cầu Giấy, Hà Nội', N'Miễn phí vận chuyển', -8640, 6, NULL, NULL),
    ('HD09', 'NV09', 'KH03', NULL,        'PT03', 2, 30000, N'Giao Hàng Nhanh', N'Lê Minh Tuấn', '0901000003',
        N'Số 88 Lê Lợi, phường Bến Nghé, quận 1, TP. Hồ Chí Minh', NULL, -2700, 0, 2, N'Khách đổi ý, muốn đổi sang mẫu vợt khác'),
    ('HD10', 'NV03', 'KH05', NULL,        'PT03', 2, 30000, N'Viettel Post', N'Hoàng Đức Anh', '0901000005',
        N'Số 5 Trần Đại Nghĩa, phường Bách Khoa, quận Hai Bà Trưng, Hà Nội', NULL, -7200, 0, 1, N'Không liên lạc được với khách hàng'),
    ('HD11', 'NV01', NULL,     'VCH7QX2YN', 'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -11520, 6, NULL, NULL),
    ('HD12', 'NV04', 'KH01', 'VCHPK20QC', 'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -43200, 6, NULL, NULL),
    ('HD13', 'NV06', 'KH04', NULL,        'PT02', 2, 0,     N'Giao Hàng Nhanh', N'Phạm Thu Hằng', '0901000004',
        N'Số 21 Hùng Vương, phường Thạch Thang, quận Hải Châu, Đà Nẵng', N'Mua 2 cây tặng bạn', -50400, 6, NULL, NULL),
    ('HD14', 'NV09', 'KH06', 'VCHHE12XN', 'PT02', 1, 0,     NULL, NULL, NULL, NULL, NULL, -93600, 6, NULL, NULL),
    ('HD15', 'NV03', NULL,     NULL,        'PT01', 1, 0,     NULL, NULL, NULL, NULL, NULL, -144000, 6, NULL, NULL),
    ('HD16', 'NV01', NULL,     NULL,        'PT01', 1, 0,     NULL, NULL, NULL, NULL, N'Khách đang chờ lấy tiền', -10, 1, NULL, NULL),
    ('HD17', 'NV04', 'KH02', 'VCHFS500K', 'PT04', 2, 0,     N'Giao Hàng Nhanh', N'Trần Văn Hùng', '0901000002',
        N'Số 102 Cầu Giấy, phường Quan Hoa, quận Cầu Giấy, Hà Nội', NULL, -180, 1, NULL, NULL),
    ('HD18', 'NV09', 'KH03', NULL,        'PT03', 2, 30000, N'Giao Hàng Tiết Kiệm', N'Lê Minh Tuấn', '0901000003',
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
    ('HD01', 'SP06-DEN-4U-G5', 1, 0),
    ('HD02', 'SP01-DEN-4U-G5', 1, 10),
    ('HD03', 'SP02-XDG-3U-G5', 1, 0),
    ('HD03', 'SP06-DEN-4U-G5', 1, 0),
    ('HD04', 'SP03-DEN-4U-G5', 1, 12),
    ('HD05', 'SP04-DEN-4U-G5', 1, 0),
    ('HD05', 'SP06-DEN-4U-G5', 2, 0),
    ('HD06', 'SP01-DO-4U-G5', 1, 10),
    ('HD07', 'SP05-TRG-4U-G5', 1, 0),
    ('HD08', 'SP02-DEN-4U-G5', 1, 0),
    ('HD08', 'SP03-DO-3U-G5', 1, 0),
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
    ('HD16', 'SP01-DEN-3U-G5', 1, 10),
    ('HD17', 'SP01-DEN-4U-G5', 1, 10),
    ('HD17', 'SP03-DO-3U-G5', 1, 12),
    ('HD18', 'SP06-DEN-4U-G5', 1, 0)
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
-- PHẦN 5: NÂNG CẤP CSDL CŨ - chỉ mục chống trùng, cột / bảng bổ sung (chạy lại nhiều lần được)
-- (nguyên văn từ SD_17_nang_cap.sql)
-- #################################################################

-- =================================================================
-- NÂNG CẤP CSDL ĐÃ CHẠY TRƯỚC ĐÓ (không cần chạy nếu vừa tạo mới bằng SD_17.sql bản mới)
-- Có thể chạy lại nhiều lần. Thêm các chỉ mục chống trùng ở mức CSDL, cột số căn cước (nhan_vien.can_cuoc_cong_dan),
-- bảng dia_chi_nhan_vien (nhân viên có nhiều địa chỉ). Vị trí làm việc đã bỏ khỏi hệ thống: nếu CSDL còn 2 bảng cũ thì hệ thống không dùng nữa, để yên không ảnh hưởng.
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
WHERE ma_danh_muc = 'DM02' AND ten_danh_muc = N'Giày cầu lông';
GO

UPDATE san_pham
SET ten_san_pham = N'Yonex Nanoflare 800 Pro',
    mo_ta = N'Vợt thiên tốc độ, khung nhẹ, phản hồi nhanh.',
    id_danh_muc = (SELECT id FROM danh_muc WHERE ma_danh_muc = 'DM01'),
    id_chat_lieu = (SELECT id FROM chat_lieu WHERE ma_chat_lieu = 'CL01')
WHERE ma_san_pham = 'SP04' AND ten_san_pham = N'Yonex Power Cushion 65Z3';
GO

UPDATE san_pham
SET ten_san_pham = N'Mizuno Fortius 10 Quick',
    mo_ta = N'Vợt nhẹ, linh hoạt, dễ điều cầu.',
    id_danh_muc = (SELECT id FROM danh_muc WHERE ma_danh_muc = 'DM01'),
    id_chat_lieu = (SELECT id FROM chat_lieu WHERE ma_chat_lieu = 'CL01')
WHERE ma_san_pham = 'SP05' AND ten_san_pham = N'Mizuno Wave Fang Pro';
GO

-- Chỉ đổi mã khi mã mới CHƯA có (nếu dữ liệu vợt mới đã được nạp trước đó thì mã mới đã tồn tại, đổi nữa sẽ trùng khóa UNIQUE).
UPDATE san_pham_chi_tiet
SET ma_spct = 'SP04-DEN-4U-G5',
    id_trong_luong = (SELECT id FROM trong_luong WHERE ma_trong_luong = 'TL03'),
    id_chu_vi = (SELECT id FROM chu_vi WHERE ma_chu_vi = 'CV02')
WHERE ma_spct = 'SP04-DEN-40'
  AND NOT EXISTS (SELECT 1 FROM san_pham_chi_tiet x WHERE x.ma_spct = 'SP04-DEN-4U-G5');
GO

-- Biến thể giày cũ còn sót lại khi mã mới đã có: ngưng hoạt động (giữ nguyên để không mất lịch sử hóa đơn cũ, không hiện trong danh sách đang bán).
UPDATE san_pham_chi_tiet SET trang_thai = 0
WHERE ma_spct = 'SP04-DEN-40'
  AND EXISTS (SELECT 1 FROM san_pham_chi_tiet x WHERE x.ma_spct = 'SP04-DEN-4U-G5');
GO

-- Chỉ đổi mã khi mã mới CHƯA có (nếu dữ liệu vợt mới đã được nạp trước đó thì mã mới đã tồn tại, đổi nữa sẽ trùng khóa UNIQUE).
UPDATE san_pham_chi_tiet
SET ma_spct = 'SP05-TRG-4U-G5',
    id_trong_luong = (SELECT id FROM trong_luong WHERE ma_trong_luong = 'TL03'),
    id_chu_vi = (SELECT id FROM chu_vi WHERE ma_chu_vi = 'CV02')
WHERE ma_spct = 'SP05-TRG-40'
  AND NOT EXISTS (SELECT 1 FROM san_pham_chi_tiet x WHERE x.ma_spct = 'SP05-TRG-4U-G5');
GO

-- Biến thể giày cũ còn sót lại khi mã mới đã có: ngưng hoạt động (giữ nguyên để không mất lịch sử hóa đơn cũ, không hiện trong danh sách đang bán).
UPDATE san_pham_chi_tiet SET trang_thai = 0
WHERE ma_spct = 'SP05-TRG-40'
  AND EXISTS (SELECT 1 FROM san_pham_chi_tiet x WHERE x.ma_spct = 'SP05-TRG-4U-G5');
GO

UPDATE dot_giam_gia SET ten_dot = N'Xả hàng vợt cầu lông'
WHERE ma_dot = 'DGG02' AND ten_dot = N'Xả hàng giày cầu lông';
GO

-- Chất liệu "Vải lưới" là của giày cũ, đổi thành chất liệu khung vợt (sản phẩm đang dùng chất liệu này vẫn giữ nguyên liên kết).
UPDATE chat_lieu SET ten_chat_lieu = N'Titanium'
WHERE ma_chat_lieu = 'CL04' AND ten_chat_lieu = N'Vải lưới';
GO

-- Nhân viên: thêm số căn cước công dân (quét QR trên thẻ CCCD hoặc nhập tay) + chống trùng.
-- Cột tên can_cuoc_cong_dan. Bản cũ từng dùng tên cột cccd: dữ liệu được chép sang cột mới, chỉ mục cũ được tạo lại trên cột mới.
IF COL_LENGTH('nhan_vien', 'can_cuoc_cong_dan') IS NULL
    ALTER TABLE nhan_vien ADD can_cuoc_cong_dan VARCHAR(12) NULL;
GO

IF EXISTS (SELECT 1 FROM sys.indexes i
           JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
           JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
           WHERE i.name = 'ux_nhan_vien_cccd' AND i.object_id = OBJECT_ID('nhan_vien') AND c.name = 'cccd')
    DROP INDEX ux_nhan_vien_cccd ON nhan_vien;
GO

IF COL_LENGTH('nhan_vien', 'cccd') IS NOT NULL
    EXEC('UPDATE nhan_vien SET can_cuoc_cong_dan = cccd WHERE can_cuoc_cong_dan IS NULL AND cccd IS NOT NULL');
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_cccd' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_cccd ON nhan_vien (can_cuoc_cong_dan) WHERE can_cuoc_cong_dan IS NOT NULL;
GO

-- Nhân viên: nhiều địa chỉ (chọn 1 địa chỉ chính). Địa chỉ theo đơn vị hành chính sau sáp nhập: Tỉnh/Thành phố -> Phường/Xã.
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
SELECT 'DC00', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM do_cung t WHERE t.ma_do_cung IN ('DC00', 'DC000') OR t.ten_do_cung = N'Không áp dụng');
INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai)
SELECT 'CB00', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM diem_can_bang t WHERE t.ma_diem_can_bang IN ('CB00', 'CB000') OR t.ten_diem_can_bang = N'Không áp dụng');
INSERT INTO trong_luong (ma_trong_luong, ten_trong_luong, trang_thai)
SELECT 'TL00', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM trong_luong t WHERE t.ma_trong_luong IN ('TL00', 'TL000') OR t.ten_trong_luong = N'Không áp dụng');
INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai)
SELECT 'CV00', N'Không áp dụng', 1
WHERE NOT EXISTS (SELECT 1 FROM chu_vi t WHERE t.ma_chu_vi IN ('CV00', 'CV000') OR t.ten_chu_vi = N'Không áp dụng');
GO

-- #################################################################
-- PHẦN 6: ẢNH SẢN PHẨM THEO MÀU (thêm 12/10/2026 theo nhận xét của cô và 2 video mẫu)
-- Thêm biến thể: mỗi màu có phần "Ảnh sản phẩm màu ..." riêng, ảnh đó dùng cho MỌI biến thể cùng màu.
-- Cột hinh_anh_san_pham.id_mau_sac: NULL = ảnh chung của sản phẩm (toàn bộ dữ liệu cũ giữ nguyên, vẫn chạy như trước);
--                                   có giá trị = ảnh của màu đó (mỗi màu có 1 ảnh la_anh_chinh = 1).
-- Chạy lại nhiều lần được (đã có cột / chỉ mục thì bỏ qua). Backend cũng tự thêm khi khởi động (SchemaUpgradeRunner).
-- #################################################################
USE shop_cau_long_footstyle;
GO

IF COL_LENGTH('hinh_anh_san_pham', 'id_mau_sac') IS NULL
    ALTER TABLE hinh_anh_san_pham ADD id_mau_sac INT NULL
        CONSTRAINT fk_hasp_mau_sac FOREIGN KEY REFERENCES mau_sac(id);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_hasp_san_pham_mau' AND object_id = OBJECT_ID('hinh_anh_san_pham'))
    CREATE INDEX ix_hasp_san_pham_mau ON hinh_anh_san_pham (id_san_pham, id_mau_sac);
GO

-- #################################################################
-- PHẦN 7: KHÁCH HÀNG KHÔNG ĐƯỢC TRÙNG EMAIL / SỐ ĐIỆN THOẠI (theo nhận xét của cô: validate trùng ở form Khách hàng)
-- Backend (KhachHangService) đã báo "đã tồn tại" khi lưu; hai chỉ mục này chặn nốt trường hợp hai người lưu cùng lúc.
-- Chỉ tạo khi dữ liệu hiện có chưa trùng. Nếu có dòng trùng thì in ra để sửa dữ liệu trước rồi chạy lại phần này.
-- Chạy lại nhiều lần được. Backend cũng tự thêm khi khởi động (SchemaUpgradeRunner).
-- #################################################################
USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON;
GO

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

-- #################################################################
-- PHẦN 8: HÓA ĐƠN THEO NHẬN XÉT CỦA CÔ (video review giai đoạn 2)
-- Không đổi cấu trúc bảng. Cột hoa_don.trang_thai (TINYINT) có thêm mã 8 = "Giao hàng thất bại":
--   0 Đã hủy, 1 Chờ xác nhận, 2 Đã xác nhận, 3 Chờ giao hàng, 4 Đang giao hàng, 5 Đã giao hàng, 6 Đã hoàn thành,
--   7 Hoàn tiền (chỉ để dành cho tab "Hoàn tiền", chưa có luồng chuyển sang), 8 Giao hàng thất bại.
-- 8.1 Đơn TẠI QUẦY (loai_hoa_don = 1) nhận hàng ngay ở cửa hàng nên KHÔNG có phí vận chuyển và đơn vị vận chuyển
--     (cô: "tại quầy sao lại có phí vận chuyển"): dọn dữ liệu cũ còn sót; thành tiền trừ lại phần phí đã cộng (không để âm).
--     Backend cũng tự chạy lệnh này khi khởi động (SchemaUpgradeRunner).
-- 8.2 Hóa đơn mẫu HD19 ở trạng thái "Giao hàng thất bại" (đủ chi tiết + lịch sử, có lý do) để xem ngay nhánh này
--     trên màn hình Hóa đơn (cô: "trường hợp giao hàng thất bại chưa thể hiện"). Không tạo lại nếu đã có HD19.
-- Chạy lại nhiều lần được.
-- #################################################################
USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON;
GO

-- 8.1 Đơn tại quầy: bỏ phí vận chuyển / đơn vị vận chuyển còn sót
UPDATE hoa_don
SET thanh_tien = CASE WHEN thanh_tien > ISNULL(phi_van_chuyen, 0) THEN thanh_tien - ISNULL(phi_van_chuyen, 0) ELSE 0 END,
    phi_van_chuyen = 0,
    don_vi_van_chuyen = NULL
WHERE loai_hoa_don = 1 AND (ISNULL(phi_van_chuyen, 0) <> 0 OR don_vi_van_chuyen IS NOT NULL);
GO

-- 8.2 Hóa đơn mẫu giao hàng thất bại (đơn COD của KH05, giao qua Giao Hàng Tiết Kiệm, khách không nghe máy)
IF NOT EXISTS (SELECT 1 FROM hoa_don WHERE ma_hoa_don = 'HD19')
   AND EXISTS (SELECT 1 FROM nhan_vien WHERE ma_nhan_vien = 'NV04')
   AND EXISTS (SELECT 1 FROM khach_hang WHERE ma_khach_hang = 'KH05')
   AND EXISTS (SELECT 1 FROM phuong_thuc_thanh_toan WHERE ma_phuong_thuc = 'PT03')
   AND EXISTS (SELECT 1 FROM san_pham_chi_tiet WHERE ma_spct = 'SP04-DEN-4U-G5')
BEGIN
    DECLARE @tao DATETIME = DATEADD(MINUTE, -3000, GETDATE());
    DECLARE @gia DECIMAL(18,0) = (SELECT TOP 1 ROUND(gia_ban, 0) FROM san_pham_chi_tiet WHERE ma_spct = 'SP04-DEN-4U-G5');
    DECLARE @id INT;

    INSERT INTO hoa_don (id_nhan_vien, id_khach_hang, id_phieu_giam_gia, id_phuong_thuc_thanh_toan, ma_hoa_don, loai_hoa_don,
                         tong_tien, so_tien_giam, phi_van_chuyen, thanh_tien, don_vi_van_chuyen, ho_ten_nguoi_nhan, sdt_nguoi_nhan,
                         dia_chi_giao_hang, ghi_chu, ngay_giao_du_kien, ngay_tao, ngay_cap_nhat, trang_thai)
    SELECT nv.id, kh.id, NULL, pt.id, 'HD19', 2,
           @gia, 0, 30000, @gia + 30000, N'Giao Hàng Tiết Kiệm', N'Hoàng Đức Anh', '0901000005',
           N'Số 5 Trần Đại Nghĩa, phường Bách Khoa, quận Hai Bà Trưng, Hà Nội', N'Gọi trước khi giao',
           DATEADD(DAY, 4, @tao), @tao, DATEADD(MINUTE, 2000, @tao), 8
    FROM nhan_vien nv, khach_hang kh, phuong_thuc_thanh_toan pt
    WHERE nv.ma_nhan_vien = 'NV04' AND kh.ma_khach_hang = 'KH05' AND pt.ma_phuong_thuc = 'PT03';

    SELECT @id = id FROM hoa_don WHERE ma_hoa_don = 'HD19';

    INSERT INTO hoa_don_chi_tiet (id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia, thanh_tien, trang_thai)
    SELECT @id, c.id, 1, @gia, @gia, 1
    FROM san_pham_chi_tiet c
    WHERE c.ma_spct = 'SP04-DEN-4U-G5';

    -- Chữ ở cột hanh_dong phải khớp với backend (HoaDonLuongTrangThai): Tạo đơn hàng, Đã xác nhận, Chờ giao hàng, Đang giao hàng, Giao hàng thất bại
    INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_thao_tac, hanh_dong, thoi_gian, mo_ta)
    SELECT @id, CASE WHEN s.code = 1 THEN N'Khách hàng' ELSE nv.ho_ten END, s.hanh_dong, DATEADD(MINUTE, s.phut, @tao), s.mo_ta
    FROM (VALUES
        (1, N'Tạo đơn hàng',      0,    N'Khách đặt hàng online'),
        (2, N'Đã xác nhận',       30,   N'Nhân viên đã gọi xác nhận đơn với khách'),
        (3, N'Chờ giao hàng',     180,  N'Đã đóng gói, chờ đơn vị vận chuyển đến lấy'),
        (4, N'Đang giao hàng',    1440, N'Đã bàn giao cho Giao Hàng Tiết Kiệm'),
        (8, N'Giao hàng thất bại', 2000, N'Khách không nghe máy sau 3 lần gọi')
    ) AS s(code, hanh_dong, phut, mo_ta)
    CROSS JOIN (SELECT ho_ten FROM nhan_vien WHERE ma_nhan_vien = 'NV04') nv;
END
GO

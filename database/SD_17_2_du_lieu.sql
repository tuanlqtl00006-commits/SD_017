-- =================================================================
-- FOOTSTYLE (SHOP CẦU LÔNG) - FILE 2/2: DỮ LIỆU (SQL SERVER)
-- =================================================================
-- Chỉ gồm DỮ LIỆU: dữ liệu mẫu + sửa dữ liệu của CSDL tạo từ bản cũ. KHÔNG tạo / sửa bảng, cột, khóa, chỉ mục.
-- Chạy SAU file 1: database/SD_17_1_cau_truc.sql (tạo database + bảng + cột + chỉ mục).
--
-- Cách chạy (SSMS: mở file, bấm Execute / F5). Chạy lại nhiều lần được: bản ghi đã có mã thì bỏ qua, dữ liệu đã đúng thì không đổi.
-- Dùng được cho cả CSDL mới lẫn CSDL đang có dữ liệu (chỉ THÊM dữ liệu mẫu còn thiếu, không xóa dữ liệu của bạn).
-- Ngày tháng tính theo HÔM NAY (lúc chạy) để luôn có đủ: phiếu / đợt đang chạy, sắp diễn ra, đã kết thúc; hóa đơn mới / cũ.
-- Mã dạng gọn: tiền tố + số tối thiểu 2 chữ số (HD01, NV01, SP01, DGG01 ... HD100 khi quá 99).
-- Mật khẩu của mọi nhân viên mẫu: Footstyle@123 (NV01, NV02 = Quản lý; NV03 - NV10 = Nhân viên; NV07, NV10 đã nghỉ).
--
-- Bố cục:
--   PHẦN 0: sửa dữ liệu cũ trước khi thêm dữ liệu mẫu (rút gọn mã HD000001 -> HD01..., chép số căn cước cột cũ)
--   PHẦN 1: vai trò, nhân viên (+ địa chỉ), khách hàng (+ ngày sinh, địa chỉ giao hàng)
--   PHẦN 2: sản phẩm - 9 bảng thuộc tính, mục "Không áp dụng", 6 sản phẩm, 14 biến thể
--   PHẦN 3: giảm giá - 11 phiếu giảm giá (+ khách được tặng phiếu cá nhân), 8 đợt giảm giá (+ biến thể áp dụng)
--   PHẦN 4: sửa dữ liệu mẫu cũ (giày -> vợt) và đơn tại quầy còn phí vận chuyển
--   PHẦN 5: thanh toán + hóa đơn mẫu HD01 - HD18, HD19 "Giao hàng thất bại"
--   PHẦN 6: thêm 25 hóa đơn "Chờ xác nhận" + 20 hóa đơn "Đang giao hàng" (ngẫu nhiên, chỉ thêm 1 lần)
-- =================================================================

USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON; -- cần khi ghi bảng có chỉ mục lọc (nhan_vien, khach_hang)
GO

-- #################################################################
-- PHẦN 0: SỬA DỮ LIỆU CŨ TRƯỚC KHI THÊM DỮ LIỆU MẪU (CSDL mới chưa có dữ liệu thì không làm gì)
-- 0.1 Rút gọn mã cũ nhiều số 0 (HD000001 -> HD01, NV0001 -> NV01, SP001 -> SP01, DGG001 -> DGG01 ...).
--     Chỉ đổi cột mã (các bảng liên kết với nhau bằng id nên không mất liên kết). Mã đã gọn thì không đổi; mã gọn sẽ trùng
--     mã đang có thì giữ nguyên mã cũ. Phải chạy TRƯỚC dữ liệu mẫu để dữ liệu mẫu (mã gọn) không bị thêm trùng với dữ liệu cũ.
--     Hàm fn_rut_gon_ma chỉ là hàm tạm: tạo ra, dùng xong xóa ngay (không để lại trong CSDL). Backend cũng tự làm việc này khi khởi động.
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

-- 0.2 Nhân viên: bản cũ từng lưu số căn cước ở cột cccd -> chép sang cột can_cuoc_cong_dan (cột tạo ở file cấu trúc).
IF COL_LENGTH('nhan_vien', 'cccd') IS NOT NULL
    EXEC('UPDATE nhan_vien SET can_cuoc_cong_dan = cccd WHERE can_cuoc_cong_dan IS NULL AND cccd IS NOT NULL');
GO

-- #################################################################
-- PHẦN 1: VAI TRÒ, NHÂN VIÊN, KHÁCH HÀNG
-- #################################################################

-- 1.1 Vai trò (tên "Quản lý" phải khớp VaiTro.TEN_QUAN_LY ở backend)
INSERT INTO vai_tro (ten_vai_tro, trang_thai)
SELECT v.ten, 1 FROM (VALUES (N'Quản lý'), (N'Nhân viên')) AS v(ten)
WHERE NOT EXISTS (SELECT 1 FROM vai_tro r WHERE r.ten_vai_tro = v.ten);
GO

-- 1.2 Nhân viên
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

-- 1.3 Địa chỉ nhân viên: nhân viên chưa có địa chỉ nào thì địa chỉ ở cột dia_chi thành địa chỉ chính (cả nhân viên của CSDL cũ);
--     thêm nhiều địa chỉ khác trên giao diện. Chạy lại không bị nhân đôi.
INSERT INTO dia_chi_nhan_vien (id_nhan_vien, dia_chi_chi_tiet, la_mac_dinh)
SELECT n.id, n.dia_chi, 1
FROM nhan_vien n
WHERE n.dia_chi IS NOT NULL AND LTRIM(RTRIM(n.dia_chi)) <> ''
  AND NOT EXISTS (SELECT 1 FROM dia_chi_nhan_vien d WHERE d.id_nhan_vien = n.id);
GO

-- 1.4 Khách hàng (để chọn khi tặng phiếu giảm giá cá nhân)
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

-- 1.5 Bổ sung ngày sinh cho khách hàng mẫu (nếu chưa có)
UPDATE khach_hang SET ngay_sinh = '1995-03-12' WHERE ma_khach_hang = 'KH01' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1992-07-25' WHERE ma_khach_hang = 'KH02' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1998-11-02' WHERE ma_khach_hang = 'KH03' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1996-05-20' WHERE ma_khach_hang = 'KH04' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '1994-09-08' WHERE ma_khach_hang = 'KH05' AND ngay_sinh IS NULL;
UPDATE khach_hang SET ngay_sinh = '2000-01-15' WHERE ma_khach_hang = 'KH06' AND ngay_sinh IS NULL;
GO

-- 1.6 Địa chỉ giao hàng mặc định cho khách hàng mẫu (cả 6 khách hàng KH01 - KH06 đều có địa chỉ)
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

-- #################################################################
-- PHẦN 2: SẢN PHẨM - 9 bảng thuộc tính, mục "Không áp dụng", sản phẩm, biến thể
-- #################################################################

-- 2.1 Danh mục
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

-- 2.2 Thương hiệu
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

-- 2.3 Xuất xứ
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

-- 2.4 Chất liệu
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

-- 2.5 Độ cứng
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

-- 2.6 Điểm cân bằng
INSERT INTO diem_can_bang (ma_diem_can_bang, ten_diem_can_bang, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('CB01', N'Nặng đầu'),
    ('CB02', N'Cân bằng'),
    ('CB03', N'Nhẹ đầu')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM diem_can_bang t WHERE t.ma_diem_can_bang = v.ma);
GO

-- 2.7 Màu sắc
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

-- 2.8 Trọng lượng
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

-- 2.9 Chu vi
INSERT INTO chu_vi (ma_chu_vi, ten_chu_vi, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('CV01', N'G4'),
    ('CV02', N'G5'),
    ('CV03', N'G6')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM chu_vi t WHERE t.ma_chu_vi = v.ma);
GO

-- 2.10 Mục "Không áp dụng": danh mục không phải vợt (Phụ kiện, Túi - balo, Quần áo thể thao, Quả cầu lông...) không có
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

-- 2.11 Sản phẩm (trỏ tới thuộc tính bằng mã)
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

-- 2.12 Biến thể = sản phẩm + màu sắc + trọng lượng + chu vi
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

-- #################################################################
-- PHẦN 3: GIẢM GIÁ - phiếu giảm giá (+ khách được tặng phiếu cá nhân), đợt giảm giá (+ biến thể áp dụng)
-- #################################################################

-- 3.1 Phiếu giảm giá (ngày tính theo hôm nay để luôn có đủ: đang hoạt động / sắp diễn ra / đã kết thúc / ngưng)
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

-- 3.2 Khách hàng được tặng phiếu cá nhân
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

-- =================================================================
-- 3.3 ĐỢT GIẢM GIÁ  (trang_thai: 1 = Đang hoạt động, 0 = Ngừng hoạt động)
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

-- 3.4 Biến thể được giảm trong từng đợt
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
-- PHẦN 4: SỬA DỮ LIỆU MẪU CŨ (CSDL mới thì không có gì để sửa)
-- #################################################################

-- 4.1 Cửa hàng chỉ bán vợt cầu lông: đổi dữ liệu mẫu giày cũ (nếu có) thành vợt. Không đổi giá, không đổi hóa đơn cũ.
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

-- 4.2 Đơn TẠI QUẦY (loai_hoa_don = 1) nhận hàng ngay ở cửa hàng nên KHÔNG có phí vận chuyển và đơn vị vận chuyển:
--     dọn dữ liệu cũ còn sót; thành tiền trừ lại phần phí đã cộng (không để âm). Backend cũng tự chạy lệnh này khi khởi động.
UPDATE hoa_don
SET thanh_tien = CASE WHEN thanh_tien > ISNULL(phi_van_chuyen, 0) THEN thanh_tien - ISNULL(phi_van_chuyen, 0) ELSE 0 END,
    phi_van_chuyen = 0,
    don_vi_van_chuyen = NULL
WHERE loai_hoa_don = 1 AND (ISNULL(phi_van_chuyen, 0) <> 0 OR don_vi_van_chuyen IS NOT NULL);
GO

-- #################################################################
-- PHẦN 5: THANH TOÁN + HÓA ĐƠN MẪU
-- Cột hoa_don.trang_thai: 0 Đã hủy, 1 Chờ xác nhận, 2 Đã xác nhận, 3 Chờ giao hàng, 4 Đang giao hàng, 5 Đã giao hàng,
-- 6 Đã hoàn thành, 7 Hoàn tiền (chỉ để dành cho tab "Hoàn tiền"), 8 Giao hàng thất bại.
-- #################################################################

-- 5.1 Hình thức thanh toán
INSERT INTO hinh_thuc_thanh_toan (ma_hinh_thuc, ten_hinh_thuc, trang_thai)
SELECT v.ma, v.ten, 1
FROM (VALUES
    ('HT01', N'Tiền mặt'),
    ('HT02', N'Chuyển khoản')
) AS v(ma, ten)
WHERE NOT EXISTS (SELECT 1 FROM hinh_thuc_thanh_toan t WHERE t.ma_hinh_thuc = v.ma);
GO

-- 5.2 Phương thức thanh toán
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
-- 5.3 HÓA ĐƠN MẪU HD01 - HD18
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
-- 5.4 Chi tiết hóa đơn. giam = % đợt giảm giá đang áp dụng lúc mua (0 = không giảm)
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
-- 5.5 Tính lại tổng tiền / tiền giảm (theo phiếu giảm giá) / thành tiền từ chi tiết
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
-- 5.6 Lịch sử hóa đơn (dòng thời gian trên trang chi tiết)
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
-- 5.7 Lịch sử thanh toán
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
-- 5.8 Cập nhật phiếu giảm giá đã dùng trong các hóa đơn trên
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

-- 5.9 Hóa đơn mẫu HD19 "Giao hàng thất bại": đơn COD của KH05, giao qua Giao Hàng Tiết Kiệm, khách không nghe máy
--      (đủ chi tiết + lịch sử, có lý do). Không tạo lại nếu đã có HD19.
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

-- #################################################################
-- PHẦN 6: THÊM NHIỀU HÓA ĐƠN "CHỜ XÁC NHẬN" VÀ "ĐANG GIAO HÀNG" (dữ liệu demo cho trang Quản lý hóa đơn)
-- Mặc định thêm 25 đơn Chờ xác nhận (trang_thai = 1) + 20 đơn Đang giao hàng (trang_thai = 4); đổi 2 số @so_... ở dưới nếu muốn.
-- Tất cả là đơn giao hàng / online (loai_hoa_don = 2), mỗi đơn có đủ: chi tiết sản phẩm, lịch sử thao tác, lịch sử thanh toán
-- (đơn trả trước bằng chuyển khoản / ví điện tử; đơn COD thu tiền khi giao nên chưa có).
-- Khách hàng, nhân viên, địa chỉ giao, sản phẩm, phương thức thanh toán LẤY NGẪU NHIÊN từ dữ liệu đang có (khách / nhân viên đang
-- hoạt động, biến thể còn hàng). Mã hóa đơn nối tiếp mã lớn nhất hiện có (vd đang có tới HD19 thì thêm từ HD20).
-- CHẠY LẠI: mặc định KHÔNG thêm nữa (nhận biết qua dòng lịch sử "đơn mẫu bổ sung"). Muốn thêm một lô nữa thì đổi @them_lai = 1.
-- Muốn xóa lô đã thêm: xem khối lệnh trong ngoặc /* ... */ ở CUỐI file. Chạy trong 1 transaction: lỗi giữa chừng thì tự hoàn tác.
-- Giống các hóa đơn mẫu khác: KHÔNG trừ tồn kho. Lưu ý khi demo: HỦY một đơn thì hệ thống cộng số lượng của đơn đó vào tồn kho.
-- #################################################################

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

DECLARE @so_cho_xac_nhan INT = 25;     -- số đơn trạng thái 1 - Chờ xác nhận
DECLARE @so_dang_giao    INT = 20;     -- số đơn trạng thái 4 - Đang giao hàng
DECLARE @them_lai        BIT = 0;      -- 1 = cho phép thêm thêm một lô nữa dù đã từng chạy

DECLARE @mo_ta_tao NVARCHAR(100) = N'Khách đặt hàng online (đơn mẫu bổ sung)'; -- dòng này cũng là dấu nhận biết lô đã thêm
DECLARE @bay_gio   DATETIME      = GETDATE();

IF @them_lai = 0 AND EXISTS (SELECT 1 FROM lich_su_hoa_don WHERE mo_ta = @mo_ta_tao)
BEGIN
    PRINT N'Đã thêm hóa đơn mẫu bổ sung trước đó nên bỏ qua (muốn thêm lô nữa thì đặt @them_lai = 1).';
    RETURN;
END

-- ---------- 1. Nguồn dữ liệu lấy từ database hiện có ----------
IF OBJECT_ID('tempdb..#dh_kh')  IS NOT NULL DROP TABLE #dh_kh;
IF OBJECT_ID('tempdb..#dh_nv')  IS NOT NULL DROP TABLE #dh_nv;
IF OBJECT_ID('tempdb..#dh_pt')  IS NOT NULL DROP TABLE #dh_pt;
IF OBJECT_ID('tempdb..#dh_bt')  IS NOT NULL DROP TABLE #dh_bt;
IF OBJECT_ID('tempdb..#dh_dv')  IS NOT NULL DROP TABLE #dh_dv;
IF OBJECT_ID('tempdb..#dh_dc')  IS NOT NULL DROP TABLE #dh_dc;
IF OBJECT_ID('tempdb..#dh_goc') IS NOT NULL DROP TABLE #dh_goc;
IF OBJECT_ID('tempdb..#dh_don') IS NOT NULL DROP TABLE #dh_don;
IF OBJECT_ID('tempdb..#dh_ct')  IS NOT NULL DROP TABLE #dh_ct;
IF OBJECT_ID('tempdb..#dh_dong') IS NOT NULL DROP TABLE #dh_dong;
IF OBJECT_ID('tempdb..#dh_tong') IS NOT NULL DROP TABLE #dh_tong;

-- Khách hàng đang hoạt động + địa chỉ giao (ưu tiên địa chỉ mặc định). Khách chưa có địa chỉ thì dùng địa chỉ dự phòng ở bước 2.
SELECT ROW_NUMBER() OVER (ORDER BY k.id) AS stt,
       k.id, k.ho_ten, k.sdt,
       dc.ho_ten_nguoi_nhan, dc.sdt_nguoi_nhan, dc.dia_chi
INTO #dh_kh
FROM khach_hang k
OUTER APPLY (
    SELECT TOP 1 d.ho_ten_nguoi_nhan, d.sdt_nguoi_nhan,
           d.dia_chi_chi_tiet
             + ISNULL(N', ' + NULLIF(LTRIM(RTRIM(d.phuong_xa)), N''), N'')
             + ISNULL(N', ' + NULLIF(LTRIM(RTRIM(d.quan_huyen)), N''), N'')
             + ISNULL(N', ' + NULLIF(LTRIM(RTRIM(d.tinh_thanh)), N''), N'') AS dia_chi
    FROM dia_chi_khach_hang d
    WHERE d.id_khach_hang = k.id AND ISNULL(d.trang_thai, 1) = 1
    ORDER BY ISNULL(d.la_mac_dinh, 0) DESC, d.id
) dc
WHERE k.trang_thai = 1 AND k.sdt IS NOT NULL;

-- Nhân viên đang hoạt động
SELECT ROW_NUMBER() OVER (ORDER BY id) AS stt, id, ho_ten
INTO #dh_nv
FROM nhan_vien
WHERE trang_thai = 1;

-- Phương thức thanh toán cho đơn giao hàng (bỏ PT01 "Tiền mặt" vì đó là thanh toán tại quầy)
SELECT ROW_NUMBER() OVER (ORDER BY id) AS stt, id, ma_phuong_thuc
INTO #dh_pt
FROM phuong_thuc_thanh_toan
WHERE trang_thai = 1 AND ma_phuong_thuc <> 'PT01';

-- Biến thể sản phẩm còn hàng (giá bán niêm yết; sản phẩm cha cũng phải đang kinh doanh)
SELECT ROW_NUMBER() OVER (ORDER BY c.id) AS stt, c.id, c.gia_ban
INTO #dh_bt
FROM san_pham_chi_tiet c
JOIN san_pham s ON s.id = c.id_san_pham AND s.trang_thai = 1
WHERE c.trang_thai = 1 AND c.gia_ban > 0 AND c.so_luong_ton > 0;

DECLARE @so_kh INT = (SELECT COUNT(*) FROM #dh_kh);
DECLARE @so_nv INT = (SELECT COUNT(*) FROM #dh_nv);
DECLARE @so_pt INT = (SELECT COUNT(*) FROM #dh_pt);
DECLARE @so_bt INT = (SELECT COUNT(*) FROM #dh_bt);

IF @so_kh = 0 OR @so_nv = 0 OR @so_pt = 0 OR @so_bt = 0
BEGIN
    DECLARE @loi NVARCHAR(400) = N'Chưa đủ dữ liệu nền để thêm hóa đơn mẫu. Cần có: khách hàng đang hoạt động (có SĐT) = '
        + CAST(@so_kh AS NVARCHAR(10)) + N', nhân viên đang hoạt động = ' + CAST(@so_nv AS NVARCHAR(10))
        + N', phương thức thanh toán khác PT01 = ' + CAST(@so_pt AS NVARCHAR(10))
        + N', biến thể sản phẩm còn hàng = ' + CAST(@so_bt AS NVARCHAR(10)) + N' (mỗi số phải >= 1).';
    RAISERROR(@loi, 16, 1);
    RETURN;
END

-- ---------- 2. Đơn vị vận chuyển (3 đơn vị giao diện nhận ra logo) + địa chỉ dự phòng cho khách chưa có địa chỉ ----------
SELECT v.stt, v.ten, v.phi
INTO #dh_dv
FROM (VALUES
    (1, N'Giao Hàng Nhanh',      CAST(30000 AS DECIMAL(18,0))),
    (2, N'Giao Hàng Tiết Kiệm',  CAST(30000 AS DECIMAL(18,0))),
    (3, N'Viettel Post',         CAST(35000 AS DECIMAL(18,0)))
) AS v(stt, ten, phi);

SELECT v.stt, v.dia_chi
INTO #dh_dc
FROM (VALUES
    (1, N'Số 24 Nguyễn Văn Cừ, phường Ngọc Lâm, quận Long Biên, Hà Nội'),
    (2, N'Số 15 Phan Chu Trinh, phường Hải Châu 1, quận Hải Châu, Đà Nẵng'),
    (3, N'Số 63 Võ Văn Tần, phường 6, quận 3, TP. Hồ Chí Minh'),
    (4, N'Số 9 Trần Hưng Đạo, phường Hồng Bàng, quận Hồng Bàng, Hải Phòng'),
    (5, N'Số 130 Nguyễn Trãi, phường Thượng Đình, quận Thanh Xuân, Hà Nội'),
    (6, N'Số 47 Lê Duẩn, phường Cửa Nam, quận Hoàn Kiếm, Hà Nội')
) AS v(stt, dia_chi);

-- ---------- 3. Sinh ngẫu nhiên các đơn (mỗi giá trị ngẫu nhiên chỉ tính 1 lần rồi lưu vào bảng tạm) ----------
-- phut = thời điểm tạo đơn, tính bằng phút so với lúc chạy (âm = trước đó).
--   Chờ xác nhận: tạo trong vòng ~3 ngày gần đây.
--   Đang giao hàng: tạo cách đây từ ~1 ngày 3 giờ tới ~3,9 ngày (mọi mốc xác nhận / chờ giao / bàn giao vận chuyển đã xảy ra trong quá khứ,
--   còn ngày giao dự kiến = ngày tạo + 4 ngày thì chưa tới hạn).
SELECT n.stt,
       CASE WHEN n.stt <= @so_cho_xac_nhan THEN 1 ELSE 4 END AS trang_thai,
       1 + ABS(CHECKSUM(NEWID()) % 3) AS so_dong,
       1 + ABS(CHECKSUM(NEWID()) % @so_kh) AS i_kh,
       1 + ABS(CHECKSUM(NEWID()) % @so_nv) AS i_nv,
       1 + ABS(CHECKSUM(NEWID()) % @so_pt) AS i_pt,
       1 + ABS(CHECKSUM(NEWID()) % 3) AS i_dv,
       ABS(CHECKSUM(NEWID()) % 3) AS i_ghi_chu,
       CASE WHEN n.stt <= @so_cho_xac_nhan
            THEN -(10 + ABS(CHECKSUM(NEWID()) % 4300))
            ELSE -(1600 + ABS(CHECKSUM(NEWID()) % 4000)) END AS phut
INTO #dh_goc
FROM (
    SELECT TOP (@so_cho_xac_nhan + @so_dang_giao)
           ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS stt
    FROM sys.all_objects a CROSS JOIN sys.all_objects b
) n;

-- Mã hóa đơn nối tiếp mã lớn nhất hiện có; đơn tạo sớm hơn có mã nhỏ hơn (HD09 -> HD10 ... HD100 khi quá 99)
DECLARE @ma_lon_nhat INT = ISNULL((
    SELECT MAX(TRY_CAST(SUBSTRING(ma_hoa_don, 3, 20) AS INT))
    FROM hoa_don
    WHERE ma_hoa_don LIKE 'HD[0-9]%'
), 0);

SELECT g.*,
       'HD' + CASE WHEN @ma_lon_nhat + g.thu_tu < 100
                   THEN RIGHT('00' + CAST(@ma_lon_nhat + g.thu_tu AS VARCHAR(10)), 2)
                   ELSE CAST(@ma_lon_nhat + g.thu_tu AS VARCHAR(10)) END AS ma
INTO #dh_don
FROM (SELECT x.*, ROW_NUMBER() OVER (ORDER BY x.phut, x.stt) AS thu_tu FROM #dh_goc x) g;

-- Các dòng sản phẩm: mỗi đơn 1-3 biến thể ngẫu nhiên; cùng biến thể xuất hiện 2 lần thì gộp số lượng
SELECT d.stt, 1 + ABS(CHECKSUM(NEWID()) % @so_bt) AS i_bt, ABS(CHECKSUM(NEWID()) % 4) AS x_sl
INTO #dh_ct
FROM #dh_don d
JOIN (VALUES (1), (2), (3)) AS k(dong) ON k.dong <= d.so_dong;

SELECT c.stt, b.id AS id_san_pham_chi_tiet, b.gia_ban AS don_gia,
       CASE WHEN SUM(CASE WHEN c.x_sl = 0 AND b.gia_ban < 3000000 THEN 2 ELSE 1 END) > 3 THEN 3
            ELSE SUM(CASE WHEN c.x_sl = 0 AND b.gia_ban < 3000000 THEN 2 ELSE 1 END) END AS so_luong
INTO #dh_dong
FROM #dh_ct c
JOIN #dh_bt b ON b.stt = c.i_bt
GROUP BY c.stt, b.id, b.gia_ban;

SELECT stt, SUM(don_gia * so_luong) AS tong_tien
INTO #dh_tong
FROM #dh_dong
GROUP BY stt;

-- ---------- 4. Ghi vào CSDL (1 transaction) ----------
BEGIN TRY
    BEGIN TRANSACTION;

    -- 4.1 Hóa đơn. Đơn từ 5.000.000đ trở lên miễn phí vận chuyển (giống hóa đơn mẫu HD08).
    INSERT INTO hoa_don (id_nhan_vien, id_khach_hang, id_phieu_giam_gia, id_phuong_thuc_thanh_toan, ma_hoa_don, loai_hoa_don,
                         tong_tien, so_tien_giam, phi_van_chuyen, thanh_tien, don_vi_van_chuyen, ho_ten_nguoi_nhan, sdt_nguoi_nhan,
                         dia_chi_giao_hang, ghi_chu, ngay_giao_du_kien, ngay_tao, ngay_cap_nhat, trang_thai)
    SELECT nv.id, kh.id, NULL, pt.id, d.ma, 2,
           t.tong_tien, 0,
           CASE WHEN t.tong_tien >= 5000000 THEN 0 ELSE dv.phi END,
           t.tong_tien + CASE WHEN t.tong_tien >= 5000000 THEN 0 ELSE dv.phi END,
           dv.ten,
           ISNULL(kh.ho_ten_nguoi_nhan, kh.ho_ten),
           ISNULL(kh.sdt_nguoi_nhan, kh.sdt),
           ISNULL(kh.dia_chi, dc.dia_chi),
           CASE WHEN t.tong_tien >= 5000000 THEN N'Miễn phí vận chuyển'
                WHEN d.i_ghi_chu = 1 THEN N'Gọi trước khi giao'
                WHEN d.i_ghi_chu = 2 THEN N'Giao giờ hành chính' END,
           DATEADD(DAY, 4, DATEADD(MINUTE, d.phut, @bay_gio)),
           DATEADD(MINUTE, d.phut, @bay_gio),
           DATEADD(MINUTE, d.phut, @bay_gio),
           d.trang_thai
    FROM #dh_don d
    JOIN #dh_tong t  ON t.stt  = d.stt
    JOIN #dh_kh   kh ON kh.stt = d.i_kh
    JOIN #dh_nv   nv ON nv.stt = d.i_nv
    JOIN #dh_pt   pt ON pt.stt = d.i_pt
    JOIN #dh_dv   dv ON dv.stt = d.i_dv
    JOIN #dh_dc   dc ON dc.stt = 1 + (d.stt % 6)
    WHERE NOT EXISTS (SELECT 1 FROM hoa_don x WHERE x.ma_hoa_don = d.ma);

    -- 4.2 Chi tiết hóa đơn
    INSERT INTO hoa_don_chi_tiet (id_hoa_don, id_san_pham_chi_tiet, so_luong, don_gia, thanh_tien, trang_thai)
    SELECT h.id, l.id_san_pham_chi_tiet, l.so_luong, l.don_gia, l.don_gia * l.so_luong, 1
    FROM #dh_dong l
    JOIN #dh_don d ON d.stt = l.stt
    JOIN hoa_don h ON h.ma_hoa_don = d.ma
    WHERE NOT EXISTS (SELECT 1 FROM hoa_don_chi_tiet x WHERE x.id_hoa_don = h.id);

    -- 4.3 Lịch sử thao tác. Chữ ở cột hanh_dong phải khớp backend (HoaDonLuongTrangThai):
    --     Tạo đơn hàng, Đã xác nhận, Chờ giao hàng, Đang giao hàng. Chờ xác nhận mới chỉ có bước 1; Đang giao hàng đi tới bước 4.
    INSERT INTO lich_su_hoa_don (id_hoa_don, nguoi_thao_tac, hanh_dong, thoi_gian, mo_ta)
    SELECT h.id,
           CASE WHEN b.code = 1 THEN N'Khách hàng' ELSE nv.ho_ten END,
           b.hanh_dong,
           DATEADD(MINUTE, b.phut, h.ngay_tao),
           CASE WHEN b.code = 1 THEN @mo_ta_tao
                WHEN b.code = 4 THEN N'Đã bàn giao cho ' + h.don_vi_van_chuyen
                ELSE b.mo_ta END
    FROM hoa_don h
    JOIN #dh_don d ON d.ma = h.ma_hoa_don
    LEFT JOIN nhan_vien nv ON nv.id = h.id_nhan_vien
    JOIN (VALUES
        (1, N'Tạo đơn hàng',   0,    N''),
        (2, N'Đã xác nhận',    30,   N'Nhân viên đã gọi xác nhận đơn với khách'),
        (3, N'Chờ giao hàng',  180,  N'Đã đóng gói, chờ đơn vị vận chuyển đến lấy'),
        (4, N'Đang giao hàng', 1440, N'')
    ) AS b(code, hanh_dong, phut, mo_ta) ON b.code <= d.trang_thai
    WHERE NOT EXISTS (SELECT 1 FROM lich_su_hoa_don x WHERE x.id_hoa_don = h.id);

    -- Ngày cập nhật = lần thao tác cuối
    UPDATE h
    SET ngay_cap_nhat = ls.thoi_gian_cuoi
    FROM hoa_don h
    JOIN #dh_don d ON d.ma = h.ma_hoa_don
    JOIN (SELECT id_hoa_don, MAX(thoi_gian) AS thoi_gian_cuoi FROM lich_su_hoa_don GROUP BY id_hoa_don) ls ON ls.id_hoa_don = h.id;

    -- 4.4 Lịch sử thanh toán: chỉ đơn trả trước (PT02 chuyển khoản, PT04 VNPay) - khách đã trả ngay sau khi đặt.
    --     Đơn COD thu tiền khi giao thành công nên chưa có dòng nào.
    INSERT INTO lich_su_thanh_toan (id_hoa_don, so_tien, ma_giao_dich, thoi_gian, trang_thai, mo_ta)
    SELECT h.id, h.thanh_tien,
           CASE WHEN pt.ma_phuong_thuc = 'PT04' THEN 'VNP' ELSE 'CK' END
             + REPLACE(h.ma_hoa_don, 'HD', '')
             + CASE WHEN pt.ma_phuong_thuc = 'PT04' THEN '88' ELSE '01' END,
           DATEADD(MINUTE, 5, h.ngay_tao), 1, N'Khách thanh toán trước khi giao hàng'
    FROM hoa_don h
    JOIN #dh_don d ON d.ma = h.ma_hoa_don
    JOIN phuong_thuc_thanh_toan pt ON pt.id = h.id_phuong_thuc_thanh_toan
    WHERE pt.ma_phuong_thuc IN ('PT02', 'PT04')
      AND NOT EXISTS (SELECT 1 FROM lich_su_thanh_toan x WHERE x.id_hoa_don = h.id);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH

-- ---------- 5. Kết quả: các hóa đơn vừa thêm + số đơn mỗi tab trên trang Quản lý hóa đơn ----------
SELECT h.ma_hoa_don AS [Mã HĐ],
       CASE h.trang_thai WHEN 1 THEN N'Chờ xác nhận' WHEN 4 THEN N'Đang giao hàng' END AS [Trạng thái],
       k.ho_ten AS [Khách hàng], h.ho_ten_nguoi_nhan AS [Người nhận], h.don_vi_van_chuyen AS [Vận chuyển],
       (SELECT COUNT(*) FROM hoa_don_chi_tiet c WHERE c.id_hoa_don = h.id) AS [Số dòng SP],
       h.tong_tien AS [Tổng tiền], h.phi_van_chuyen AS [Phí ship], h.thanh_tien AS [Thành tiền], h.ngay_tao AS [Ngày tạo]
FROM hoa_don h
JOIN #dh_don d ON d.ma = h.ma_hoa_don
LEFT JOIN khach_hang k ON k.id = h.id_khach_hang
ORDER BY h.trang_thai, h.ngay_tao DESC;

SELECT CASE trang_thai WHEN 0 THEN N'Đã hủy' WHEN 1 THEN N'Chờ xác nhận' WHEN 2 THEN N'Đã xác nhận' WHEN 3 THEN N'Chờ giao hàng'
                       WHEN 4 THEN N'Đang giao hàng' WHEN 5 THEN N'Đã giao hàng' WHEN 6 THEN N'Đã hoàn thành'
                       WHEN 7 THEN N'Hoàn tiền' WHEN 8 THEN N'Giao hàng thất bại' END AS [Tab trạng thái],
       COUNT(*) AS [Số hóa đơn]
FROM hoa_don
GROUP BY trang_thai
ORDER BY trang_thai;

DROP TABLE #dh_kh, #dh_nv, #dh_pt, #dh_bt, #dh_dv, #dh_dc, #dh_goc, #dh_don, #dh_ct, #dh_dong, #dh_tong;
GO

SET XACT_ABORT OFF;
SET NOCOUNT OFF;
GO

-- =================================================================
-- KIỂM TRA NHANH: số bản ghi mỗi bảng chính
-- =================================================================
SELECT N'Nhân viên' AS [Bảng], COUNT(*) AS [Số dòng] FROM nhan_vien
UNION ALL SELECT N'Khách hàng', COUNT(*) FROM khach_hang
UNION ALL SELECT N'Sản phẩm', COUNT(*) FROM san_pham
UNION ALL SELECT N'Biến thể sản phẩm', COUNT(*) FROM san_pham_chi_tiet
UNION ALL SELECT N'Phiếu giảm giá', COUNT(*) FROM phieu_giam_gia
UNION ALL SELECT N'Đợt giảm giá', COUNT(*) FROM dot_giam_gia
UNION ALL SELECT N'Phương thức thanh toán', COUNT(*) FROM phuong_thuc_thanh_toan
UNION ALL SELECT N'Hóa đơn', COUNT(*) FROM hoa_don;
GO

-- =================================================================
-- (TÙY CHỌN) XÓA LÔ HÓA ĐƠN MẪU BỔ SUNG VỪA THÊM - bỏ dấu comment rồi chạy khi muốn dọn.
-- Nhận biết bằng dòng lịch sử "Khách đặt hàng online (đơn mẫu bổ sung)". Chỉ xóa các đơn có dòng này,
-- không đụng tới hóa đơn mẫu khác.
-- =================================================================
/*
USE shop_cau_long_footstyle;
BEGIN TRANSACTION;
SELECT DISTINCT id_hoa_don INTO #xoa FROM lich_su_hoa_don WHERE mo_ta = N'Khách đặt hàng online (đơn mẫu bổ sung)';
DELETE FROM lich_su_thanh_toan WHERE id_hoa_don IN (SELECT id_hoa_don FROM #xoa);
DELETE FROM hoa_don_chi_tiet   WHERE id_hoa_don IN (SELECT id_hoa_don FROM #xoa);
DELETE FROM lich_su_hoa_don    WHERE id_hoa_don IN (SELECT id_hoa_don FROM #xoa);
DELETE FROM hoa_don            WHERE id         IN (SELECT id_hoa_don FROM #xoa);
DROP TABLE #xoa;
COMMIT TRANSACTION;
*/

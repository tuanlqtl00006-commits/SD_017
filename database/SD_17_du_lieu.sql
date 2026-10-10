-- =================================================================
-- SD_17_du_lieu.sql: FILE SQL DỮ LIỆU của shop cầu lông FootStyle (SQL Server)
-- CHỈ có dữ liệu mẫu, KHÔNG tạo database, KHÔNG tạo bảng chính (cấu trúc nằm ở SD_17_tong.sql).
-- Dùng khi đã có database (tạo từ SD_17_tong.sql) và muốn nạp / bổ sung dữ liệu.
-- Chạy lại nhiều lần được: bản ghi đã có mã thì bỏ qua, không bị nhân đôi.
-- Gồm lần lượt: 1. vai trò, nhân viên, khách hàng, phiếu giảm giá  2. thuộc tính, sản phẩm, biến thể (có mục "Không áp dụng")
--               3. địa chỉ khách hàng, đợt giảm giá  4. phương thức thanh toán, hóa đơn
-- Muốn THÊM dữ liệu của riêng bạn: viết vào phần "THÊM DỮ LIỆU MỚI Ở ĐÂY" ở cuối file rồi chạy lại.
-- =================================================================


-- #################################################################
-- PHẦN 1. Vai trò, nhân viên, khách hàng, phiếu giảm giá
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
-- PHẦN 2. Thuộc tính, sản phẩm, biến thể (kèm mục "Không áp dụng")
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
-- PHẦN 3. Địa chỉ khách hàng, đợt giảm giá
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
-- PHẦN 4. Phương thức thanh toán, hóa đơn
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
-- THÊM DỮ LIỆU MỚI Ở ĐÂY
-- Viết lệnh INSERT theo mẫu "chưa có mã thì mới thêm" để chạy lại không bị trùng. Ví dụ (bỏ dấu -- ở đầu dòng để dùng):
--
-- USE shop_cau_long_footstyle;
-- GO
-- INSERT INTO thuong_hieu (ma_thuong_hieu, ten_thuong_hieu, trang_thai)
-- SELECT v.ma, v.ten, 1
-- FROM (VALUES ('TH099', N'Tên thương hiệu mới')) AS v(ma, ten)
-- WHERE NOT EXISTS (SELECT 1 FROM thuong_hieu t WHERE t.ma_thuong_hieu = v.ma);
-- GO
-- #################################################################

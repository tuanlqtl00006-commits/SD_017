-- =================================================================
-- DỮ LIỆU MẪU: vai trò, nhân viên, khách hàng, phiếu giảm giá
-- Chạy SAU khi đã chạy SD_17.sql. Có thể chạy lại nhiều lần (bản ghi đã có mã thì bỏ qua).
-- Mật khẩu của mọi nhân viên mẫu: Footstyle@123 (đã băm BCrypt, đăng nhập sẽ dùng cùng thuật toán).
-- =================================================================

USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON; -- cần khi ghi bảng có chỉ mục lọc (nhan_vien)
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

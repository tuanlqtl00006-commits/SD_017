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
    ('DM002', N'Giày cầu lông'),
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
    ('CL004', N'Vải lưới'),
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

-- 10. Sản phẩm (trỏ tới thuộc tính bằng mã)
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang,
                      ma_san_pham, ten_san_pham, mo_ta, trang_thai)
SELECT dm.id, th.id, xx.id, cl.id, dc.id, cb.id, s.ma, s.ten, s.mo_ta, 1
FROM (VALUES
    ('SP001', N'Yonex Astrox 99 Pro', 'DM001', 'TH001', 'XX001', 'CL001', 'DC003', 'CB001', N'Vợt thiên công cao cấp, khung carbon.'),
    ('SP002', N'Lining Axforce 80', 'DM001', 'TH002', 'XX002', 'CL001', 'DC004', 'CB001', N'Vợt dành cho người chơi lực tay tốt.'),
    ('SP003', N'Victor Thruster K 9900', 'DM001', 'TH003', 'XX003', 'CL002', 'DC003', 'CB001', N'Vợt thiên công, nặng đầu, đập cầu mạnh.'),
    ('SP004', N'Yonex Power Cushion 65Z3', 'DM002', 'TH001', 'XX001', 'CL004', 'DC002', 'CB002', N'Giày cầu lông đệm Power Cushion giảm chấn.'),
    ('SP005', N'Mizuno Wave Fang Pro', 'DM002', 'TH004', 'XX001', 'CL004', 'DC002', 'CB002', N'Giày cầu lông đế êm, bám sân tốt.'),
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
    ('SP004-DEN-40', 'SP004', 'MS001', 'TL001', 'CV001', 2790000, 12),
    ('SP005-TRG-40', 'SP005', 'MS002', 'TL001', 'CV001', 2350000, 9),
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

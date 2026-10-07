-- =================================================================
-- DỮ LIỆU MẪU: đợt giảm giá, phương thức thanh toán, hóa đơn (+ chi tiết, lịch sử, thanh toán)
-- Chạy SAU 3 file: SD_17.sql -> SD_17_du_lieu_mau.sql -> SD_17_du_lieu_san_pham.sql
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
    ('DGG002', N'Xả hàng giày cầu lông', 15, -40, -25, 1),
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
    ('DGG002', 'SP004-DEN-40'), ('DGG002', 'SP005-TRG-40'),
    ('DGG003', 'SP002-DEN-4U-G5'), ('DGG003', 'SP002-DEN-4U-G4'), ('DGG003', 'SP002-XDG-3U-G5'),
    ('DGG004', 'SP003-DEN-4U-G5'), ('DGG004', 'SP003-DO-3U-G5'),
    ('DGG005', 'SP006-DEN-4U-G5'), ('DGG005', 'SP001-DEN-4U-G5'),
    ('DGG006', 'SP006-DEN-4U-G5'),
    ('DGG007', 'SP002-DEN-4U-G5'), ('DGG007', 'SP004-DEN-40'),
    ('DGG008', 'SP001-DEN-4U-G5'), ('DGG008', 'SP004-DEN-40'), ('DGG008', 'SP005-TRG-40')
) AS v(ma_dot, ma_spct)
JOIN dot_giam_gia d ON d.ma_dot = v.ma_dot
JOIN san_pham_chi_tiet c ON c.ma_spct = v.ma_spct
WHERE NOT EXISTS (SELECT 1 FROM dot_giam_gia_chi_tiet x WHERE x.id_dot_giam_gia = d.id AND x.id_san_pham_chi_tiet = c.id);
GO

-- =================================================================
-- 5. HÓA ĐƠN
--    loai: 1 = Tại quầy, 2 = Giao hàng
--    trang_thai: 0 Đã hủy, 1 Chờ xác nhận, 2 Đã xác nhận, 3 Chờ lấy hàng, 4 Đang giao hàng, 5 Đã giao hàng, 6 Hoàn thành
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
    ('HD000005', 'SP004-DEN-40', 1, 0),
    ('HD000005', 'SP006-DEN-4U-G5', 2, 0),
    ('HD000006', 'SP001-DO-4U-G5', 1, 10),
    ('HD000007', 'SP005-TRG-40', 1, 0),
    ('HD000008', 'SP002-DEN-4U-G5', 1, 0),
    ('HD000008', 'SP003-DO-3U-G5', 1, 0),
    ('HD000009', 'SP006-DEN-4U-G5', 1, 0),
    ('HD000010', 'SP005-TRG-40', 1, 0),
    ('HD000011', 'SP003-DEN-4U-G5', 1, 0),
    ('HD000011', 'SP006-DEN-4U-G5', 1, 0),
    ('HD000012', 'SP004-DEN-40', 1, 15),
    ('HD000012', 'SP005-TRG-40', 1, 15),
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
--    Chữ ở cột hanh_dong phải khớp với backend: Tạo đơn hàng, Đã xác nhận, Chờ lấy hàng,
--    Đang giao hàng, Đã giao hàng, Hoàn thành, Đã hủy.
-- =================================================================
IF OBJECT_ID('tempdb..#buoc') IS NOT NULL DROP TABLE #buoc;
CREATE TABLE #buoc (code TINYINT, hanh_dong NVARCHAR(100), phut INT, mo_ta NVARCHAR(255));
INSERT INTO #buoc VALUES
    (1, N'Tạo đơn hàng', 0, N'Khách đặt hàng online'),
    (2, N'Đã xác nhận', 30, N'Nhân viên đã gọi xác nhận đơn với khách'),
    (3, N'Chờ lấy hàng', 180, N'Đã đóng gói, chờ đơn vị vận chuyển đến lấy'),
    (4, N'Đang giao hàng', 1440, N'Đã bàn giao cho đơn vị vận chuyển'),
    (5, N'Đã giao hàng', 4320, N'Giao hàng thành công'),
    (6, N'Hoàn thành', 4380, N'Đơn hàng hoàn thành');

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
    (6, N'Hoàn thành', 3, N'Khách đã thanh toán, nhận hàng tại cửa hàng')
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

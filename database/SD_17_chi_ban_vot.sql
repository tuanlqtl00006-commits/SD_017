-- Chỉ bán vợt cầu lông (10/11): bỏ hẳn quần áo, túi - balo, phụ kiện, quả cầu khỏi dữ liệu, kể cả trong hóa đơn cũ.
-- Chỉ sửa DỮ LIỆU, không đổi cấu trúc bảng. Chạy sau các file SD_17_*.sql khác. Chạy lại lần 2 không làm gì thêm.
--   1. Hóa đơn chỉ có hàng không phải vợt: xóa (kèm lịch sử, thanh toán), trả lượt phiếu giảm giá.
--   2. Hóa đơn có cả vợt lẫn hàng khác: bỏ dòng hàng khác, tính lại tiền hàng, tiền phiếu giảm (đơn không còn đạt mức tối thiểu
--      thì bỏ phiếu), tổng thanh toán và số tiền đã thanh toán (giữ nguyên trạng thái trả đủ / chưa trả).
--   3. Ca bán hàng đã giao: tính lại số hóa đơn, doanh thu, tiền mặt, chuyển khoản, tiền mặt hệ thống / thực tế theo đúng công thức
--      GiaoCaService.tongKet (giữ nguyên chênh lệch đã ghi); tiền bàn giao không vượt tiền thực tế, tiền đầu ca sau = tiền bàn giao ca trước.
--   4. Xóa sản phẩm, biến thể, ảnh, đợt giảm giá chỉ gồm hàng bị bỏ, danh mục và chất liệu không dùng cho vợt.
SET QUOTED_IDENTIFIER ON; -- bảng có filtered index (giao ca)
SET ANSI_NULLS ON;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRAN;

IF OBJECT_ID('tempdb..#sp_bo') IS NOT NULL DROP TABLE #sp_bo;
IF OBJECT_ID('tempdb..#bt_bo') IS NOT NULL DROP TABLE #bt_bo;
IF OBJECT_ID('tempdb..#hd') IS NOT NULL DROP TABLE #hd;
IF OBJECT_ID('tempdb..#tt') IS NOT NULL DROP TABLE #tt;
IF OBJECT_ID('tempdb..#dot_bo') IS NOT NULL DROP TABLE #dot_bo;
IF OBJECT_ID('tempdb..#tl_bo') IS NOT NULL DROP TABLE #tl_bo;
IF OBJECT_ID('tempdb..#cv_bo') IS NOT NULL DROP TABLE #cv_bo;

-- Danh mục vợt: DM01 (Vợt cầu lông), DM02 (Vợt cầu lông người mới). Mọi sản phẩm thuộc danh mục khác bị bỏ.
SELECT s.id INTO #sp_bo FROM san_pham s JOIN danh_muc d ON d.id = s.id_danh_muc WHERE d.ma_danh_muc NOT IN ('DM01', 'DM02');
SELECT c.id INTO #bt_bo FROM san_pham_chi_tiet c WHERE c.id_san_pham IN (SELECT id FROM #sp_bo);
DECLARE @so_sp int = (SELECT COUNT(*) FROM #sp_bo);
DECLARE @pt_tien_mat int = (SELECT id FROM phuong_thuc_thanh_toan WHERE ma_phuong_thuc = 'PT01');
DECLARE @pt_ket_hop int = (SELECT id FROM phuong_thuc_thanh_toan WHERE ma_phuong_thuc = 'PT05');

/* ===================== 1 + 2. Hóa đơn ===================== */
SELECT h.id, h.trang_thai, h.id_phieu_giam_gia AS id_phieu, h.id_khach_hang, h.thanh_tien AS thanh_cu, ISNULL(h.phi_van_chuyen, 0) AS phi,
       ISNULL((SELECT SUM(c.thanh_tien) FROM hoa_don_chi_tiet c WHERE c.id_hoa_don = h.id AND c.id_san_pham_chi_tiet NOT IN (SELECT id FROM #bt_bo)), 0) AS tong_moi,
       (SELECT COUNT(*) FROM hoa_don_chi_tiet c WHERE c.id_hoa_don = h.id AND c.id_san_pham_chi_tiet NOT IN (SELECT id FROM #bt_bo)) AS dong_con,
       CAST(0 AS decimal(18, 0)) AS giam_moi, CAST(0 AS decimal(18, 0)) AS thanh_moi, CAST(0 AS bit) AS bo_phieu
INTO #hd
FROM hoa_don h
WHERE EXISTS (SELECT 1 FROM hoa_don_chi_tiet c WHERE c.id_hoa_don = h.id AND c.id_san_pham_chi_tiet IN (SELECT id FROM #bt_bo));

-- Tiền phiếu giảm trên tiền hàng mới, cùng công thức PhieuApDung.soTienGiam (loai_giam = 1: phần trăm, làm tròn xuống, có trần)
UPDATE x SET
    bo_phieu = CASE WHEN x.dong_con > 0 AND p.id IS NOT NULL AND x.tong_moi < ISNULL(p.gia_tri_don_hang_toi_thieu, 0) THEN 1 ELSE 0 END,
    giam_moi = CASE
        WHEN x.dong_con = 0 OR p.id IS NULL OR x.tong_moi < ISNULL(p.gia_tri_don_hang_toi_thieu, 0) OR ISNULL(p.gia_tri_giam, 0) <= 0 THEN 0
        WHEN p.loai_giam = 1 THEN
            CASE WHEN ISNULL(p.giam_toi_da, 0) > 0 AND FLOOR(x.tong_moi * CASE WHEN p.gia_tri_giam > 100 THEN 100 ELSE p.gia_tri_giam END / 100) > p.giam_toi_da
                 THEN p.giam_toi_da
                 ELSE FLOOR(x.tong_moi * CASE WHEN p.gia_tri_giam > 100 THEN 100 ELSE p.gia_tri_giam END / 100) END
        ELSE CASE WHEN p.gia_tri_giam > x.tong_moi THEN x.tong_moi ELSE p.gia_tri_giam END
    END
FROM #hd x LEFT JOIN phieu_giam_gia p ON p.id = x.id_phieu;
UPDATE #hd SET thanh_moi = tong_moi - giam_moi + phi;

-- Trả lượt phiếu của hóa đơn bị xóa / bị bỏ phiếu (đơn đã hủy thì không tính)
UPDATE p SET so_luong_da_dung = CASE WHEN ISNULL(p.so_luong_da_dung, 0) > t.n THEN p.so_luong_da_dung - t.n ELSE 0 END
FROM phieu_giam_gia p
JOIN (SELECT id_phieu, COUNT(*) AS n FROM #hd WHERE id_phieu IS NOT NULL AND trang_thai <> 0 AND (dong_con = 0 OR bo_phieu = 1) GROUP BY id_phieu) t ON t.id_phieu = p.id;
UPDATE k SET ngay_su_dung = NULL
FROM phieu_giam_gia_khach_hang k
JOIN #hd x ON x.id_phieu = k.id_phieu_giam_gia AND x.id_khach_hang = k.id_khach_hang
WHERE x.trang_thai <> 0 AND (x.dong_con = 0 OR x.bo_phieu = 1);

-- 1. Hóa đơn không còn dòng nào
DELETE FROM lich_su_thanh_toan WHERE id_hoa_don IN (SELECT id FROM #hd WHERE dong_con = 0);
DELETE FROM lich_su_hoa_don WHERE id_hoa_don IN (SELECT id FROM #hd WHERE dong_con = 0);
DELETE FROM hoa_don_chi_tiet WHERE id_hoa_don IN (SELECT id FROM #hd WHERE dong_con = 0);
DELETE FROM hoa_don WHERE id IN (SELECT id FROM #hd WHERE dong_con = 0);
DECLARE @so_hd_xoa int = @@ROWCOUNT;

-- 2. Hóa đơn còn dòng vợt
DELETE FROM hoa_don_chi_tiet WHERE id_san_pham_chi_tiet IN (SELECT id FROM #bt_bo);
UPDATE h SET tong_tien = x.tong_moi, so_tien_giam = x.giam_moi, thanh_tien = x.thanh_moi,
             id_phieu_giam_gia = CASE WHEN x.bo_phieu = 1 THEN NULL ELSE h.id_phieu_giam_gia END
FROM hoa_don h JOIN #hd x ON x.id = h.id
WHERE x.dong_con > 0;
DECLARE @so_hd_sua int = @@ROWCOUNT;

-- Tiền đã thanh toán: đơn nào trước đây trả đủ thì nay vẫn trả đủ số mới. Đơn trả 2 khoản (tiền mặt + chuyển khoản) thì chia theo tỉ lệ cũ,
-- khoản chuyển khoản làm tròn tới nghìn, khoản tiền mặt nhận phần còn lại.
SELECT t.id, t.id_hoa_don, x.thanh_moi,
       CAST(ROUND(t.so_tien * x.thanh_moi / x.thanh_cu, -3) AS decimal(18, 0)) AS moi,
       ROW_NUMBER() OVER (PARTITION BY t.id_hoa_don ORDER BY CASE WHEN t.id_phuong_thuc_thanh_toan = @pt_tien_mat THEN 1 ELSE 0 END, t.id) AS thu_tu,
       COUNT(*) OVER (PARTITION BY t.id_hoa_don) AS so_dong
INTO #tt
FROM lich_su_thanh_toan t JOIN #hd x ON x.id = t.id_hoa_don
WHERE x.dong_con > 0 AND x.thanh_cu > 0 AND x.thanh_moi <> x.thanh_cu
  AND x.thanh_cu = (SELECT SUM(k.so_tien) FROM lich_su_thanh_toan k WHERE k.id_hoa_don = x.id);
UPDATE a SET moi = a.thanh_moi - ISNULL((SELECT SUM(b.moi) FROM #tt b WHERE b.id_hoa_don = a.id_hoa_don AND b.thu_tu < a.thu_tu), 0)
FROM #tt a WHERE a.thu_tu = a.so_dong;
IF EXISTS (SELECT 1 FROM #tt WHERE moi < 0) THROW 50001, 'Chia lai tien thanh toan bi am, dung lai.', 1;
UPDATE t SET so_tien = a.moi FROM lich_su_thanh_toan t JOIN #tt a ON a.id = t.id;
DELETE FROM lich_su_thanh_toan WHERE id IN (SELECT id FROM #tt WHERE moi = 0);
-- Đơn "tiền mặt + chuyển khoản" nay chỉ còn một khoản thì ghi theo phương thức của khoản đó
UPDATE h SET id_phuong_thuc_thanh_toan = t.id_phuong_thuc_thanh_toan
FROM hoa_don h
JOIN lich_su_thanh_toan t ON t.id_hoa_don = h.id
WHERE h.id IN (SELECT id_hoa_don FROM #tt WHERE moi = 0) AND h.id_phuong_thuc_thanh_toan = @pt_ket_hop AND t.id_phuong_thuc_thanh_toan IS NOT NULL
  AND (SELECT COUNT(*) FROM lich_su_thanh_toan k WHERE k.id_hoa_don = h.id) = 1;

-- Câu mô tả có ghi số tiền cũ của đơn: đổi sang số mới; bỏ phần "(khách đưa ...)" vì không còn đúng
UPDATE t SET mo_ta = LEFT(t.mo_ta, CHARINDEX(N' (kh', t.mo_ta) - 1)
FROM lich_su_thanh_toan t JOIN #hd x ON x.id = t.id_hoa_don
WHERE x.dong_con > 0 AND x.thanh_moi <> x.thanh_cu AND t.mo_ta LIKE N'% (kh_ch %)';
UPDATE l SET mo_ta = REPLACE(l.mo_ta, FORMAT(x.thanh_cu, '#,##0', 'vi-VN') + N' ', FORMAT(x.thanh_moi, '#,##0', 'vi-VN') + N' ')
FROM lich_su_hoa_don l JOIN #hd x ON x.id = l.id_hoa_don
WHERE x.dong_con > 0 AND x.thanh_moi <> x.thanh_cu AND l.mo_ta LIKE N'%' + FORMAT(x.thanh_cu, '#,##0', 'vi-VN') + N' %';

/* ===================== 3. Ca bán hàng đã giao ===================== */
IF @so_sp > 0
BEGIN
    DECLARE @id int, @dau decimal(18, 0), @bg decimal(18, 0), @cl decimal(18, 0);
    DECLARE @bg_truoc_cu decimal(18, 0) = NULL, @bg_truoc_moi decimal(18, 0) = NULL;
    DECLARE @dau_moi decimal(18, 0), @so int, @dt decimal(18, 0), @tm decimal(18, 0), @ck decimal(18, 0), @tte decimal(18, 0), @bg_moi decimal(18, 0);
    DECLARE ca CURSOR LOCAL FAST_FORWARD FOR
        SELECT id, tien_dau_ca, tien_ban_giao, ISNULL(chenh_lech, 0) FROM giao_ca WHERE trang_thai = 2 ORDER BY thoi_gian_nhan_ca, id;
    OPEN ca;
    FETCH NEXT FROM ca INTO @id, @dau, @bg, @cl;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        -- tiền đầu ca trước đây lấy đúng tiền bàn giao của ca trước thì nay lấy theo số mới của ca trước
        SET @dau_moi = CASE WHEN @bg_truoc_cu IS NOT NULL AND @dau = @bg_truoc_cu THEN @bg_truoc_moi ELSE @dau END;
        SELECT @so = COUNT(*), @dt = ISNULL(SUM(h.thanh_tien - ISNULL(h.phi_van_chuyen, 0)), 0)
        FROM hoa_don h WHERE h.id_giao_ca = @id AND h.trang_thai <> 0 AND NOT (h.loai_hoa_don = 1 AND h.trang_thai = 1);
        SELECT @tm = ISNULL(SUM(CASE WHEN ISNULL(t.id_phuong_thuc_thanh_toan, h.id_phuong_thuc_thanh_toan) = @pt_tien_mat THEN t.so_tien ELSE 0 END), 0),
               @ck = ISNULL(SUM(CASE WHEN ISNULL(t.id_phuong_thuc_thanh_toan, h.id_phuong_thuc_thanh_toan) = @pt_tien_mat THEN 0 ELSE t.so_tien END), 0)
        FROM lich_su_thanh_toan t JOIN hoa_don h ON h.id = t.id_hoa_don
        WHERE h.id_giao_ca = @id AND t.trang_thai = 1 AND h.trang_thai <> 0;
        SET @tte = @dau_moi + @tm + @cl;
        IF @tte < 0 SET @tte = 0;
        SET @bg_moi = CASE WHEN @bg IS NULL THEN NULL WHEN @bg > @tte THEN @tte ELSE @bg END;
        UPDATE giao_ca SET tien_dau_ca = @dau_moi, so_hoa_don = @so, doanh_thu = @dt, tien_mat_thu = @tm, chuyen_khoan_thu = @ck,
                           tien_mat_he_thong = @dau_moi + @tm, tien_mat_thuc_te = @tte, chenh_lech = @tte - (@dau_moi + @tm), tien_ban_giao = @bg_moi
        WHERE id = @id;
        SET @bg_truoc_cu = @bg;
        SET @bg_truoc_moi = @bg_moi;
        FETCH NEXT FROM ca INTO @id, @dau, @bg, @cl;
    END
    CLOSE ca;
    DEALLOCATE ca;
END

/* ===================== 4. Sản phẩm, đợt giảm giá, danh mục, thuộc tính ===================== */
-- Đợt giảm giá chỉ gồm hàng bị bỏ thì xóa cả đợt
SELECT d.id INTO #dot_bo FROM dot_giam_gia d
WHERE EXISTS (SELECT 1 FROM dot_giam_gia_chi_tiet c WHERE c.id_dot_giam_gia = d.id)
  AND NOT EXISTS (SELECT 1 FROM dot_giam_gia_chi_tiet c WHERE c.id_dot_giam_gia = d.id AND c.id_san_pham_chi_tiet NOT IN (SELECT id FROM #bt_bo));
-- Trọng lượng / chu vi chỉ biến thể bị bỏ dùng (vd "Không áp dụng")
SELECT a.id INTO #tl_bo FROM trong_luong a
WHERE EXISTS (SELECT 1 FROM san_pham_chi_tiet c WHERE c.id_trong_luong = a.id AND c.id IN (SELECT id FROM #bt_bo))
  AND NOT EXISTS (SELECT 1 FROM san_pham_chi_tiet c WHERE c.id_trong_luong = a.id AND c.id NOT IN (SELECT id FROM #bt_bo));
SELECT a.id INTO #cv_bo FROM chu_vi a
WHERE EXISTS (SELECT 1 FROM san_pham_chi_tiet c WHERE c.id_chu_vi = a.id AND c.id IN (SELECT id FROM #bt_bo))
  AND NOT EXISTS (SELECT 1 FROM san_pham_chi_tiet c WHERE c.id_chu_vi = a.id AND c.id NOT IN (SELECT id FROM #bt_bo));

DELETE FROM dot_giam_gia_chi_tiet WHERE id_san_pham_chi_tiet IN (SELECT id FROM #bt_bo);
DELETE FROM dot_giam_gia WHERE id IN (SELECT id FROM #dot_bo);
DELETE FROM hinh_anh_san_pham WHERE id_san_pham IN (SELECT id FROM #sp_bo);
DELETE FROM san_pham_chi_tiet WHERE id IN (SELECT id FROM #bt_bo);
DELETE FROM san_pham WHERE id IN (SELECT id FROM #sp_bo);
DELETE FROM trong_luong WHERE id IN (SELECT id FROM #tl_bo);
DELETE FROM chu_vi WHERE id IN (SELECT id FROM #cv_bo);
DELETE d FROM danh_muc d WHERE d.ma_danh_muc NOT IN ('DM01', 'DM02') AND NOT EXISTS (SELECT 1 FROM san_pham s WHERE s.id_danh_muc = d.id);
-- Chất liệu không thuộc nhóm vợt và không còn sản phẩm nào dùng
DELETE c FROM chat_lieu c WHERE ISNULL(c.nhom_ap_dung, '') NOT LIKE '%VOT%' AND NOT EXISTS (SELECT 1 FROM san_pham s WHERE s.id_chat_lieu = c.id);

-- Phiếu giảm giá đặt tên theo phụ kiện / quả cầu: phiếu chưa ai dùng thì xóa, phiếu đã dùng thì đặt lại tên theo đúng điều kiện của phiếu
DELETE p FROM phieu_giam_gia p
WHERE p.ma_phieu = 'VCH8DK3SO' AND NOT EXISTS (SELECT 1 FROM hoa_don h WHERE h.id_phieu_giam_gia = p.id)
  AND NOT EXISTS (SELECT 1 FROM phieu_giam_gia_khach_hang k WHERE k.id_phieu_giam_gia = p.id);
UPDATE phieu_giam_gia SET ten_phieu = N'Giảm 20% đơn từ 200K' WHERE ma_phieu = 'VCHPK20QC' AND loai_giam = 1 AND gia_tri_giam = 20 AND gia_tri_don_hang_toi_thieu = 200000;

-- Kiểm tra trước khi lưu: tiền từng hóa đơn và từng ca phải khớp
IF EXISTS (SELECT 1 FROM hoa_don h WHERE h.tong_tien <> (SELECT ISNULL(SUM(c.thanh_tien), 0) FROM hoa_don_chi_tiet c WHERE c.id_hoa_don = h.id))
    THROW 50002, 'Tong tien hoa don khong khop cac dong, dung lai.', 1;
IF EXISTS (SELECT 1 FROM hoa_don WHERE thanh_tien <> tong_tien - ISNULL(so_tien_giam, 0) + ISNULL(phi_van_chuyen, 0) OR thanh_tien < 0)
    THROW 50003, 'Tong thanh toan hoa don khong khop, dung lai.', 1;
IF EXISTS (SELECT 1 FROM giao_ca WHERE trang_thai = 2 AND (tien_mat_he_thong <> tien_dau_ca + tien_mat_thu OR tien_ban_giao > tien_mat_thuc_te))
    THROW 50004, 'Tien ca ban hang khong khop, dung lai.', 1;

COMMIT;
PRINT CONCAT(N'Da bo ', @so_sp, N' san pham khong phai vot; xoa ', @so_hd_xoa, N' hoa don, tinh lai ', @so_hd_sua, N' hoa don.');

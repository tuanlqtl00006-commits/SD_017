-- Gọn dữ liệu nhân viên (10/11): cửa hàng chỉ còn 4 tài khoản NV01 (Quản lý), NV03, NV04, NV05 (Nhân viên);
-- lịch làm việc mỗi ca trong ngày chỉ còn MỘT nhân viên (người đó nhận ca, bán, giữ két, giao ca).
-- Chỉ sửa DỮ LIỆU, không đổi cấu trúc bảng. Chạy sau các file SD_17_*.sql khác. Chạy lại lần 2 không làm gì thêm.
--   NV02 -> NV01, NV06 -> NV04, NV08 -> NV03, NV09 -> NV05 (hóa đơn, ca, lịch, lịch sử của người bị gộp chuyển sang người nhận);
--   NV07, NV10 (đang ngừng hoạt động, không có hóa đơn / ca / lịch) xóa hẳn.
SET QUOTED_IDENTIFIER ON; -- bảng có filtered index (giao ca)
SET ANSI_NULLS ON;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRAN;

DECLARE @map TABLE (ma_cu varchar(20), ma_moi varchar(20));
INSERT INTO @map VALUES ('NV02', 'NV01'), ('NV06', 'NV04'), ('NV08', 'NV03'), ('NV09', 'NV05'), ('NV07', 'NV03'), ('NV10', 'NV03');

IF OBJECT_ID('tempdb..#doi') IS NOT NULL DROP TABLE #doi;
SELECT cu.id AS id_cu, moi.id AS id_moi, cu.ho_ten AS ten_cu, moi.ho_ten AS ten_moi
INTO #doi
FROM @map m
JOIN nhan_vien cu ON cu.ma_nhan_vien = m.ma_cu
JOIN nhan_vien moi ON moi.ma_nhan_vien = m.ma_moi;

-- 1. Lịch làm việc: mỗi (ngày, ca) giữ một dòng, ưu tiên dòng đã có người nhận ca theo lịch đó.
--    Ca đã nhận theo dòng lịch bị bỏ thì thành ca ngoài lịch (không còn tính phút đi muộn).
IF OBJECT_ID('tempdb..#lich_bo') IS NOT NULL DROP TABLE #lich_bo;
;WITH x AS (
    SELECT l.id,
           ROW_NUMBER() OVER (PARTITION BY l.ngay_lam, l.id_ca_lam_viec
                              ORDER BY CASE WHEN EXISTS (SELECT 1 FROM giao_ca g WHERE g.id_lich_lam_viec = l.id) THEN 0 ELSE 1 END, l.id) AS rn
    FROM lich_lam_viec l
)
SELECT id INTO #lich_bo FROM x WHERE rn > 1;
UPDATE giao_ca SET id_lich_lam_viec = NULL, so_phut_di_muon = NULL WHERE id_lich_lam_viec IN (SELECT id FROM #lich_bo);
DELETE FROM lich_lam_viec WHERE id IN (SELECT id FROM #lich_bo);
DECLARE @so_lich_bo int = (SELECT COUNT(*) FROM #lich_bo);

-- 2. Chuyển mọi thứ của người bị gộp sang người nhận
UPDATE l SET id_nhan_vien = d.id_moi FROM lich_lam_viec l JOIN #doi d ON d.id_cu = l.id_nhan_vien;
UPDATE h SET id_nhan_vien = d.id_moi FROM hoa_don h JOIN #doi d ON d.id_cu = h.id_nhan_vien;
UPDATE g SET id_nhan_vien = d.id_moi FROM giao_ca g JOIN #doi d ON d.id_cu = g.id_nhan_vien;
UPDATE g SET id_nhan_vien_nhan_ban_giao = d.id_moi FROM giao_ca g JOIN #doi d ON d.id_cu = g.id_nhan_vien_nhan_ban_giao;
-- hai người gộp thành một thì "bàn giao cho chính mình" đổi thành không chỉ định người nhận
UPDATE giao_ca SET id_nhan_vien_nhan_ban_giao = NULL WHERE id_nhan_vien_nhan_ban_giao = id_nhan_vien;

-- Tên người thao tác lưu dạng chữ trong lịch sử
UPDATE l SET nguoi_thao_tac = d.ten_moi FROM lich_su_hoa_don l JOIN #doi d ON d.ten_cu = l.nguoi_thao_tac;
UPDATE t SET nguoi_nhan = d.ten_moi FROM lich_su_thanh_toan t JOIN #doi d ON d.ten_cu = t.nguoi_nhan;

-- Chat nội bộ / hỗ trợ
UPDATE h SET id_nhan_vien_phu_trach = d.id_moi FROM hoi_thoai h JOIN #doi d ON d.id_cu = h.id_nhan_vien_phu_trach;
UPDATE t SET ten_nguoi_gui = d.ten_moi FROM tin_nhan t JOIN #doi d ON d.id_cu = t.id_nhan_vien AND t.ten_nguoi_gui = d.ten_cu;
UPDATE t SET id_nhan_vien = d.id_moi FROM tin_nhan t JOIN #doi d ON d.id_cu = t.id_nhan_vien;
DELETE t FROM thanh_vien_hoi_thoai t JOIN #doi d ON d.id_cu = t.id_nhan_vien
WHERE EXISTS (SELECT 1 FROM thanh_vien_hoi_thoai k WHERE k.id_hoi_thoai = t.id_hoi_thoai AND k.id_nhan_vien = d.id_moi);
;WITH x AS (
    SELECT t.id, ROW_NUMBER() OVER (PARTITION BY t.id_hoi_thoai, d.id_moi ORDER BY t.id) AS rn
    FROM thanh_vien_hoi_thoai t JOIN #doi d ON d.id_cu = t.id_nhan_vien
)
DELETE FROM thanh_vien_hoi_thoai WHERE id IN (SELECT id FROM x WHERE rn > 1);
UPDATE t SET id_nhan_vien = d.id_moi FROM thanh_vien_hoi_thoai t JOIN #doi d ON d.id_cu = t.id_nhan_vien;

-- 3. Xóa tài khoản đã gộp
DELETE FROM dia_chi_nhan_vien WHERE id_nhan_vien IN (SELECT id_cu FROM #doi);
DELETE FROM nhan_vien_vi_tri WHERE id_nhan_vien IN (SELECT id_cu FROM #doi);
DELETE FROM nhan_vien WHERE id IN (SELECT id_cu FROM #doi);
DECLARE @so_nv_xoa int = @@ROWCOUNT;

-- 4. Vai trò "Thu ngân" không còn dùng (mỗi ca một nhân viên tự giữ két): ngưng nếu không ai đang mang vai trò này
UPDATE v SET trang_thai = 0 FROM vai_tro v
WHERE v.ten_vai_tro LIKE N'Thu ng_n' AND v.trang_thai = 1 AND NOT EXISTS (SELECT 1 FROM nhan_vien n WHERE n.id_vai_tro = v.id);

COMMIT;
PRINT CONCAT(N'Da gop / xoa ', @so_nv_xoa, N' nhan vien, bo ', @so_lich_bo, N' dong lich trung ca.');
SELECT n.ma_nhan_vien, n.ho_ten, n.trang_thai,
       (SELECT COUNT(*) FROM hoa_don h WHERE h.id_nhan_vien = n.id) AS so_hoa_don,
       (SELECT COUNT(*) FROM giao_ca g WHERE g.id_nhan_vien = n.id) AS so_ca,
       (SELECT COUNT(*) FROM lich_lam_viec l WHERE l.id_nhan_vien = n.id) AS so_lich
FROM nhan_vien n ORDER BY n.id;

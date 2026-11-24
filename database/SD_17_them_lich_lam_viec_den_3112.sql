-- Thêm lịch làm việc từ 24/11/2026 tới 31/12/2026 (24/11: lịch cũ chỉ tới 21/11 nên nhân viên không nhận ca được).
-- Theo luồng chốt 10/11: mỗi ca trong ngày chỉ MỘT nhân viên; quản lý (NV01) không nhận ca nên không xếp.
-- Mỗi ngày 3 ca (Ca sáng / chiều / tối đang bật), xoay vòng NV03, NV04, NV05 - mỗi người một ca mỗi ngày.
-- Chạy lại nhiều lần không bị trùng: ngày + ca nào đã có người thì bỏ qua. Chỉ thêm dữ liệu, không đổi cấu trúc bảng.
USE shop_cau_long_footstyle;
GO
SET NOCOUNT ON;

DECLARE @tu date = '2026-11-24', @den date = '2026-12-31';

DECLARE @nv TABLE (thu_tu int, id int);
INSERT INTO @nv (thu_tu, id)
SELECT ROW_NUMBER() OVER (ORDER BY ma_nhan_vien) - 1, id
FROM nhan_vien
WHERE ma_nhan_vien IN ('NV03', 'NV04', 'NV05') AND trang_thai = 1;

DECLARE @ca TABLE (thu_tu int, id int);
INSERT INTO @ca (thu_tu, id)
SELECT ROW_NUMBER() OVER (ORDER BY gio_bat_dau) - 1, id
FROM ca_lam_viec
WHERE trang_thai = 1;

DECLARE @soNv int = (SELECT COUNT(*) FROM @nv);
IF @soNv = 0
BEGIN
    PRINT N'Không có nhân viên NV03 / NV04 / NV05 đang làm - không thêm lịch.';
    RETURN;
END;

DECLARE @them int = 0;
DECLARE @ngay date = @tu, @i int = 0;
WHILE @ngay <= @den
BEGIN
    INSERT INTO lich_lam_viec (id_nhan_vien, id_ca_lam_viec, ngay_lam, trang_thai)
    SELECT nv.id, ca.id, @ngay, 1
    FROM @ca ca
    JOIN @nv nv ON nv.thu_tu = (@i + ca.thu_tu) % @soNv
    WHERE NOT EXISTS (SELECT 1 FROM lich_lam_viec l WHERE l.ngay_lam = @ngay AND l.id_ca_lam_viec = ca.id)
      AND NOT EXISTS (SELECT 1 FROM lich_lam_viec l WHERE l.ngay_lam = @ngay AND l.id_nhan_vien = nv.id AND l.id_ca_lam_viec = ca.id);
    SET @them += @@ROWCOUNT;
    SET @ngay = DATEADD(day, 1, @ngay);
    SET @i += 1;
END;

PRINT N'Đã thêm ' + CAST(@them AS nvarchar(10)) + N' dòng lịch làm việc.';
GO

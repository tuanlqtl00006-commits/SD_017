-- Thêm 6 đợt giảm giá cho vợt cầu lông (10/11), tháng 11 - 12/2026. Chỉ thêm DỮ LIỆU, không đổi cấu trúc bảng.
-- Chạy sau SD_17_chi_ban_vot.sql. Chạy lại lần 2 không thêm trùng (đợt đã có tên đó thì bỏ qua).
-- Mỗi đợt áp cho các biến thể đang bán của một thương hiệu / danh mục; biến thể đang nằm trong một đợt khác đang bật
-- mà trùng thời gian thì không đưa vào (tránh hai đợt chồng nhau trên cùng một biến thể).
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRAN;

DECLARE @dot TABLE (thu_tu int IDENTITY(1, 1), ten nvarchar(255), phan_tram int, bat_dau datetime, ket_thuc datetime,
                    thuong_hieu varchar(50), ma_danh_muc varchar(20), mo_ta nvarchar(500));
INSERT INTO @dot (ten, phan_tram, bat_dau, ket_thuc, thuong_hieu, ma_danh_muc, mo_ta) VALUES
    (N'Tuần lễ vợt Mizuno',              10, '2026-11-10T00:00:00', '2026-11-24T23:59:00', 'Mizuno', 'DM01', N'Vợt Mizuno giảm 10%'),
    (N'Ưu đãi vợt Kumpoo tháng 11',      12, '2026-11-10T00:00:00', '2026-11-30T23:59:00', 'Kumpoo', NULL,   N'Vợt Kumpoo giảm 12%'),
    (N'Vợt cho người mới - giảm 8%',      8, '2026-11-12T00:00:00', '2026-12-12T23:59:00', 'Yonex',  'DM02', N'Vợt Yonex cho người mới giảm 8%'),
    (N'Sale giữa tháng 11 vợt Victor',   15, '2026-11-13T00:00:00', '2026-11-27T23:59:00', 'Victor', 'DM01', N'Vợt Victor giảm 15%'),
    (N'Chào tháng 12 vợt Yonex',         10, '2026-12-06T00:00:00', '2026-12-20T23:59:00', 'Yonex',  'DM01', N'Vợt Yonex giảm 10%'),
    (N'Cuối năm vợt Lining',             12, '2026-12-10T00:00:00', '2026-12-31T23:59:00', 'Lining', 'DM01', N'Vợt Lining giảm 12%');

DECLARE @i int = 1, @n int = (SELECT COUNT(*) FROM @dot), @da_them int = 0;
DECLARE @ten nvarchar(255), @pt int, @bd datetime, @kt datetime, @th varchar(50), @dm varchar(20), @mo_ta nvarchar(500);
DECLARE @so int, @ma varchar(20), @id int;
WHILE @i <= @n
BEGIN
    SELECT @ten = ten, @pt = phan_tram, @bd = bat_dau, @kt = ket_thuc, @th = thuong_hieu, @dm = ma_danh_muc, @mo_ta = mo_ta FROM @dot WHERE thu_tu = @i;
    SET @i += 1;
    IF EXISTS (SELECT 1 FROM dot_giam_gia WHERE ten_dot = @ten) CONTINUE;

    -- Biến thể đang bán của thương hiệu / danh mục, chưa nằm trong đợt đang bật nào trùng thời gian
    IF OBJECT_ID('tempdb..#bt') IS NOT NULL DROP TABLE #bt;
    SELECT c.id INTO #bt
    FROM san_pham_chi_tiet c
    JOIN san_pham s ON s.id = c.id_san_pham
    JOIN thuong_hieu t ON t.id = s.id_thuong_hieu
    JOIN danh_muc d ON d.id = s.id_danh_muc
    WHERE c.trang_thai = 1 AND s.trang_thai = 1 AND t.ten_thuong_hieu = @th AND (@dm IS NULL OR d.ma_danh_muc = @dm)
      AND NOT EXISTS (SELECT 1 FROM dot_giam_gia_chi_tiet x JOIN dot_giam_gia k ON k.id = x.id_dot_giam_gia
                      WHERE x.id_san_pham_chi_tiet = c.id AND x.trang_thai = 1 AND k.trang_thai = 1
                        AND k.ngay_bat_dau <= @kt AND k.ngay_ket_thuc >= @bd);
    IF NOT EXISTS (SELECT 1 FROM #bt) CONTINUE;

    -- Mã nối tiếp số lớn nhất đang có: DGG13, DGG14...
    SET @so = (SELECT ISNULL(MAX(TRY_CAST(SUBSTRING(ma_dot, 4, 20) AS int)), 0) FROM dot_giam_gia WHERE ma_dot LIKE 'DGG%') + 1;
    SET @ma = 'DGG' + RIGHT('0' + CAST(@so AS varchar(10)), CASE WHEN @so < 100 THEN 2 ELSE LEN(@so) END);
    INSERT INTO dot_giam_gia (ma_dot, ten_dot, phan_tram_giam_dot, ngay_bat_dau, ngay_ket_thuc, mo_ta, ngay_tao, trang_thai)
    VALUES (@ma, @ten, @pt, @bd, @kt, @mo_ta, GETDATE(), 1);
    SET @id = SCOPE_IDENTITY();
    INSERT INTO dot_giam_gia_chi_tiet (id_san_pham_chi_tiet, id_dot_giam_gia, phan_tram_giam_bien_the, trang_thai)
    SELECT id, @id, @pt, 1 FROM #bt;
    SET @da_them += 1;
END

COMMIT;
PRINT CONCAT(N'Da them ', @da_them, N' dot giam gia.');
SELECT d.ma_dot, d.ten_dot, d.phan_tram_giam_dot AS phan_tram, CONVERT(varchar(10), d.ngay_bat_dau, 23) AS bat_dau, CONVERT(varchar(10), d.ngay_ket_thuc, 23) AS ket_thuc,
       (SELECT COUNT(*) FROM dot_giam_gia_chi_tiet c WHERE c.id_dot_giam_gia = d.id) AS so_bien_the
FROM dot_giam_gia d ORDER BY d.ngay_bat_dau;

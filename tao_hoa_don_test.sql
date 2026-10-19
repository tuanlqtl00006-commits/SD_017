-- Script tạo 1 hóa đơn mới nguyên (trạng thái: Chờ xác nhận) để test luồng nút bấm
USE shop_cau_long_footstyle;
GO

SET NOCOUNT ON;

DECLARE @NewId INT;
DECLARE @MaHD VARCHAR(20);
DECLARE @IdSPCT INT;
DECLARE @GiaBan DECIMAL(18, 2);
DECLARE @NextNum INT;

-- 1. Tìm biến thể sản phẩm có sẵn trong database
SELECT TOP 1 @IdSPCT = id, @GiaBan = ISNULL(gia_ban, 1000000)
FROM san_pham_chi_tiet
WHERE trang_thai = 1;

IF @IdSPCT IS NULL
    SELECT TOP 1 @IdSPCT = id, @GiaBan = ISNULL(gia_ban, 1000000) FROM san_pham_chi_tiet;

IF @GiaBan IS NULL OR @GiaBan <= 0
    SET @GiaBan = 1000000;

-- 2. Sinh mã hóa đơn tiếp theo (HD18, HD19...)
SELECT @NextNum = ISNULL(MAX(id), 0) + 1 FROM hoa_don;
SET @MaHD = 'HD' + CAST(@NextNum AS VARCHAR(10));

WHILE EXISTS (SELECT 1 FROM hoa_don WHERE ma_hoa_don = @MaHD)
BEGIN
    SET @NextNum = @NextNum + 1;
    SET @MaHD = 'HD' + CAST(@NextNum AS VARCHAR(10));
END

-- 3. Tạo hóa đơn mới (loai_hoa_don = 2: Giao hàng, trang_thai = 1: Chờ xác nhận)
INSERT INTO hoa_don (
    ma_hoa_don,
    loai_hoa_don,
    trang_thai,
    tong_tien,
    so_tien_giam,
    phi_van_chuyen,
    thanh_tien,
    don_vi_van_chuyen,
    ho_ten_nguoi_nhan,
    sdt_nguoi_nhan,
    dia_chi_giao_hang,
    ghi_chu,
    ngay_tao,
    ngay_cap_nhat
)
VALUES (
    @MaHD,
    2, -- Đơn giao hàng (đi đủ 6 bước)
    1, -- 1 = Chờ xác nhận (Mới nguyên, có đủ nút bấm "Xác nhận đơn", "Hủy đơn")
    @GiaBan,
    0,
    30000,
    @GiaBan + 30000,
    N'GHN',
    N'Nguyễn Văn Test',
    '0987654321',
    N'Số 1 Trịnh Văn Bô, Nam Từ Liêm, Hà Nội',
    N'Đơn hàng mới tạo để kiểm tra các nút chuyển trạng thái',
    GETDATE(),
    GETDATE()
);

SET @NewId = SCOPE_IDENTITY();

-- 4. Thêm sản phẩm vào hóa đơn chi tiết
IF @IdSPCT IS NOT NULL
BEGIN
    INSERT INTO hoa_don_chi_tiet (
        id_hoa_don,
        id_san_pham_chi_tiet,
        so_luong,
        don_gia,
        thanh_tien,
        trang_thai
    )
    VALUES (
        @NewId,
        @IdSPCT,
        1,
        @GiaBan,
        @GiaBan,
        1
    );
END

-- 5. Thêm lịch sử tạo đơn ban đầu
INSERT INTO lich_su_hoa_don (
    id_hoa_don,
    nguoi_thao_tac,
    hanh_dong,
    thoi_gian,
    mo_ta
)
VALUES (
    @NewId,
    N'Khách hàng',
    N'Tạo đơn hàng',
    GETDATE(),
    N'Khách đặt hàng trực tuyến thành công'
);

-- In thông báo và kết quả
PRINT N'=====================================================';
PRINT N'Tạo thành công hóa đơn mới nguyên: ' + @MaHD + N' (ID: ' + CAST(@NewId AS VARCHAR(10)) + N')';
PRINT N'Trạng thái: 1 - Chờ xác nhận';
PRINT N'=====================================================';

SELECT id, ma_hoa_don, loai_hoa_don, trang_thai, tong_tien, phi_van_chuyen, thanh_tien, ngay_tao
FROM hoa_don
WHERE id = @NewId;

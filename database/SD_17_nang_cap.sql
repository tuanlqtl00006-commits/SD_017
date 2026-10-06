\xef\xbb\xbf-- =================================================================
-- NÂNG CẤP CSDL ĐÃ CHẠY TRƯỚC ĐÓ (không cần chạy nếu vừa tạo mới bằng SD_17.sql bản mới)
-- Có thể chạy lại nhiều lần. Thêm các chỉ mục chống trùng ở mức CSDL.
-- Nếu báo lỗi "duplicate key" nghĩa là đang có nhân viên trùng email / SĐT: sửa dữ liệu trùng rồi chạy lại.
-- =================================================================

USE shop_cau_long_footstyle;
GO

SET QUOTED_IDENTIFIER ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_email' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_email ON nhan_vien (email) WHERE email IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_nhan_vien_sdt' AND object_id = OBJECT_ID('nhan_vien'))
    CREATE UNIQUE INDEX ux_nhan_vien_sdt ON nhan_vien (sdt) WHERE sdt IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_spct_san_pham_mau_tl_cv' AND object_id = OBJECT_ID('san_pham_chi_tiet'))
    CREATE UNIQUE INDEX ux_spct_san_pham_mau_tl_cv ON san_pham_chi_tiet (id_san_pham, id_mau_sac, id_trong_luong, id_chu_vi);
GO

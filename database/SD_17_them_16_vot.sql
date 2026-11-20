-- 16 vợt mới (20/11/2026): Yonex, Lining, Victor, Mizuno, Kumpoo. Chỉ INSERT, không đổi cấu trúc bảng.
-- Ảnh thật tải từ shopvnb.com, nằm trong frontend/public/images/san-pham/ (sp34_... đến sp49_...). Giá theo giá niêm yết shopvnb.
-- Đã có SP49 thì bỏ qua cả file (chạy lại không bị trùng). Chạy: sqlcmd -I -f 65001 -d shop_cau_long_footstyle -i SD_17_them_16_vot.sql
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF EXISTS (SELECT 1 FROM san_pham WHERE ma_san_pham = 'SP49')
BEGIN
    PRINT N'Đã có 16 vợt mới, bỏ qua.';
    RETURN;
END
BEGIN TRAN;
DECLARE @id int;

-- SP34 Vợt cầu lông Yonex Astrox 99 Tour 2025
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM01'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH01'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX01'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL01'), (SELECT id FROM do_cung WHERE ma_do_cung='DC03'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB01'), 'SP34', N'Vợt cầu lông Yonex Astrox 99 Tour 2025', N'Nặng đầu, đũa cứng, hợp lối đánh tấn công đập cầu mạnh.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp34_yonex_astrox_99_tour.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp34_yonex_astrox_99_tour.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS07'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS07'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL02'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP34-XLA-3U-G5', 4349000, 6, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS07'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP34-XLA-4U-G5', 4349000, 9, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP35 Vợt cầu lông Yonex Astrox 100 Tour VA
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM01'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH01'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX01'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL01'), (SELECT id FROM do_cung WHERE ma_do_cung='DC03'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB01'), 'SP35', N'Vợt cầu lông Yonex Astrox 100 Tour VA', N'Nặng đầu, đũa cứng, phù hợp người chơi tấn công đã có lực cổ tay tốt.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp35_yonex_astrox_100_tour_va.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp35_yonex_astrox_100_tour_va.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS06'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS06'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL02'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP35-XAM-3U-G5', 4279000, 12, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS06'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP35-XAM-4U-G5', 4279000, 8, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP36 Vợt cầu lông Yonex Astrox 99 Play 2025
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM02'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH01'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX01'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL02'), (SELECT id FROM do_cung WHERE ma_do_cung='DC02'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB01'), 'SP36', N'Vợt cầu lông Yonex Astrox 99 Play 2025', N'Phiên bản dễ đánh của dòng Astrox 99, nặng đầu vừa phải, đũa trung bình.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp36_yonex_astrox_99_play.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp36_yonex_astrox_99_play.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS07'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS07'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP36-XLA-4U-G5', 1769000, 7, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS07'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL04'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP36-XLA-5U-G5', 1769000, 14, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP37 Vợt cầu lông Yonex Astrox 01C 2024
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM02'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH01'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX01'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL02'), (SELECT id FROM do_cung WHERE ma_do_cung='DC01'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB01'), 'SP37', N'Vợt cầu lông Yonex Astrox 01C 2024', N'Khung nhẹ, đũa dẻo, dễ trợ lực cho người mới tập.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp37_yonex_astrox_01c.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp37_yonex_astrox_01c.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS02'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS02'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP37-TRG-4U-G5', 1009000, 10, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS02'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL04'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP37-TRG-5U-G5', 1009000, 5, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP38 Vợt cầu lông Lining Bladex 800 Power 2026
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM01'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH02'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX02'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL01'), (SELECT id FROM do_cung WHERE ma_do_cung='DC03'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB01'), 'SP38', N'Vợt cầu lông Lining Bladex 800 Power 2026', N'Thiên công, nặng đầu, đũa cứng cho cú đập uy lực.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp38_lining_bladex_800_power.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp38_lining_bladex_800_power.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL02'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP38-DEN-3U-G5', 4490000, 9, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP38-DEN-4U-G5', 4490000, 11, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP39 Vợt cầu lông Lining Halbertec Motor Pro
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM01'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH02'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX02'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL01'), (SELECT id FROM do_cung WHERE ma_do_cung='DC02'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB02'), 'SP39', N'Vợt cầu lông Lining Halbertec Motor Pro', N'Cân bằng, đũa trung bình, đánh công thủ toàn diện.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp39_lining_halbertec_motor_pro.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp39_lining_halbertec_motor_pro.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL02'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP39-DEN-3U-G5', 2100000, 15, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP39-DEN-4U-G5', 2100000, 6, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP40 Vợt cầu lông Lining Axforce 30
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM02'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH02'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX02'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL02'), (SELECT id FROM do_cung WHERE ma_do_cung='DC02'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB01'), 'SP40', N'Vợt cầu lông Lining Axforce 30', N'Hơi nặng đầu, đũa trung bình, dễ làm quen với lối đánh tấn công.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp40_lining_axforce_30.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp40_lining_axforce_30.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS03'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS03'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP40-DO-4U-G5', 1350000, 8, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS03'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL04'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP40-DO-5U-G5', 1350000, 13, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP41 Vợt cầu lông Victor Thruster Ryuga II Pro
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM01'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH03'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX03'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL01'), (SELECT id FROM do_cung WHERE ma_do_cung='DC04'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB01'), 'SP41', N'Vợt cầu lông Victor Thruster Ryuga II Pro', N'Nặng đầu, đũa rất cứng, dành cho người chơi tấn công trình độ cao.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp41_victor_ryuga_ii_pro.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp41_victor_ryuga_ii_pro.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL02'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP41-DEN-3U-G5', 4350000, 5, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP41-DEN-4U-G5', 4350000, 10, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP42 Vợt cầu lông Victor Auraspeed 99 J 2026
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM01'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH03'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX03'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL01'), (SELECT id FROM do_cung WHERE ma_do_cung='DC03'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB02'), 'SP42', N'Vợt cầu lông Victor Auraspeed 99 J 2026', N'Cân bằng, thiên tốc độ, phản tạt và chơi đôi nhanh.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp42_victor_auraspeed_99_j.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp42_victor_auraspeed_99_j.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL02'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP42-XDG-3U-G5', 4190000, 6, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP42-XDG-4U-G5', 4190000, 9, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP43 Vợt cầu lông Victor Auraspeed A
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM02'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH03'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX03'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL02'), (SELECT id FROM do_cung WHERE ma_do_cung='DC02'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB03'), 'SP43', N'Vợt cầu lông Victor Auraspeed A', N'Nhẹ đầu, xoay vợt nhanh, hợp người mới chơi phong trào.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp43_victor_auraspeed_a.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp43_victor_auraspeed_a.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP43-XDG-4U-G5', 1190000, 12, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL04'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP43-XDG-5U-G5', 1190000, 8, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP44 Vợt cầu lông Mizuno JPX 10.5 CT
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM01'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH04'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX01'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL01'), (SELECT id FROM do_cung WHERE ma_do_cung='DC03'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB01'), 'SP44', N'Vợt cầu lông Mizuno JPX 10.5 CT', N'Nặng đầu, đũa cứng, đánh cầu sâu và chắc tay.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp44_mizuno_jpx_105_ct.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp44_mizuno_jpx_105_ct.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL02'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP44-XDG-3U-G5', 2800000, 7, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP44-XDG-4U-G5', 2800000, 14, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP45 Vợt cầu lông Mizuno Acroforce 600
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM01'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH04'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX01'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL01'), (SELECT id FROM do_cung WHERE ma_do_cung='DC03'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB02'), 'SP45', N'Vợt cầu lông Mizuno Acroforce 600', N'Cân bằng, đũa cứng, kiểm soát cầu tốt khi đánh nhanh.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp45_mizuno_acroforce_600.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp45_mizuno_acroforce_600.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL02'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP45-DEN-3U-G5', 3700000, 10, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS01'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP45-DEN-4U-G5', 3700000, 5, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP46 Vợt cầu lông Mizuno Carbo Pro 837
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM02'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH04'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX01'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL02'), (SELECT id FROM do_cung WHERE ma_do_cung='DC01'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB02'), 'SP46', N'Vợt cầu lông Mizuno Carbo Pro 837', N'Cân bằng, đũa dẻo, dễ đánh cho người mới.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp46_mizuno_carbo_pro_837.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp46_mizuno_carbo_pro_837.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS03'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS03'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP46-DO-4U-G5', 1040000, 9, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS03'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL04'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP46-DO-5U-G5', 1040000, 11, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP47 Vợt cầu lông Kumpoo Dream
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM02'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH05'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX02'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL02'), (SELECT id FROM do_cung WHERE ma_do_cung='DC01'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB02'), 'SP47', N'Vợt cầu lông Kumpoo Dream', N'Khung nhẹ, đũa dẻo, giá mềm cho người mới tập.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp47_kumpoo_dream.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp47_kumpoo_dream.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS02'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS02'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP47-TRG-4U-G5', 938000, 15, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS02'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL04'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP47-TRG-5U-G5', 938000, 6, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP48 Vợt cầu lông Kumpoo Pines II
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM02'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH05'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX02'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL02'), (SELECT id FROM do_cung WHERE ma_do_cung='DC02'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB01'), 'SP48', N'Vợt cầu lông Kumpoo Pines II', N'Hơi nặng đầu, đũa trung bình, tập đập cầu dễ dàng.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp48_kumpoo_pines_ii.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp48_kumpoo_pines_ii.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS07'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS07'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP48-XLA-4U-G5', 950000, 8, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS07'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL04'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP48-XLA-5U-G5', 950000, 13, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

-- SP49 Vợt cầu lông Kumpoo Blue Dome
INSERT INTO san_pham (id_danh_muc, id_thuong_hieu, id_xuat_xu, id_chat_lieu, id_do_cung, id_diem_can_bang, ma_san_pham, ten_san_pham, mo_ta, ngay_tao, ngay_cap_nhat, trang_thai)
VALUES ((SELECT id FROM danh_muc WHERE ma_danh_muc='DM02'), (SELECT id FROM thuong_hieu WHERE ma_thuong_hieu='TH05'), (SELECT id FROM xuat_xu WHERE ma_xuat_xu='XX02'), (SELECT id FROM chat_lieu WHERE ma_chat_lieu='CL02'), (SELECT id FROM do_cung WHERE ma_do_cung='DC01'), (SELECT id FROM diem_can_bang WHERE ma_diem_can_bang='CB03'), 'SP49', N'Vợt cầu lông Kumpoo Blue Dome', N'Nhẹ đầu, đũa dẻo, phản xạ nhanh, hợp chơi phong trào.', '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
SET @id = SCOPE_IDENTITY();
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp49_kumpoo_blue_dome.webp', 1, 1, NULL);
INSERT INTO hinh_anh_san_pham (id_san_pham, duong_dan_hinh_anh, la_anh_chinh, trang_thai, id_mau_sac) VALUES (@id, '/images/san-pham/sp49_kumpoo_blue_dome.webp', 0, 1, (SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'));
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL03'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP49-XDG-4U-G5', 859000, 5, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);
INSERT INTO san_pham_chi_tiet (id_mau_sac, id_trong_luong, id_chu_vi, id_san_pham, ma_spct, gia_ban, so_luong_ton, ngay_tao, ngay_cap_nhat, trang_thai) VALUES ((SELECT id FROM mau_sac WHERE ma_mau_sac='MS04'), (SELECT id FROM trong_luong WHERE ma_trong_luong='TL04'), (SELECT id FROM chu_vi WHERE ma_chu_vi='CV02'), @id, 'SP49-XDG-5U-G5', 859000, 10, '2026-11-20 21:00:00', '2026-11-20 21:00:00', 1);

COMMIT;
PRINT N'Đã thêm 16 vợt.';

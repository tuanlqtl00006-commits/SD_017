# Kế hoạch sửa SD_017 (FootStyle) theo lời cô review GĐ2

Nguồn: bản chép lời cô trong video review (đoạn nhóm SD-17 từ 11:02 đến 15:05, kèm các nhận xét cô nói với nhóm đầu SD-09 và SD-03
mà cô bảo "các nhóm xem lại") và video demo của nhóm (xem cô đang chỉ vào màn hình nào).

Cô kết luận về nhóm mình: **chỉ có Sản phẩm và Hóa đơn "tạm được", các module còn lại (Phiếu giảm giá, Đợt giảm giá, Khách hàng, Nhân viên) chưa được,
chưa thể hiện được "case khó" (validate).**

**Tình trạng: đã làm xong cả 21 ý (cột "Kết quả" bên dưới), đã chạy test toàn bộ (mục 4).**

## 1. Đọc lời cô -> việc phải làm -> kết quả

Tên file ở cột "Kết quả": giao diện nằm trong `frontend/src/`, bài test giao diện trong `frontend/tests/`, bài test backend trong
`backend/src/test/java/com/footstyle/demo/`.

| # | Cô nói (mốc video) | Hiểu là | Việc làm | Kết quả |
|---|---|---|---|---|
| 1 | "Chưa chuyển thành Select2 tất cả các combobox" (12:16, 09:49 SD-03, 16:18) | Mọi ô chọn (combobox) phải là ô chọn có ô tìm kiếm kiểu Select2 | Làm component `Select2.vue`, thay hết `<select>` trong các trang | ✅ `components/common/Select2.vue` (tìm kiếm, bàn phím, xóa lựa chọn) dùng ở 19 trang / form. Không còn `<select>` thường nào, trừ 2 ô Tháng / Năm nằm trong lịch của ô chọn ngày (`DateInput.vue`). Test: `thanhPhanChung.test.js`, `helpersSelect2.js` |
| 2 | "Chưa có thêm nhanh" (12:22, 16:18) | Trong ô Select2 gõ tên chưa có thì "Thêm nhanh" luôn, khỏi sang trang thuộc tính | Select2 có nút "Thêm nhanh"; dùng cho Danh mục, Thương hiệu, Xuất xứ, Chất liệu, Độ cứng, Điểm cân bằng, Màu sắc, Trọng lượng, Chu vi | ✅ `composables/useThemNhanh.js` + `Select2.vue` (nút xuất hiện ngay khi gõ tên chưa có, tạo xong tự chọn luôn). Test: `sanPhamAnhThemNhanh.test.js` |
| 3 | "2 x 2 x 2 = 8 thì dưới này phải có 8 biến thể" (12:30) | Số biến thể sinh ra = tích số màu x trọng lượng x chu vi, hiển thị đúng | Giữ nhóm theo màu (cô đã chấp nhận), thêm tổng số biến thể + test 2x2x2 = 8 | ✅ `utils/bienThe.js` (`taoBienThe`, `congThucSoBienThe`), form hiện "Tổng cộng N biến thể (2 màu × 2 trọng lượng × 2 chu vi)". Test: `bienTheTheoMau.test.js` (có đúng ví dụ 2×2×2 = 8, 3×3×1 = 9, 4×3×2 = 24) |
| 4 | "Ảnh đang chưa có mà tại sao vẫn thành công" (13:18) | Chưa có ảnh thì không được lưu sản phẩm | Bắt buộc ảnh (ảnh chung hoặc đủ ảnh cho từng màu), chặn cả ở backend | ✅ Giao diện: khung ảnh có dấu `*`, lỗi hiện dưới khung ảnh (`SanPhamFormPage.vue`, `utils/bienThe.js#kiemTraAnhBatBuoc`). Backend: không có ảnh nào hoặc thiếu ảnh của một màu thì trả lỗi 400, không lưu. Test: `sanPhamAnhThemNhanh.test.js`, `SanPhamAnhBatBuocTest.java` |
| 5 | "Giảm giá, không thêm cột vào đây, sau này nó sẽ gạch trừ đi, xóa cột này đi" (13:27) | Bảng biến thể bỏ cột Giảm giá, giá hiện gạch ngang + giá sau giảm | Bỏ cột, hiển thị giá gạch ngang trong cột Giá bán | ✅ Bảng ở trang Biến thể (`BienTheManager.vue`) không còn cột Giảm giá; cột Giá bán hiện giá sau giảm kèm giá gốc gạch ngang khi biến thể nằm trong đợt đang chạy (`composables/useGiamGiaBienThe.js`, `utils/giamGia.js`); danh sách Sản phẩm (`SanPhamManager.vue`) cũng hiện giá sau giảm + giá gốc gạch ngang; file Excel xuất có cột "Giá sau giảm (₫)" thay cho cột "Giảm giá (%)". Form thêm / sửa sản phẩm không có cột Giảm giá. Test: `giamGiaBienThe.test.js` |
| 6 | "Phân trang nhiều quá thì chỉ 1 2 ... số cuối thôi, các nhóm khác cũng sửa theo" (13:35) | Phân trang rút gọn có dấu ba chấm | Viết lại `BasePagination.vue`: `1 2 3 … 20`, bỏ nút « » | ✅ `utils/phanTrang.js` + `components/common/BasePagination.vue`, mọi bảng danh sách dùng chung; luôn có trang 1, 2, trang cuối, trang đang xem và hai trang kề nó, còn lại là dấu ba chấm (20 trang: đang ở trang 1 hiện `1 2 … 20`, ở trang 10 hiện `1 2 … 9 10 11 … 20`). Test: `thanhPhanChung.test.js` |
| 7 | "Phần này không cần in ra trong hóa đơn" / "In mã đã dùng, giảm bao nhiêu %, phí vận chuyển, logo đơn vị giao hàng (GHN, GHTK), có QR là OK" (11:49, 11:53) | Bản in hóa đơn: bỏ khối Lịch sử thanh toán; dòng giảm giá ghi mã + %; có phí vận chuyển; có logo đơn vị vận chuyển; giữ QR | Sửa bản in / PDF hóa đơn | ✅ `utils/hoaDonIn.js`: bỏ Lịch sử thanh toán; có dòng "Giảm giá (mã … - …%)", phí vận chuyển, đơn vị vận chuyển (logo hoặc ô chữ, `utils/vanChuyen.js` + `components/common/VanChuyenLogo.vue`), QR. Test: `hoaDonTrangThaiIn.test.js` |
| 8 | "Chuyển sang Đã xác nhận thì tự động in PDF, chưa có" (06:56, nhóm đầu) | Bấm xác nhận đơn là tự mở bản in / lưu PDF | Tự mở hộp thoại in PDF khi đơn sang Đã xác nhận | ✅ `HoaDonChiTiet.vue`: bấm "Đã xác nhận" thì mở cửa sổ in ngay khi chuyển thành công (cửa sổ được mở sẵn trong lúc bấm để trình duyệt không chặn pop-up). Test: `hoaDonChiTietTrangThai.test.js`, `hoaDonTrangThaiIn.test.js` |
| 9 | "Tại quầy sao lại có phí vận chuyển 30 nghìn" (10:43), "Khách lẻ tại sao có số điện thoại" (07:13) | Đơn tại quầy phí ship = 0, không đơn vị vận chuyển; khách lẻ không hiện SĐT | Backend + giao diện | ✅ Backend (`HoaDonService`, `SchemaUpgradeRunner`, `database/SD_17_du_lieu_va_nang_cap.sql` PHẦN 8.1 sửa luôn dữ liệu cũ): đơn tại quầy phí ship = 0, không đơn vị vận chuyển. Giao diện: khách lẻ không hiện SĐT. Test: `HoaDonChiTietHienThiTest.java`, `SchemaUpgradeRunnerHoaDonTest.java`, `hoaDonChiTietTrangThai.test.js` |
| 10 | "Đã hoàn thành rồi sao còn chuyển trạng thái" (10:36) | Đơn Đã hoàn thành / Đã hủy là trạng thái cuối, khóa nút chuyển | Giao diện + backend | ✅ Luật chuyển trạng thái viết một chỗ ở backend `HoaDonLuongTrangThai.java`; API chi tiết trả `chuyenDuoc`, giao diện chỉ hiện đúng các nút đó. Trạng thái cuối (Đã hủy, Đã hoàn thành, Hoàn tiền) không còn nút. Test: `HoaDonLuongTrangThaiTest.java`, `HoaDonDoiTrangThaiTest.java`, `hoaDonChiTietTrangThai.test.js` |
| 11 | "Trường hợp giao hàng thất bại chưa thể hiện" (07:24) | Có nhánh Giao hàng thất bại | Thêm trạng thái "Giao hàng thất bại" (bắt buộc lý do) -> giao lại hoặc hủy + trả tồn kho | ✅ Mã trạng thái 8. Đang giao hàng -> Giao hàng thất bại (bắt buộc lý do) -> Giao lại (về Đang giao hàng) hoặc Hủy đơn (cộng lại tồn kho). Có tab riêng ở danh sách, màu riêng, dòng lý do trên chi tiết, đơn mẫu HD19 trong SQL PHẦN 8.2. Test: `HoaDonGiaoThatBaiTest.java`, `hoaDonChiTietTrangThai.test.js` |
| 12 | Phiếu giảm giá "làm lại, không đúng yêu cầu, phần ngày bắt đầu các thứ cũng không đúng, xem lại nhóm đầu" (13:52) | Làm lại theo mẫu nhóm đầu: Thông tin chung + Đối tượng áp dụng, **ngày GIỜ** bắt đầu / kết thúc, "Trạng thái dự kiến" | Viết lại form phiếu + backend nhận ngày giờ | ✅ `components/phieuGiamGia/PhieuGiamGiaFormModal.vue` (hiện thành trang qua `PhieuGiamGiaFormPage.vue`): thẻ Thông tin chung, thẻ Đối tượng áp dụng (Công khai / Cá nhân), ô ngày giờ (`DateTimeInput.vue`), "Trạng thái dự kiến" đổi ngay khi chọn giờ, lỗi dưới từng ô. Backend nhận `yyyy-MM-ddTHH:mm[:ss]`, tính theo phút (`ThoiGianPhut.java`). Test: `phieuGiamGia.test.js`, `phieuGiamGiaNgayGio.test.js`, `PhieuGiamGiaServiceTest.java` |
| 13 | Số lượng: công khai nhập được, chuyển sang cá nhân thì biến mất / không cho nhập (04:50) | Ô số lượng chỉ có ở phiếu công khai, phiếu cá nhân tự bằng số khách chọn | Ẩn ô số lượng khi chọn Cá nhân, xóa số đã nhập | ✅ Chọn Cá nhân thì ô "Số lượng phát hành" biến mất và số đã gõ bị xóa; số lượng gửi lên = số khách đã chọn. Chuyển lại Công khai thì ô hiện lại, để trống. Test: `phieuGiamGia.test.js`, `phieuCaNhanMail.test.js` |
| 14 | Lọc khách khi tặng phiếu cá nhân: ngày mua / đơn mua, ngày sinh, giới tính (05:09) | Bảng chọn khách có Tổng đơn, Mua gần nhất, Ngày sinh, Giới tính + bộ lọc | Backend trả thêm tổng đơn / mua gần nhất; lọc theo giới tính, tháng sinh / sắp sinh nhật, số đơn, ngày mua | ✅ `components/phieuGiamGia/KhachHangChonBang.vue` + `utils/khachChonPhieu.js`: các cột Tổng đơn, Mua gần nhất, Ngày sinh, Giới tính; bộ lọc giới tính, tháng sinh / sắp sinh nhật, số đơn tối thiểu, khoảng ngày mua gần nhất; có ô tích chọn tất cả khách đang hiển thị. Test: `phieuCaNhanMail.test.js`, `phieuGiamGia.test.js` |
| 15 | Phiếu / đợt chồng nhau: "cộng dồn hay lấy mức cao nhất" (06:13) | Phải thể hiện và demo được quy tắc | Giữ quy tắc lấy mức cao nhất, hiện rõ trong form + hộp cảnh báo khi chọn biến thể đã nằm trong đợt khác | ✅ `GiamGiaChongDot.java`: chế độ chồng giảm giá cấu hình được (`app.giam-gia.che-do-chong-dot`: `MAX` mặc định = lấy mức cao nhất, `CONG_DON` = cộng dồn tối đa 100%, `TRUNG_BINH`); mỗi đơn dùng 1 phiếu; trừ đợt giảm trước, phiếu giảm sau trên số tiền còn lại. Form phiếu có thẻ "Khi có nhiều ưu đãi cùng lúc thì tính thế nào?" kèm ví dụ tính; form đợt có `DotGiamGiaChongDotCard.vue` cảnh báo biến thể đã nằm trong đợt khác. Test: `GiamGiaChongDotTest.java`, `dotGiamGia*.test.js`, `phieuGiamGia.test.js` |
| 16 | Hóa đơn: "giảm giá chưa thể hiện dùng mã nào, giảm bao nhiêu %" (07:13, 10:43) | Chi tiết + bản in ghi rõ mã phiếu và % giảm | Backend trả mã + loại + giá trị phiếu | ✅ API chi tiết hóa đơn trả mã phiếu, tên phiếu, loại giảm, giá trị, mức giảm tối đa; màn chi tiết có dòng "Phiếu giảm giá" (mã + "giảm 10% (tối đa …)" hoặc "giảm 50.000 ₫"), bản in có dòng "Giảm giá (mã … – giảm …%, tối đa …)". Test: `HoaDonChiTietHienThiTest.java`, `hoaDonTrangThaiIn.test.js` |
| 17 | "Đợt giảm giá... tao nói từ bao giờ rồi mà chưa sửa, làm lại" (14:10) | Làm lại theo mẫu nhóm đầu: ngày giờ, trạng thái tự theo thời gian, trạng thái dự kiến, case khó | Viết lại validate + ngày giờ + cảnh báo trùng đợt | ✅ `components/dotGiamGia/DotGiamGiaFormPage.vue` + `utils/ngayGio.js`: ngày giờ bắt đầu / kết thúc, trạng thái dự kiến, thẻ Tổng kết đợt, lịch biến thể (`DotGiamGiaLichCard.vue`), cảnh báo trùng đợt; sửa đợt đã bắt đầu thì không đổi giờ bắt đầu; tạo mới không bắt đầu trong quá khứ. Backend `DotGiamGiaController` + `ThoiGianPhut.java` kiểm lại. Test: `dotGiamGia.test.js`, `dotGiamGiaNgayGio.test.js`, `dotGiamGiaChiTiet.test.js`, `dotGiamGiaTongKet.test.js`, `DotGiamGiaControllerTest.java`, `LichGiamGiaServiceTest.java` |
| 18 | Khách hàng: "chưa thể hiện được valis, các case khó" (14:30, 14:42) | Phải bắt lỗi và HIỆN thông báo lỗi: SĐT đúng đầu số, trùng email / SĐT, tuổi | Validate đủ ở form + backend, hiện lỗi dưới từng ô | ✅ `components/khachHang/KhachHangFormPage.vue` + `utils/kiemTra.js`: họ tên, SĐT (10 số, đầu 03/05/07/08/09), email, ngày sinh; báo trùng SĐT / email ngay khi gõ; mọi lỗi hiện dưới từng ô. Backend `KhachHangService` + `QuyTacDuLieu.java` kiểm lại và trả lỗi theo từng trường. Test: `khachHangForm.test.js`, `kiemTra.test.js`, `KhachHangServiceTest.java`, `QuyTacDuLieuTest.java` |
| 19 | "Tuổi từ 15 mới được mua hàng thì phải có căn cứ đàng hoàng" (01:03) | Mọi con số tuổi phải có lý do nêu được với hội đồng | Khách >= 15 tuổi (BLDS 2015 Đ.21), nhân viên >= 18 (BLLĐ 2019 Đ.143), ghi rõ trên form và trong tài liệu | ✅ Căn cứ ghi dưới ô Ngày sinh của form khách / nhân viên, trong chú thích `QuyTacDuLieu.java` + `utils/kiemTra.js`, và đầy đủ trong `CAN_CU_PHAP_LY_VA_QUY_TAC.md` (đã đối chiếu với nội dung điều luật; nói rõ mốc 15 / 18 là quy định nội bộ dựa trên các điều luật đó, tuổi tối đa 100 chỉ để chặn gõ nhầm năm sinh). Test: `kiemTra.test.js`, `QuyTacDuLieuTest.java` |
| 20 | Nhân viên: "chỉ có hai quyền", "import excel hàng loạt, một dòng sai thì cả file không đẩy, tự gửi mail" (02:17, 02:45) | Nhân viên có Import Excel, kiểm cả file, 1 lỗi là từ chối cả file; sau khi nhập tự gửi mail tài khoản | Làm Import Excel (mẫu .xlsx, báo lỗi từng dòng), endpoint nhập một lần (tất cả hoặc không gì cả), gửi mail tài khoản | ✅ Chỉ còn 2 quyền (Quản lý, Nhân viên). `NhanVienImportService.java` + `components/nhanVien/NhanVienImportModal.vue`: tải file mẫu .xlsx, kiểm tra cả file (tối đa 100 dòng, 5 MB); có dòng sai thì từ chối cả file (HTTP 422) và liệt kê lỗi theo dòng / cột, không lưu gì; file đúng thì lưu trong một giao dịch, tạo mật khẩu tạm và gửi mail tài khoản (có màn kết quả gửi mail). Test: `NhanVienImportServiceTest.java`, `MailTaiKhoanNhanVienTest.java`, `nhanVienImport.test.js` |
| 21 | "Nhân viên cũng thế" (14:51), "phần update chưa tới demo" (02:38) | Form sửa cũng phải validate như thêm | Dùng chung bộ luật validate cho thêm + sửa | ✅ Thêm và sửa dùng chung một form / một bộ luật cho khách hàng (`KhachHangFormPage.vue`) và nhân viên (`NhanVienFormPage.vue`), backend cũng dùng chung một hàm kiểm tra (`NhanVienService.kiemTra`, `QuyTacDuLieu.java`) cho thêm, sửa và từng dòng file Excel. Khi sửa, email / ngày vào làm / mật khẩu giữ nguyên; sửa ngày sinh làm nhân viên chưa đủ 18 tuổi vào ngày vào làm thì báo lỗi ở ô Ngày sinh. Test: `nhanVienForm.test.js`, `nhanVienValidate.test.js`, `NhanVienServiceQuyTacTest.java`, `KhachHangServiceTest.java` |

## 2. Thứ tự làm (đã xong hết)

1. ✅ Dựng khung kiểm thử cho backend (Maven Central bị chặn ở môi trường làm việc nên dùng bộ giả lập Spring + JUnit để biên dịch và chạy test Java).
2. ✅ Thành phần chung: `Select2`, `DateTimeInput`, phân trang rút gọn.
3. ✅ Sản phẩm: Select2 + Thêm nhanh, ảnh bắt buộc, bỏ cột Giảm giá.
4. ✅ Phiếu giảm giá (backend rồi giao diện), Đợt giảm giá.
5. ✅ Khách hàng, Nhân viên (validate, Import Excel, mail).
6. ✅ Hóa đơn (in PDF, vận chuyển, trạng thái).
7. ✅ Chạy toàn bộ test (vitest + JUnit), build giao diện, sửa lỗi, cập nhật hướng dẫn (`HUONG_DAN_CHAY.md`) và tài liệu căn cứ (`CAN_CU_PHAP_LY_VA_QUY_TAC.md`).

## 3. Giả định đã chọn (cô không nói rõ)

- Logo GHN / GHTK: không có file logo chính thức trong máy làm việc nên bản in dùng ô chữ thay thế; muốn dùng logo thật thì bỏ file `ghn.png`, `ghtk.png` (và `vtp.png` nếu có) vào `frontend/public/van-chuyen/` (code tự dùng).
- Khách hàng: không nhập ngày sinh thì được, có nhập thì phải đủ 15 tuổi. Nhân viên: bắt buộc ngày sinh và phải đủ 18 tuổi.
- Phiếu giảm giá / đợt giảm giá bắt đầu trong quá khứ khi tạo mới bị chặn (cho lệch tối đa 5 phút so với giờ hiện tại để bấm "Bây giờ" không bị lỗi).
- Chồng ưu đãi: mặc định lấy mức cao nhất (cô hỏi "cộng dồn hay lấy mức cao nhất" nhưng chưa chốt) và có thể đổi sang cộng dồn bằng cấu hình mà không phải sửa code.
- "Hoàn tiền" (mã 7) vẫn chỉ là tab theo thiết kế của nhóm, chưa có luồng chuyển sang vì cô không nhắc tới; "Giao hàng thất bại" là mã 8.
- Đơn mẫu có trạng thái giao hàng thất bại (HD19) chỉ có khi chạy `database/SD_17_du_lieu_va_nang_cap.sql` PHẦN 8.2.
- Hai ô Tháng / Năm trong lịch của ô chọn ngày vẫn là `<select>` thường vì chúng là nút điều hướng lịch, không phải combobox nhập dữ liệu.

## 4. Kết quả kiểm thử

| Phần | Kết quả |
|---|---|
| Giao diện (vitest + jsdom) | 729 test, 34 file: **đạt hết** |
| Backend (JUnit 5) | 429 test, 36 nhóm test: **đạt hết** (chạy bằng bộ giả lập Spring ở môi trường làm việc, xem lưu ý dưới) |
| Build giao diện (`vite build`) | Thành công |
| Xem bằng mắt (trình duyệt tự động, dữ liệu giả) | Hóa đơn (mọi trạng thái, bản in), Phiếu giảm giá, Đợt giảm giá, Khách hàng, Nhân viên + Import Excel, Sản phẩm: đều hiện đúng; đã sửa lỗi nhãn "Ảnh chính" đè lên nút ✕ ở ô ảnh |

Lưu ý: môi trường làm việc không tải được Maven Central nên phần Java chạy bằng bộ giả lập Spring / JUnit; trước khi nộp nên chạy lại bằng Maven thật: `cd backend && ./mvnw test`.
Riêng test `BackendApplicationTests` (có sẵn từ bản gốc) khởi động cả ứng dụng nên cần SQL Server đang chạy và **không nằm trong 429 test ở trên**; 429 test đó là test thường, không cần cơ sở dữ liệu.
Chưa bật SQL Server thì bỏ qua test đó: `./mvnw test -Dtest='!BackendApplicationTests'` (Windows PowerShell / cmd: `mvnw.cmd test "-Dtest=!BackendApplicationTests"`).

### Chạy được trên máy / phiên bản khác (bổ sung sau)
Để project chạy được trên máy khác (JDK, Node, hệ điều hành, ngôn ngữ / múi giờ khác nhau) mà không lỗi, đã làm thêm:
- `pom.xml`: Spring Boot 3.3.0 → 3.3.13 (bản vá cuối của nhánh 3.3), Lombok 1.18.48, Byte Buddy 1.17.8 để biên dịch / chạy được trên JDK mới; `BackendApplication` bật sẵn `net.bytebuddy.experimental`.
- Bỏ phụ thuộc vào ngôn ngữ của máy: `String.format` / `toUpperCase` / `toLowerCase` trong backend đều chỉ định `Locale` (máy tiếng Ả Rập / Thái / Thổ Nhĩ Kỳ không còn sinh mã sai kiểu `NV٠٣`); test canh giữ `KhongPhuThuocMayTest`, `MaUtilTest`.
- Test giao diện về mã QR không còn đỏ ngẫu nhiên trên máy chậm (đợi bằng `vi.waitFor` thay vì ngủ 30 ms).
- `frontend/package.json` khai báo `engines` (Node 20.19+ hoặc 22.12+), thêm `frontend/.nvmrc`.
- Script `scripts/kiem-tra-moi-truong.cjs` kiểm tra Node / Java / SQL Server và báo cách sửa; `chay-du-an.bat` gọi nó trước khi chạy, thêm `chay-du-an.sh` cho macOS / Linux.
- Bảng phiên bản yêu cầu, những gì đã / chưa kiểm chứng và lỗi thường gặp: xem mục "Phiên bản được hỗ trợ" trong `HUONG_DAN_CHAY.md`.

### Gộp với bản của nhóm trên git (bổ sung sau)
Chỉ lấy phần mới thật sự từ bản nhóm (ảnh đại diện khách hàng, cột "Tên người nhận", hộp xác nhận có icon, CORS mọi cổng localhost); những chỗ bản nhóm cũ hơn bản này (mật khẩu chữ thường, SĐT 10-11 số, bỏ `DateInput`, các trang đã sửa theo cô...) thì giữ bản đang có. Bảng chi tiết: mục "Gộp với bản của nhóm trên git" trong `HUONG_DAN_CHAY.md`.

## 5. Kịch bản demo trước cô (các "case khó")

**Phiếu giảm giá** (phần của bạn)
1. Bấm Thêm, lưu ngay khi để trống: mỗi ô bắt buộc hiện lỗi đỏ ngay dưới ô.
2. Loại giảm "%" nhập 0 hoặc 101: "Phần trăm giảm từ 1 đến 100."
3. Chọn ngày giờ bắt đầu trong quá khứ: "Ngày giờ bắt đầu không được ở quá khứ."; chọn giờ hợp lệ thì "Trạng thái dự kiến" đổi ngay (Sắp diễn ra / Đang hoạt động / Đã kết thúc) kèm mô tả "Bắt đầu lúc …, còn … nữa".
4. Chọn "Nhóm chỉ định (Cá nhân)": ô Số lượng biến mất, bảng khách hàng hiện ra (Tổng đơn, Mua gần nhất, Ngày sinh, Giới tính, bộ lọc); chuyển lại "Tất cả (Công khai)" thì ô Số lượng hiện lại.
5. Thẻ "Khi có nhiều ưu đãi cùng lúc thì tính thế nào?" và "Ví dụ với phiếu này": trả lời câu hỏi "cộng dồn hay lấy mức cao nhất".

**Đợt giảm giá**: ngày giờ kết thúc trước ngày giờ bắt đầu; bắt đầu trong quá khứ; chọn biến thể đã nằm trong đợt khác thì hộp cảnh báo hiện tên đợt đó; mở đợt đã bắt đầu thì không đổi được giờ bắt đầu.

**Khách hàng**: SĐT "0123456789" -> "Số điện thoại gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09."; SĐT / email trùng -> "Số điện thoại đã tồn tại (khách hàng …)." / "Email đã tồn tại (khách hàng …)."; ngày sinh làm khách mới 10 tuổi -> "Khách hàng phải từ đủ 15 tuổi."; mở form sửa và làm lại các lỗi trên: báo y như form thêm.

**Nhân viên**: ngày sinh làm nhân viên 17 tuổi -> "Nhân viên phải từ đủ 18 tuổi."; mật khẩu "abcdefgh" -> "Mật khẩu phải có cả chữ và số, không chứa khoảng trắng."; Nhập Excel: tải file mẫu, cố ý để sai 1 dòng -> cả file bị từ chối và hiện danh sách lỗi theo dòng / cột, không nhân viên nào được lưu; sửa lại cho đúng -> nhập thành công, hiện kết quả gửi mail tài khoản.

**Sản phẩm**: lưu khi chưa có ảnh -> báo thiếu ảnh dưới khung ảnh; chọn 2 màu × 2 trọng lượng × 2 chu vi -> đúng 8 biến thể; gõ tên màu chưa có trong ô chọn -> bấm "Thêm nhanh" ngay trong ô; trang Biến thể không còn cột Giảm giá (giá gốc gạch ngang + giá sau giảm nằm ngay cột Giá bán); danh sách nhiều trang hiện dạng `1 2 … 20`.

**Hóa đơn**: đơn tại quầy phí vận chuyển 0, khách lẻ không có SĐT; đơn giao hàng bấm "Đã xác nhận" thì hộp thoại in PDF tự mở; bản in có mã phiếu + %, phí vận chuyển, đơn vị vận chuyển, QR và không có Lịch sử thanh toán; đơn "Đã hoàn thành" không còn nút đổi trạng thái; đơn "Đang giao hàng" bấm "Giao hàng thất bại" (bắt buộc ghi lý do), sau đó chọn Giao lại hoặc Hủy đơn (trả lại tồn kho).

---
name: giai-doan-2-footstyle
description: Toàn bộ nội dung Giai đoạn 2 (GĐ2) của dự án FootStyle (SD_017) — lời cô review, 21 việc đã sửa, quy tắc nghiệp vụ, căn cứ pháp lý (tuổi 15/18, định dạng dữ liệu), giải thích luồng Sản phẩm/Nhân viên, kịch bản demo và kết quả kiểm thử. Dùng khi cần trả lời câu hỏi về GĐ2, ôn demo trước hội đồng, hoặc đối chiếu tính năng đã làm so với yêu cầu.
---

# Giai đoạn 2 (GĐ2) — Dự án FootStyle (SD_017)

> File này gộp lại toàn bộ các ghi chú rời rạc trước đây (`KE_HOACH_SUA_THEO_LOI_CO.md`, `CAN_CU_PHAP_LY_VA_QUY_TAC.md`,
> `GIAI_THICH_SAN_PHAM.md`, `GIAI_THICH_NGAY_2.md`, `KIEM_TRA_HOP_NHAT.txt`) thành một tài liệu duy nhất để tra cứu nhanh.
> Hướng dẫn chạy project (cài đặt, API, các trang) vẫn nằm riêng ở `HUONG_DAN_CHAY.md`.

## 0. Tóm tắt kết luận của cô (lý do GĐ2 phải sửa)

Cô review: **chỉ có Sản phẩm và Hóa đơn "tạm được"**, các module còn lại (Phiếu giảm giá, Đợt giảm giá, Khách hàng, Nhân viên)
**chưa được**, chưa thể hiện được "case khó" (validate). Nguồn: lời cô trong video review nhóm SD-17 (11:02–15:05) kèm nhận xét
chung với nhóm SD-09, SD-03 ("các nhóm xem lại").

**Trạng thái hiện tại: đã làm xong toàn bộ 21 ý bên dưới, đã chạy test toàn bộ (xem mục 6).**

## 1. Bảng 21 việc đã sửa theo lời cô

Tên file: giao diện ở `frontend/src/`, test giao diện ở `frontend/tests/`, test backend ở `backend/src/test/java/com/footstyle/demo/`.

| # | Cô nói (mốc video) | Hiểu là | Việc làm | Kết quả |
|---|---|---|---|---|
| 1 | "Chưa chuyển thành Select2 tất cả các combobox" (12:16) | Mọi ô chọn phải là Select2 có tìm kiếm | Component `Select2.vue`, thay hết `<select>` | ✅ `components/common/Select2.vue` dùng ở 19 trang/form. Không còn `<select>` thường, trừ ô Tháng/Năm trong lịch chọn ngày (`DateInput.vue`). Test: `thanhPhanChung.test.js`, `helpersSelect2.js` |
| 2 | "Chưa có thêm nhanh" (12:22) | Gõ tên chưa có thì "Thêm nhanh" ngay trong Select2 | Nút "Thêm nhanh" cho Danh mục, Thương hiệu, Xuất xứ, Chất liệu, Độ cứng, Điểm cân bằng, Màu sắc, Trọng lượng, Chu vi | ✅ `composables/useThemNhanh.js` + `Select2.vue`. Test: `sanPhamAnhThemNhanh.test.js` |
| 3 | "2×2×2 = 8 thì dưới này phải có 8 biến thể" (12:30) | Số biến thể = tích số màu × trọng lượng × chu vi | `utils/bienThe.js` (`taoBienThe`, `congThucSoBienThe`) + hiện tổng số | ✅ Form hiện "Tổng cộng N biến thể (2 màu × 2 trọng lượng × 2 chu vi)". Test: `bienTheTheoMau.test.js` (2×2×2=8, 3×3×1=9, 4×3×2=24) |
| 4 | "Ảnh đang chưa có mà sao vẫn thành công" (13:18) | Chưa có ảnh thì không lưu được sản phẩm | Bắt buộc ảnh, chặn cả backend | ✅ Giao diện: khung ảnh có `*`, lỗi dưới khung ảnh. Backend: thiếu ảnh → 400, không lưu. Test: `sanPhamAnhThemNhanh.test.js`, `SanPhamAnhBatBuocTest.java` |
| 5 | "Giảm giá, xóa cột này đi, sau này gạch trừ" (13:27) | Bỏ cột Giảm giá, giá hiện gạch ngang + giá sau giảm | Bỏ cột, hiển thị giá gạch ngang | ✅ `BienTheManager.vue` bỏ cột Giảm giá; cột Giá bán hiện giá sau giảm + giá gốc gạch ngang (`composables/useGiamGiaBienThe.js`, `utils/giamGia.js`). File Excel có cột "Giá sau giảm (₫)" thay "Giảm giá (%)". Test: `giamGiaBienThe.test.js` |
| 6 | "Phân trang nhiều quá thì chỉ 1 2 … số cuối" (13:35) | Phân trang rút gọn có dấu ba chấm | `BasePagination.vue` viết lại | ✅ `utils/phanTrang.js` + `BasePagination.vue` dùng chung mọi bảng: `1 2 3 … 20`, không nút « ». Test: `thanhPhanChung.test.js` |
| 7 | "Không cần in Lịch sử thanh toán" / "in mã đã dùng, % giảm, phí ship, logo đơn vị giao hàng, QR là OK" (11:49, 11:53) | Sửa bản in hóa đơn | Bỏ khối Lịch sử thanh toán; thêm mã phiếu + %, phí ship, logo vận chuyển, giữ QR | ✅ `utils/hoaDonIn.js` + `utils/vanChuyen.js` + `VanChuyenLogo.vue`. Test: `hoaDonTrangThaiIn.test.js` |
| 8 | "Chuyển Đã xác nhận thì tự in PDF, chưa có" (06:56) | Bấm xác nhận là tự mở in/PDF | Mở cửa sổ in ngay khi chuyển trạng thái | ✅ `HoaDonChiTiet.vue`. Test: `hoaDonChiTietTrangThai.test.js`, `hoaDonTrangThaiIn.test.js` |
| 9 | "Tại quầy sao có phí ship 30k" (10:43), "Khách lẻ sao có SĐT" (07:13) | Tại quầy phí ship=0, không đơn vị vận chuyển; khách lẻ ẩn SĐT | Sửa backend + giao diện, dọn dữ liệu cũ | ✅ `HoaDonService`, `SchemaUpgradeRunner`, SQL mục 4.2. Test: `HoaDonChiTietHienThiTest.java`, `SchemaUpgradeRunnerHoaDonTest.java` |
| 10 | "Đã hoàn thành rồi sao còn chuyển trạng thái" (10:36) | Trạng thái cuối khóa nút chuyển | Luật chuyển trạng thái tập trung 1 chỗ ở backend | ✅ `HoaDonLuongTrangThai.java`, API trả `chuyenDuoc`. Test: `HoaDonLuongTrangThaiTest.java`, `HoaDonDoiTrangThaiTest.java` |
| 11 | "Giao hàng thất bại chưa thể hiện" (07:24) | Thêm nhánh Giao hàng thất bại | Trạng thái mã 8, bắt buộc lý do, giao lại/hủy | ✅ Đang giao hàng → Giao hàng thất bại → Giao lại hoặc Hủy (trả tồn kho). Test: `HoaDonGiaoThatBaiTest.java` |
| 12 | Phiếu giảm giá "làm lại, không đúng yêu cầu" (13:52) | Theo mẫu nhóm đầu: Thông tin chung + Đối tượng áp dụng, ngày **giờ**, Trạng thái dự kiến | Viết lại form + backend nhận ngày giờ | ✅ `PhieuGiamGiaFormPage.vue`, `DateTimeInput.vue`, backend `ThoiGianPhut.java`. Test: `phieuGiamGia.test.js`, `phieuGiamGiaNgayGio.test.js` |
| 13 | Số lượng công khai nhập được, cá nhân phải tự biến mất (04:50) | Ô số lượng chỉ ở phiếu công khai | Ẩn ô khi chọn Cá nhân, số lượng = số khách chọn | ✅ Test: `phieuGiamGia.test.js`, `phieuCaNhanMail.test.js` |
| 14 | Lọc khách tặng phiếu cá nhân: ngày mua, ngày sinh, giới tính (05:09) | Bảng chọn khách có cột + bộ lọc | `KhachHangChonBang.vue` + `utils/khachChonPhieu.js` | ✅ Cột Tổng đơn, Mua gần nhất, Ngày sinh, Giới tính; lọc theo các cột đó. Test: `phieuCaNhanMail.test.js` |
| 15 | Phiếu/đợt chồng nhau: "cộng dồn hay lấy mức cao nhất" (06:13) | Phải thể hiện + demo được | `GiamGiaChongDot.java`, cấu hình `MAX`/`CONG_DON`/`TRUNG_BINH` | ✅ Mặc định `MAX`. Form có thẻ giải thích + ví dụ tính, cảnh báo trùng đợt (`DotGiamGiaChongDotCard.vue`). Test: `GiamGiaChongDotTest.java`, `ViDuGiamGiaTrenFormTest.java` |
| 16 | Hóa đơn "chưa thể hiện dùng mã nào, giảm bao nhiêu %" (07:13, 10:43) | Chi tiết + bản in ghi rõ mã + % | API trả mã, loại, giá trị phiếu | ✅ Dòng "Phiếu giảm giá" + bản in "Giảm giá (mã … – giảm …%, tối đa …)". Test: `HoaDonChiTietHienThiTest.java` |
| 17 | Đợt giảm giá "làm lại" (14:10) | Ngày giờ, trạng thái tự theo thời gian, case khó | Viết lại validate + cảnh báo trùng đợt | ✅ `DotGiamGiaFormPage.vue`, `utils/ngayGio.js`, `DotGiamGiaLichCard.vue`. Test: `dotGiamGia.test.js`, `DotGiamGiaControllerTest.java` |
| 18 | Khách hàng "chưa thể hiện validate, case khó" (14:30, 14:42) | Bắt lỗi + hiện lỗi: SĐT, trùng email/SĐT, tuổi | Validate đủ 2 lớp, lỗi dưới từng ô | ✅ `KhachHangFormPage.vue` + `utils/kiemTra.js` + backend `QuyTacDuLieu.java`. Test: `khachHangForm.test.js`, `KhachHangServiceTest.java` |
| 19 | "Tuổi từ 15 phải có căn cứ đàng hoàng" (01:03) | Mọi mốc tuổi phải có lý do nêu được | Khách ≥15 (BLDS Đ.21), NV ≥18 (BLLĐ Đ.143) | ✅ Ghi dưới ô Ngày sinh, trong code và mục 2 bên dưới. Test: `kiemTra.test.js`, `QuyTacDuLieuTest.java` |
| 20 | Nhân viên "chỉ 2 quyền", "import Excel, 1 dòng sai thì cả file không đẩy, tự gửi mail" (02:17, 02:45) | Import Excel kiểm cả file, gửi mail tài khoản | `NhanVienImportService.java` + modal import | ✅ Sai 1 dòng → từ chối cả file (422), đúng → lưu 1 giao dịch + gửi mail. Test: `NhanVienImportServiceTest.java` |
| 21 | "Nhân viên cũng thế" (14:51), "update chưa tới demo" (02:38) | Form sửa phải validate như thêm | Dùng chung bộ luật thêm + sửa | ✅ `KhachHangFormPage.vue`, `NhanVienFormPage.vue` dùng chung 1 bộ luật kể cả Excel. Test: `nhanVienForm.test.js`, `NhanVienServiceQuyTacTest.java` |

### Thứ tự đã làm
1. ✅ Khung kiểm thử backend. 2. ✅ Thành phần chung (Select2, DateTimeInput, phân trang rút gọn). 3. ✅ Sản phẩm.
4. ✅ Phiếu + Đợt giảm giá. 5. ✅ Khách hàng, Nhân viên. 6. ✅ Hóa đơn. 7. ✅ Chạy toàn bộ test, build, cập nhật tài liệu.

### Giả định đã chọn (cô không nói rõ)
- Logo GHN/GHTK: chưa có file chính thức → bản in dùng ô chữ thay thế; có file thật thì bỏ `ghn.png`, `ghtk.png`, `vtp.png` vào `frontend/public/van-chuyen/`.
- Khách hàng: ngày sinh không bắt buộc, có nhập thì phải đủ 15 tuổi. Nhân viên: bắt buộc, đủ 18 tuổi.
- Phiếu/đợt không được bắt đầu trong quá khứ khi tạo mới (lệch tối đa 5 phút cho nút "Bây giờ").
- Chồng ưu đãi: mặc định lấy mức cao nhất (`MAX`), đổi được bằng cấu hình, không phải sửa code.
- "Hoàn tiền" (mã 7) chỉ là tab theo thiết kế nhóm, chưa có luồng chuyển sang vì cô không nhắc tới. "Giao hàng thất bại" là mã 8.
- Hai ô Tháng/Năm trong lịch chọn ngày vẫn là `<select>` thường vì là nút điều hướng lịch, không phải combobox nhập liệu.

## 2. Căn cứ pháp lý và quy tắc nghiệp vụ

> Cách nhóm vận dụng luật để đặt quy tắc phần mềm, không phải tư vấn pháp lý. Mốc nào là quy định nội bộ (không phải luật bắt buộc) ghi rõ.

### 2.1 Độ tuổi

| Đối tượng | Mốc | Bắt buộc? | Căn cứ |
|---|---|---|---|
| Khách hàng | từ đủ **15 tuổi** | Ngày sinh có thể để trống; đã nhập thì phải đủ 15 | Bộ luật Dân sự 2015, Điều 21 khoản 4 |
| Nhân viên | từ đủ **18 tuổi** | Ngày sinh bắt buộc | Bộ luật Lao động 2019, Điều 143 khoản 1 |
| Cả hai | tối đa 100 tuổi | - | Không phải luật, chỉ chặn gõ nhầm năm sinh |

**Khách hàng (BLDS 2015, Điều 21):** khoản 4 — người từ đủ 15 đến chưa đủ 18 tuổi tự mình xác lập giao dịch dân sự (trừ bất động sản/động sản
phải đăng ký). Mua vợt/phụ kiện là động sản thông thường → người từ đủ 15 tuổi tự mua được. Luật **không cấm** dưới 15 tuổi mua hàng (khoản 3: cần
người đại diện đồng ý), nhưng hệ thống không kiểm chứng được sự đồng ý đó nên lấy mốc 15 làm điều kiện tạo hồ sơ. Khách vãng lai (mua tại quầy,
không tạo hồ sơ) không bị hỏi tuổi.

**Nhân viên (BLLĐ 2019, Điều 143–147):** người dưới 18 tuổi là "lao động chưa thành niên", bị hạn chế công việc/nơi làm/thời giờ làm và có nguyên
tắc sử dụng riêng. Cửa hàng chọn mốc 18 để nhân viên (làm ca, xử lý hóa đơn, tiền) không thuộc diện phải quản lý các hạn chế đó. Luật không cấm hẳn
15–17 tuổi đi làm — mốc 18 là **quy định nội bộ**. Ràng buộc thêm: ngày vào làm không sớm hơn sinh nhật thứ 18.

**Cách tính tuổi:** tuổi tròn theo ngày sinh (chưa tới sinh nhật thì chưa cộng tuổi). Ví dụ mốc 15/10/2026: sinh 15/10/2011 → đủ 15 (hợp lệ); sinh
16/10/2011 → 14 tuổi 364 ngày → lỗi "Khách hàng phải từ đủ 15 tuổi.". Ngày sinh tương lai → lỗi; quá 100 tuổi → lỗi. Áp dụng cùng một bộ luật cho
Thêm/Sửa/Import Excel (`backend/.../service/QuyTacDuLieu.java`, bản sao `frontend/src/utils/kiemTra.js`, hai bản đối chiếu bằng test).

### 2.2 Định dạng dữ liệu

| Trường | Quy tắc | Lý do |
|---|---|---|
| SĐT (khách, người nhận, nhân viên) | đúng 10 chữ số, bắt đầu bằng 0 | Quy ước SĐT Việt Nam |
| Email | `ten@tenmien.xx`, tối đa 255 ký tự | Nhân viên dùng làm tên đăng nhập + nhận mail tài khoản |
| Họ tên | bắt đầu bằng chữ cái, chỉ chữ (có dấu)/khoảng trắng/`.`/`'`/`-`, không số; khách 2–100 ký tự | Chặn số/ký tự lạ |
| CCCD | đúng 12 chữ số, không bắt buộc, không trùng | Số định danh cá nhân hiện hành |
| Mật khẩu NV mới | 8–50 ký tự, có cả chữ và số, không khoảng trắng, băm BCrypt | Bảo mật cơ bản |

**Chống trùng** (frontend kiểm ngay khi gõ, backend kiểm lại, CSDL có UNIQUE làm chốt cuối): email + SĐT khách hàng; email + SĐT + CCCD nhân viên;
mã phiếu giảm giá; mã đợt giảm giá; bộ Màu×Trọng lượng×Chu vi của một sản phẩm. Email so sánh không phân biệt hoa thường.

**Hiển thị lỗi:** màu đỏ ngay dưới từng ô sai, hiện tất cả cùng lúc (không phải từng lỗi một); form Sửa dùng đúng bộ luật form Thêm.

### 2.3 Nhân viên: 2 quyền + Import Excel
Vai trò chỉ **Quản lý** và **Nhân viên** (backend từ chối vai trò khác). Import Excel: tải mẫu `.xlsx` → "Kiểm tra file" → "Nhập vào hệ thống", tối
đa 100 dòng/file. **Sai 1 dòng là từ chối cả file** (HTTP 422, chưa ghi gì), bảng lỗi nêu đúng dòng/cột/lỗi (dòng 1 là tiêu đề). Kiểm tra giống form
Thêm + trùng email/SĐT/CCCD trong file và với dữ liệu đã có. File hợp lệ → ghi trong 1 giao dịch, sinh mật khẩu tạm, tự gửi mail tài khoản (cần cấu
hình `mail.properties`); chưa cấu hình thì báo rõ + cho tải danh sách tài khoản.

### 2.4 Phiếu giảm giá

| Quy tắc | Giá trị |
|---|---|
| Mã phiếu | 4–20 ký tự chữ HOA/số, không dấu/khoảng trắng, không trùng |
| Tên phiếu | tối đa 100 ký tự |
| Loại giảm | % (1–100) hoặc số tiền (1.000 – 100.000.000 ₫) |
| Giảm tối đa (phiếu %) | trống = không giới hạn; có thì >0, ≤100.000.000 ₫ |
| Đơn tối thiểu | 0–1.000.000.000 ₫; phiếu tiền mặt thì đơn tối thiểu ≥ số tiền giảm |
| Hình thức | Công khai: có ô Số lượng (1–1.000.000) + giới hạn/khách; Cá nhân: chọn khách, không có ô Số lượng (tự = số khách chọn) |
| Ngày giờ | có giờ phút, kết thúc sau bắt đầu |
| Trạng thái dự kiến | hiện ngay trên form theo ngày giờ đã chọn |

Phiếu cá nhân: bảng chọn khách có cột Tổng đơn (chỉ đếm đơn Đã hoàn thành), Mua gần nhất, Ngày sinh, Giới tính + bộ lọc theo các cột đó. Khách
ngừng hoạt động không chọn được. Tạo phiếu cá nhân thì tự gửi mail cho khách.

### 2.5 Đợt giảm giá và chồng ưu đãi

**Quy tắc ngày giờ chung** (`ThoiGianPhut.java`): tính theo phút, giờ kết thúc tính tới hết phút đó; tạo mới không bắt đầu trong quá khứ (lệch tối
đa 5 phút); sửa phiếu/đợt đã bắt đầu thì không đổi giờ bắt đầu.

**Đợt giảm giá:** mã 2–20 ký tự (chữ/số/`-`/`_`), tên ≤255, mô tả ≤500, mức giảm 1–100%, ít nhất 1 biến thể, biến thể ngừng bán không áp dụng được.

**Nhiều đợt chồng nhau cùng lúc:** chọn bằng cấu hình `app.giam-gia.che-do-chong-dot` (hoặc env `GIAM_GIA_CHE_DO`), mặc định `MAX`.

| Chế độ | Cách tính | VD đợt 10% + 15% |
|---|---|---|
| `MAX` (mặc định) | lấy mức giảm cao nhất | 15% |
| `CONG_DON` | cộng dồn, không quá 100% | 25% |
| `TRUNG_BINH` | trung bình cộng | 12,5% |

Lý do chọn `MAX`: khách luôn hưởng mức tốt nhất, cửa hàng không bị cộng dồn ngoài dự tính. Đổi cách tính chỉ cần đổi cấu hình, không sửa code.

**Phiếu + đợt cùng lúc:** (1) mỗi đơn chỉ dùng 1 phiếu, các phiếu không cộng dồn với nhau; (2) đợt giảm trước (trên giá từng biến thể), phiếu
giảm sau (trên tổng tiền đã trừ giảm giá đợt, đơn tối thiểu của phiếu cũng xét trên số đã trừ); (3) nhiều đợt cùng áp 1 biến thể → theo bảng trên.

Hóa đơn: giá từng dòng là giá sau giảm theo đợt (giá gốc gạch ngang + % giảm); phiếu trừ ở cấp đơn, ghi rõ mã phiếu + mức giảm, ví dụ
`Giảm giá (mã VCH7QX2YN – giảm 10%, tối đa 200.000 ₫)`.

### 2.6 Hóa đơn — trạng thái

| Mã | Trạng thái | Ghi chú |
|---|---|---|
| 1 | Chờ xác nhận | |
| 2 | Đã xác nhận | tự mở bản in/PDF |
| 3 | Chờ giao hàng | |
| 4 | Đang giao hàng | |
| 5 | Đã giao hàng | |
| 6 | Đã hoàn thành | trạng thái cuối, khóa mọi nút chuyển |
| 0 | Đã hủy | trạng thái cuối, bắt buộc lý do, trả tồn kho |
| 8 | Giao hàng thất bại | bắt buộc lý do; từ đây Giao lại (về Đang giao hàng) hoặc Hủy (trả tồn kho) |
| 7 | Hoàn tiền | chỉ là tab theo thiết kế, chưa có luồng chuyển sang |

- Đơn giao hàng: Chờ xác nhận → Đã xác nhận → Chờ giao hàng → Đang giao hàng → Đã giao hàng → Đã hoàn thành. Hủy được khi chưa giao đơn vị vận
  chuyển (3 bước đầu). Từ Đang giao hàng: Đã giao hàng hoặc Giao hàng thất bại.
- Đơn tại quầy: Chờ xác nhận → Đã hoàn thành; "chờ" thì hủy được; không có Giao hàng thất bại.
- Luật chuyển trạng thái tập trung 1 chỗ ở backend (`HoaDonLuongTrangThai.java`); giao diện chỉ hiện nút mà API trả về trong `chuyenDuoc`; gọi API
  trái luật cũng bị từ chối.
- Đơn tại quầy: phí vận chuyển = 0, không đơn vị vận chuyển. Khách lẻ (không gắn hồ sơ): hiện "Khách vãng lai", không có SĐT.
- Bản in: bỏ khối Lịch sử thanh toán; có dòng giảm giá (mã + %), phí vận chuyển (đơn giao hàng), logo đơn vị vận chuyển (GHN/GHTK/Viettel Post —
  logo là nhãn hiệu công ty nên không đóng gói sẵn, chưa có file thì dùng ô chữ thay thế, có file thật thì đặt vào `frontend/public/van-chuyen/`),
  giữ mã QR.

### 2.7 Sản phẩm
- Mọi ô chọn là Select2 (tìm kiếm được); gõ tên chưa có → "Thêm nhanh" tạo + tự chọn luôn (Danh mục, Thương hiệu, Xuất xứ, Chất liệu, Độ cứng, Điểm
  cân bằng, Màu sắc, Trọng lượng, Chu vi cán).
- Biến thể = Màu sắc × Trọng lượng × Chu vi cán (riêng vợt cầu lông); danh mục khác chỉ theo Màu sắc, 4 thuộc tính riêng của vợt ẩn đi, CSDL dùng
  mục "Không áp dụng" (DC000/CB000/TL000/CV000) để vẫn thỏa ràng buộc khóa ngoại.
- Ảnh bắt buộc: ít nhất 1 ảnh chung hoặc ảnh riêng từng màu; thiếu ảnh → không lưu được (chặn cả 2 lớp).
- Bảng biến thể không còn cột Giảm giá — giá gạch ngang + giá sau giảm nằm trong cột Giá bán.
- Mọi danh sách dùng phân trang rút gọn (`1 2 … 20`, `1 2 … 9 10 11 … 20`).

## 3. Luồng Sản phẩm — giải thích chi tiết (hỏi đáp)

**Các file chính:** `services/sanPhamService.js` (gọi `/api/san-pham`, `/api/bien-the-san-pham`), `services/thuocTinhService.js` (9 bảng thuộc
tính), `components/SanPhamManager.vue` (danh sách, lọc, phân trang, Excel), `components/sanPham/SanPhamFormPage.vue` (trang riêng Thêm/Sửa),
`components/sanPham/SanPhamDetailModal.vue` (chi tiết + biến thể), `utils/text.js` (`includesText`, `layTen`, `layLuaChon`).

**Luồng:** mở trang → `taiDuLieu()` lấy sản phẩm/biến thể/9 bảng thuộc tính → `sanPhamKemThongKe` (computed) tính số biến thể/tổng tồn/giá thấp
nhất (chỉ biến thể hoạt động) → `danhSachDaLoc` lọc theo từ khóa/danh mục/thương hiệu/trạng thái → `dongHienThi` cắt theo trang → "Thêm sản phẩm"
chuyển `/san-pham/them`, "Chỉnh sửa" chuyển `/san-pham/:id/sua` → lưu xong quay về danh sách → nút nguồn đổi `hoatDong` (không xóa dữ liệu).

**Thêm vs Sửa:** biến `spDangSua` — `null` là thêm, có giá trị là sửa; khi sửa ô Mã bị khóa (`readonly`, mã không đổi được). Sửa được: tên, danh
mục, thương hiệu, xuất xứ, chất liệu, độ cứng, điểm cân bằng, mô tả, ảnh. Không sửa: mã sản phẩm, ngày tạo.

**Câu hỏi hay gặp:**
- *Vì sao kiểm tra cả frontend lẫn backend?* Frontend báo lỗi nhanh cho người dùng; backend là lớp bảo vệ thật (có thể gọi API không qua form).
- *Vì sao sau khi lưu phải tải lại danh sách?* Để lấy dữ liệu mới nhất từ DB (id, ngày tạo do backend cấp).
- *Ảnh lưu thế nào?* Bảng `hinh_anh_san_pham`, đúng 1 ảnh `la_anh_chinh=1`/sản phẩm; ảnh bị bỏ khi sửa → `trang_thai=0`, không xóa dòng.
- *`ref` vs `reactive`?* `ref` cho 1 giá trị đơn (cần `.value` trong script), `reactive` cho object.
- *`computed` khác hàm thường?* Tự tính lại + cache khi dữ liệu phụ thuộc đổi.
- *`watch` dùng để làm gì?* Chạy hành động khi dữ liệu đổi (vd đổi bộ lọc → về trang 1).
- *Component cha-con nói chuyện?* Cha→con bằng `props`, con→cha bằng `emit`; con không tự sửa dữ liệu cha.
- *Vì sao không xóa sản phẩm/biến thể?* Đã có dữ liệu liên quan (hóa đơn, đợt giảm giá) → chỉ đổi `trang_thai`.
- *Biến thể là gì?* sản phẩm + màu + trọng lượng + chu vi (`san_pham_chi_tiet`); trùng bộ 4 giá trị thì bị chặn (`laTrungBienThe`).
- *Validate gồm gì?* Mã bắt buộc 3–20 ký tự chữ/số không trùng; tên bắt buộc ≤255; 6 ô chọn bắt buộc; ảnh ≤10 (ảnh đầu là ảnh chính), chỉ nhận ảnh
  tải lên (`/api/uploads/...`) hoặc link http(s).
- *Quy tắc backend giữ (hợp ERD)?* Thuộc tính mới chọn phải đang hoạt động (sản phẩm cũ dùng thuộc tính đã ngưng vẫn sửa được nếu không đổi mục
  đó); biến thể: sản phẩm phải hoạt động mới thêm/kích hoạt lại được, không trùng mã/bộ (sp, màu, trọng lượng, chu vi), giá ≥1.000₫, tồn kho
  nguyên không âm; sửa biến thể đổi được sản phẩm (mã tự đổi theo tiền tố sản phẩm mới) nhưng mã biến thể không sửa tay.

**Tải ảnh từ máy:** chọn file → frontend kiểm nhanh (JPG/PNG/WEBP/GIF, ≤5MB, ≤10 ảnh) → `hinhAnhService.upload` gửi multipart tới
`POST /api/hinh-anh/upload` → backend kiểm lại dung lượng + đọc byte đầu để xác định định dạng thật (không tin đuôi file) → lưu `uploads/` với
tên ngẫu nhiên (tránh ghi đè + tránh path traversal) → trả `/api/uploads/<tên>.jpg`. Ảnh đầu là `anhChinh`, còn lại `anhPhu`. Hạn chế còn tồn: ảnh
tải lên rồi bỏ trước khi lưu vẫn nằm trong `uploads/` (chưa có bước dọn); API upload chưa kiểm quyền (chưa có đăng nhập).

**Backend:** entity khớp ERD (`SanPham` 6 khóa ngoại, `SanPhamChiTiet`, `HinhAnhSanPham`); 9 bảng thuộc tính dùng chung `ThuocTinh`
(`@MappedSuperclass`) + `ThuocTinhRepository`/`ThuocTinhService`/`ThuocTinhController`; `ThuocTinhChon.chon(...)` kiểm 1 thuộc tính được chọn dùng
chung cho sản phẩm và biến thể. Đặt tên hàm kiểu `getAll/getById/them/sua/doiTrangThai` (không có `xoa`), dùng `for`/`List` thay `stream`/`Set` để
dễ đọc, hàm `loi(dieuKien, thongBao)` gom validate thành 1 dòng/quy tắc.

## 4. Kịch bản demo trước cô (case khó)

**Phiếu giảm giá:** để trống lưu ngay → lỗi đỏ dưới từng ô; % nhập 0 hoặc 101 → "Phần trăm giảm từ 1 đến 100."; ngày giờ bắt đầu quá khứ → lỗi,
hợp lệ thì "Trạng thái dự kiến" đổi ngay; chọn "Cá nhân" → ô Số lượng biến mất + bảng khách hiện ra; thẻ ví dụ tính chồng ưu đãi.

**Đợt giảm giá:** giờ kết thúc trước giờ bắt đầu; bắt đầu quá khứ; chọn biến thể đã trong đợt khác → cảnh báo tên đợt đó; mở đợt đã bắt đầu →
không đổi được giờ bắt đầu.

**Khách hàng:** SĐT "0123456789" → lỗi đầu số; SĐT/email trùng → lỗi nêu tên khách trùng; ngày sinh làm khách 10 tuổi → "phải từ đủ 15 tuổi."; mở
form sửa lặp lại các lỗi trên → báo y hệt form thêm.

**Nhân viên:** ngày sinh làm NV 17 tuổi → "phải từ đủ 18 tuổi."; mật khẩu "abcdefgh" → "phải có cả chữ và số, không khoảng trắng."; Import Excel
sai 1 dòng → từ chối cả file + liệt kê lỗi theo dòng/cột; sửa đúng → nhập thành công + gửi mail tài khoản.

**Sản phẩm:** lưu khi chưa có ảnh → báo thiếu ảnh; chọn 2×2×2 → đúng 8 biến thể; gõ màu chưa có → "Thêm nhanh" ngay trong ô; trang Biến thể không
còn cột Giảm giá; phân trang `1 2 … 20`.

**Hóa đơn:** tại quầy phí ship=0, khách lẻ không SĐT; bấm "Đã xác nhận" → tự mở in PDF; bản in có mã phiếu+%, phí ship, logo vận chuyển, QR, không
có Lịch sử thanh toán; "Đã hoàn thành" không còn nút đổi trạng thái; "Đang giao hàng" → "Giao hàng thất bại" (bắt buộc lý do) → Giao lại hoặc Hủy
(trả tồn kho).

## 5. Lịch sử gộp 2 bản nhóm (10/10/2026) — bối cảnh, không còn thao tác tiếp

Dự án từng có 2 nhánh phát triển song song được gộp làm một:
- **Bản 1:** nhân viên nhiều địa chỉ + CCCD + quét QR + mã theo tên (`MaNhanVienUtil`), địa chỉ khách hàng qua `DiaChiHanhChinhSelect`, có bộ test
  vitest.
- **Bản 2** (lấy làm gốc, mới hơn ở hóa đơn/đợt giảm giá/sản phẩm/SQL): hóa đơn theo thiết kế, chồng đợt giảm giá MAX/TRUNG_BINH, mail phiếu chỉ
  khi "Sắp diễn ra", nhân viên có vị trí làm việc + cột CCCD, `SchemaUpgradeRunner`, thuộc tính theo danh mục ("Không áp dụng").

Kết quả gộp: giữ Bản 2 làm gốc + đưa phần thiếu của Bản 1 vào (nhân viên nhiều địa chỉ, quét CCCD, địa chỉ khách hàng dùng ô chọn chung, bộ test
vitest). Thống nhất tên: cột CCCD dùng tên `cccd` (không phải `can_cuoc_cong_dan`), mã nhân viên dùng `MaNhanVienUtil`, địa chỉ dùng
`DiaChiHanhChinhSelect`, quét CCCD dùng `CccdScannerModal`. Toàn bộ 6 file SQL rời được gộp thành `database/SD_17_tong.sql` (cấu trúc + dữ liệu
mẫu + nâng cấp) và tách thêm `database/SD_17_du_lieu.sql` (chỉ phần dữ liệu, chạy lại nhiều lần được). Lần gộp sau (mã đợt giảm giá dạng
`DGG`+số, thuộc tính theo danh mục, SĐT KH ở hóa đơn) đã kiểm bằng `npm ci && npm run build` sạch, 84/84 test cũ (thời điểm đó) đạt.

## 6. Kết quả kiểm thử

| Phần | Kết quả |
|---|---|
| Backend (`mvn test`, JUnit 5) | **574/574 pass**, 0 lỗi/fail (xác nhận gần nhất 22/10/2026) |
| Frontend (`npx vitest run`) | **43/43 test files, 789/789 test pass** (xác nhận gần nhất 22/10/2026) |
| Build giao diện (`vite build`) | Thành công |

Trước khi nộp: chạy lại `cd backend && ./mvnw test` (cần SQL Server đang chạy, test `BackendApplicationTests` khởi động cả ứng dụng). Muốn bỏ qua
test đó khi chưa bật SQL Server: `./mvnw test -Dtest='!BackendApplicationTests'` (Windows: `mvnw.cmd test "-Dtest=!BackendApplicationTests"`).

### Chạy được trên máy/phiên bản khác
- `pom.xml`: Spring Boot 3.3.13, Lombok 1.18.48, Byte Buddy 1.17.8 để tương thích JDK mới.
- Không phụ thuộc ngôn ngữ máy: `String.format`/`toUpperCase`/`toLowerCase` ở backend đều chỉ định `Locale`.
- Test QR không còn đỏ ngẫu nhiên trên máy chậm (dùng `vi.waitFor` thay vì ngủ cố định).
- `frontend/package.json` khai báo `engines` (Node 20.19+ hoặc 22.12+), có `.nvmrc`.
- `scripts/kiem-tra-moi-truong.cjs` kiểm Node/Java/SQL Server trước khi chạy (`chay-du-an.bat`/`chay-du-an.sh` tự gọi).

## 7. Nguồn pháp lý đã đối chiếu
- Bộ luật Dân sự 2015, Điều 21 — thuvienphapluat.vn
- Bộ luật Lao động 2019, Chương XI (Điều 143–147) — hethongphapluat.com, toàn văn thuvienphapluat.vn

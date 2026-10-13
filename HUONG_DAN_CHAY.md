# Chạy project FootStyle (Thống kê, Quản lý hóa đơn, sản phẩm, phiếu giảm giá, đợt giảm giá, khách hàng, nhân viên)

1. SQL Server (chỉ 2 file, chạy lần lượt trong SSMS):
   - `database/SD_17.sql` — tạo database `shop_cau_long_footstyle` và toàn bộ bảng. File này giữ nguyên, không sửa. Chạy MỘT LẦN khi tạo CSDL mới.
   - `database/SD_17_du_lieu_va_nang_cap.sql` — dữ liệu mẫu + nâng cấp, gộp từ 5 file cũ theo đúng thứ tự chạy: dữ liệu mẫu (vai trò, 10 nhân viên, 6 khách hàng, 11 phiếu giảm giá),
     sản phẩm (9 bảng thuộc tính, 6 sản phẩm, 14 biến thể), địa chỉ khách hàng + 8 đợt giảm giá, phương thức thanh toán + hóa đơn mẫu, và phần nâng cấp CSDL cũ. Chạy lại nhiều lần được (bản ghi đã có thì bỏ qua).
   Mật khẩu nhân viên mẫu: `Footstyle@123`.
   **Đã có CSDL từ trước?** Không chạy lại `SD_17.sql` (database đã tồn tại), chỉ chạy `database/SD_17_du_lieu_va_nang_cap.sql` (hoặc khởi động lại backend, backend tự bổ sung phần thiếu). Phần nâng cấp bổ sung:
   2 cột `ngay_tao`, `ngay_cap_nhat` của bảng `dia_chi_khach_hang`; cột `can_cuoc_cong_dan` cho `nhan_vien` (CSDL cũ đang có cột `cccd` thì số đã nhập được CHÉP sang, cột `cccd` cũ vẫn được giữ, không xóa dữ liệu);
   bảng `dia_chi_nhan_vien` (mỗi nhân viên nhiều địa chỉ, địa chỉ cũ ở cột `dia_chi` được chuyển thành địa chỉ chính);
   cột `mo_ta` cho `dot_giam_gia`; cột `nhom_ap_dung` cho `chat_lieu` (chất liệu theo danh mục, kèm chất liệu cho quần áo / túi - balo / phụ kiện / quả cầu lông); đổi dữ liệu mẫu giày cũ (nếu có) thành vợt cầu lông;
   **(mới 12/10)** cột `id_mau_sac` cho `hinh_anh_san_pham` (ảnh sản phẩm THEO MÀU, PHẦN 6 cuối file; ảnh cũ có `id_mau_sac = NULL` = ảnh chung, vẫn hiện như trước).
   **Thuộc tính theo danh mục sản phẩm:** CSDL cần 4 mục "Không áp dụng" (`DC00`, `CB00`, `TL00`, `CV00`) cho độ cứng, điểm cân bằng, trọng lượng, chu vi. File `SD_17_du_lieu_va_nang_cap.sql` đã có sẵn; CSDL cũ thì khởi động lại backend (tự bổ sung) hoặc chạy file này.
   **Quy tắc mã (HD01, NV01, SP01, DGG01...):** mọi mã tự cấp = tiền tố + số thứ tự tối thiểu 2 chữ số, KHÔNG đệm thêm số 0; quá 99 thì viết thẳng (HD99 -> HD100). Mã nhân viên = chữ cái đầu của họ tên + số thứ tự (Lê Quốc Tuấn -> `LQT01`). **Mã cũ nhiều số 0** (`HD000001`, `NV0001`, `SP001`, `DGG001`, `SP001-DEN-4U-G5`...) được đổi sang mã gọn tự động khi khởi động lại backend (log: "Đã rút gọn N mã cũ nhiều số 0"), hoặc chạy PHẦN 0 của `SD_17_du_lieu_va_nang_cap.sql`. Chỉ đổi cột mã, không mất liên kết dữ liệu (các bảng nối nhau bằng id); chạy lại nhiều lần vẫn an toàn. Mã phiếu giảm giá do người dùng đặt / sinh ngẫu nhiên (`VCH...`) không bị đổi.
   **Gặp lỗi "Invalid column name 'can_cuoc_cong_dan'" / "Invalid column name 'id_mau_sac'" / "Invalid object name 'dia_chi_nhan_vien'" / trang Hóa đơn báo "Không tải được danh sách hóa đơn"?** Nghĩa là CSDL được tạo từ bản SQL cũ.
   Cách xử lý: khởi động lại backend (backend tự bổ sung phần còn thiếu và ghi dòng "Đã kiểm tra / nâng cấp cấu trúc CSDL" vào log), hoặc chạy `database/SD_17_du_lieu_va_nang_cap.sql`.
2. Backend (Java 17, cổng 8081): sửa user/mật khẩu SQL Server trong `backend/src/main/resources/application.properties`
   (hoặc đặt biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), rồi `cd backend && ./mvnw spring-boot:run`.
   **Gửi mail phiếu giảm giá cá nhân (không bắt buộc):** thư gửi dạng HTML (có bản chữ dự phòng), có 3 mẫu cùng bộ khung, khác phần đầu và màu nhấn:
   **Mới** (xanh, "Bạn nhận được phiếu giảm giá"), **Cập nhật** (cam, "Phiếu giảm giá đã được cập nhật, xem thông tin mới thay cho thông tin cũ"),
   **Hủy** (đỏ / xám, "đã bị hủy và không còn sử dụng được", không hiện mức giảm, đơn tối thiểu, hiệu lực). Thẻ phiếu gồm mã phiếu, tên phiếu, mức giảm,
   giảm tối đa (phiếu %), đơn tối thiểu, hiệu lực. Xem thử 3 mẫu: mở các file trong thư mục `mail-mau/` bằng trình duyệt. Logo lấy từ `backend/src/main/resources/static/logo.jpg`
   (không có file này thì thư vẫn gửi, chỉ không có logo).
   - **Bật gửi Gmail thật (làm 1 lần):** mở file `backend/src/main/resources/mail.properties` (tự được nạp khi chạy backend), điền 2 dòng `spring.mail.username=` (email Gmail dùng để gửi)
     và `spring.mail.password=` (**Mật khẩu ứng dụng** 16 ký tự, viết liền), lưu file rồi **chạy lại backend**. Cách lấy mật khẩu ứng dụng: bật **Xác minh 2 bước** cho Gmail →
     vào https://myaccount.google.com/apppasswords tạo mật khẩu (KHÔNG dùng mật khẩu đăng nhập Gmail thường, Gmail sẽ từ chối với lỗi 535). Không muốn dùng file thì đặt biến môi trường
     `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` (biến môi trường được ưu tiên hơn file). File này chứa mật khẩu nên đã nằm trong `.gitignore`, không bị đẩy lên GitHub
     (clone về máy mới thì chép `mail.properties.example` thành `mail.properties`).
   - **Kiểm tra cấu hình ngay, không cần tạo phiếu:** chạy backend rồi mở http://localhost:8081/api/phieu-giam-gia/thu-gui-mail trên trình duyệt → backend gửi 1 thư thử đến chính email gửi
     và trả về kết quả; nếu lỗi thì hiện **lý do thật** (vd 535 sai mật khẩu ứng dụng, không nối được smtp.gmail.com:587). Lúc khởi động, log backend cũng ghi 1 dòng
     "Gửi mail: ĐÃ cấu hình ..." hoặc "Gửi mail: CHƯA cấu hình Gmail ...".
   - **Tự kiểm tra đăng nhập Gmail lúc khởi động:** khi đã điền email + mật khẩu, backend thử đăng nhập Gmail ở nền và ghi vào log 1 dòng
     "Gmail: đăng nhập thử THÀNH CÔNG" hoặc "THẤT BẠI" kèm lý do thật (sai mật khẩu ứng dụng, mạng chặn cổng 587, diệt virus chặn SSL...). Mật khẩu ứng dụng dán dạng "abcd efgh ijkl mnop" (có dấu cách)
     cũng được, backend tự bỏ khoảng trắng. Nếu log báo THẤT BẠI thì thư sẽ không gửi được, hãy sửa theo gợi ý trong log rồi chạy lại backend.
   - **Chưa điền email / mật khẩu thì hệ thống KHÔNG gửi mail thật** (chỉ ghi nội dung thư vào log backend, dòng `[MAIL GIẢ LẬP ...]`). Khi đó tạo / sửa phiếu cá nhân vẫn lưu bình thường nhưng giao diện hiện
     **thông báo ĐỎ "Phiếu đã lưu nhưng mail chưa gửi - chưa cấu hình Gmail"** (không còn báo "đã gửi" như trước). Đã cấu hình thì thông báo là "Đang gửi N mail ..." (thư gửi ở luồng nền).
     Khách chưa có email bị bỏ qua (giao diện báo "n khách chưa có email nên không gửi được"). Gửi lỗi (sai mật khẩu, mất mạng) chỉ ghi log mức ERROR kèm lý do và gợi ý sửa, không ảnh hưởng dữ liệu đã lưu.
   - Nút **"Mua sắm ngay"** trong thư Mới / Cập nhật chỉ hiện khi có địa chỉ web: đặt `APP_LINK_CUA_HANG=https://địa-chỉ-web-của-bạn` (phải bắt đầu bằng http:// hoặc https://); để trống thì thư không có nút.
   - Gmail cá nhân giới hạn khoảng **500 thư mỗi ngày**; phiếu tặng cho nhiều khách sẽ gửi từng thư một ở luồng nền.
   - Thử thật: tạo phiếu cá nhân cho một khách có email của chính bạn, kiểm tra thư tới hộp thư chính (không vào Spam), trên điện thoại và máy tính, chế độ sáng / tối.
   - **Báo rõ kết quả gửi trên giao diện (mới 12/10):** form phiếu cá nhân hiện trạng thái Gmail ngay khi chọn "Cá nhân" (chưa cấu hình = khung vàng; đăng nhập thử lỗi = khung đỏ kèm lý do thật, vd 535 sai mật khẩu ứng dụng; sẵn sàng = khung xanh).
     Sau khi lưu, giao diện tự hỏi lại backend vài giây sau và hiện **"Đã gửi 3/3 mail phiếu VCH... thành công"** hoặc thông báo ĐỎ **"Gửi mail lỗi (phiếu vẫn đã lưu)"** kèm lý do (sai mật khẩu, mất mạng...). Lưu phiếu không bao giờ bị hỏng vì lỗi mail.
   - Khi nào gửi: tạo phiếu cá nhân → mọi khách nhận thư Mới. Sửa phiếu còn "Sắp diễn ra": đổi thông tin phiếu (tên, loại giảm, giá trị, giảm tối đa, đơn tối thiểu, ngày bắt đầu / kết thúc) → khách giữ lại nhận thư Cập nhật;
     thêm khách → thư Mới; bỏ khách → thư Hủy; chỉ đổi danh sách khách → khách giữ lại không nhận gì. Phiếu đang / đã diễn ra, phiếu công khai: sửa không gửi thư.
   **Đợt giảm giá chồng nhau:** một biến thể nằm trong nhiều đợt chạy cùng ngày thì mặc định lấy mức giảm CAO NHẤT (vd 10% và 15% → 15%).
   Muốn lấy TRUNG BÌNH CỘNG (10% và 15% → 12,5%) thì đặt `app.giam-gia.che-do-chong-dot=TRUNG_BINH` trong `application.properties`
   (hoặc biến môi trường `GIAM_GIA_CHE_DO=TRUNG_BINH`). Cả giá hiển thị ở danh sách sản phẩm / biến thể lẫn khung lịch giảm giá đều theo cấu hình này.
3. Frontend (cổng 5173): `cd frontend && npm install && npm run dev` (lần đầu cần `npm install` lại vì có thêm thư viện `jsqr` để đọc mã QR căn cước và bộ công cụ kiểm thử), mở http://localhost:5173.
   Kiểm thử giao diện: `cd frontend && npm test` (vitest, không cần backend). Kiểm thử backend: `cd backend && ./mvnw test`
   (cần SQL Server đang chạy cho test `BackendApplicationTests.contextLoads`, các test còn lại không cần CSDL; muốn bỏ qua test đó: `./mvnw test -Dtest='!BackendApplicationTests'`).
   **Địa chỉ Tỉnh/Thành phố → Phường/Xã** (thêm khách hàng, địa chỉ khách hàng, địa chỉ nhân viên) lấy từ API đơn vị hành chính **sau sáp nhập 07/2025**
   `https://provinces.open-api.vn/api/v2` (không còn cấp Quận/Huyện) nên máy chạy giao diện cần có Internet. Địa chỉ cũ đã lưu (có Quận/Huyện) vẫn hiển thị bình thường,
   khi sửa thì giao diện nhắc chọn lại theo danh sách mới.
   **Quét căn cước (thêm nhân viên):** camera chỉ mở được khi truy cập bằng `http://localhost` hoặc https; nếu không có camera thì dùng "Tải ảnh QR" hoặc máy quét mã vạch cầm tay.

## Các trang trên giao diện (http://localhost:5173)
Giao diện quản trị chạy với vai trò **Quản lý** (thấy toàn bộ menu). Theo nhận xét của cô, header đã **bỏ ô phóng to 75%, ô chọn vai trò "Quản lý / Nhân viên" và nút sáng/tối chưa dùng**; header chỉ còn tiêu đề trang + Ca làm việc, Thông báo, Tài khoản; nội dung luôn hiển thị ở mức 75%. Menu theo vai trò (Nhân viên chỉ thấy Bán hàng tại quầy, Quản lý hóa đơn, Quản lý khách hàng) vẫn nằm trong code `MainLayout.vue` (`menuAll`, đổi hằng `role` nếu cần xem thử).

| Đường dẫn | Trang |
|---|---|
| /thong-ke | Thống kê (tổng hợp từ danh sách hóa đơn, chỉ Quản lý): chọn 7 / 30 / 90 ngày, Tất cả hoặc Tự chọn từ ngày - đến ngày; 4 chỉ số (doanh thu, tổng đơn, đơn hoàn thành, đơn hủy) kèm % tăng giảm so với kỳ liền trước; biểu đồ doanh thu theo ngày (tự gom theo tuần / tháng khi khoảng dài); cơ cấu trạng thái, doanh thu theo loại đơn, nhân viên bán tốt nhất, khách mua nhiều nhất, 8 đơn gần đây |
| /hoa-don, /hoa-don/{id} | Danh sách hóa đơn theo thiết kế: bộ lọc 1 hàng (mã hóa đơn, từ ngày - đến ngày, loại đơn Trực tuyến / Tại quầy, tự lọc khi đổi, nút Đặt lại), các tab trạng thái kèm SỐ hóa đơn từng tab (Tất cả, Chờ xác nhận, Đã xác nhận, Chờ giao hàng, Đang giao hàng, Đã giao hàng, Đã hoàn thành, Đã hủy, Hoàn tiền), bảng cột Mã HD / Mã NV / Tên KH / SĐT KH (lấy SĐT hồ sơ khách; khách chưa có SĐT hoặc khách vãng lai thì lấy SĐT người nhận ghi trên hóa đơn, vẫn không có thì hiện —) / Tổng tiền TT / Loại đơn / Ngày tạo / Trạng thái / Hành động, phân trang (5 dòng mỗi trang, không có ô chọn số dòng), xuất Excel toàn bộ danh sách đang lọc; trang chi tiết hóa đơn |
| /san-pham, /san-pham/them, /san-pham/{id}/sua | Sản phẩm: danh sách (hiện giá gốc gạch ngang + giá sau giảm nếu đang có đợt giảm giá), thêm, sửa. **Thuộc tính đổi theo Danh mục**: chọn *Vợt cầu lông* / *Vợt cầu lông người mới* thì có Độ cứng, Điểm cân bằng và biến thể theo Màu sắc + Trọng lượng + Chu vi cán; chọn *Phụ kiện*, *Túi - balo*, *Quần áo thể thao*, *Quả cầu lông* (mọi danh mục không phải vợt) thì ẩn các mục đó, chỉ còn Thương hiệu, Xuất xứ, Chất liệu và biến thể chỉ theo Màu sắc (hệ thống tự gán "Không áp dụng" cho phần ẩn, mục này không hiện trong ô chọn / danh sách thuộc tính). Trang Biến thể sản phẩm cũng theo quy tắc này |
| /san-pham/bien-the | Biến thể sản phẩm: bảng phẳng có lọc theo sản phẩm, màu sắc, **trọng lượng, chu vi cán**, tồn kho, trạng thái; cột **Ảnh** (ảnh theo màu của biến thể); nút **Mã QR** từng biến thể (xem + tải ảnh QR chứa mã biến thể). Cửa sổ "Chi tiết sản phẩm" (nút mắt ở trang Sản phẩm) cũng có bảng biến thể phẳng + lọc màu / trọng lượng / chu vi + QR từng biến thể + ảnh theo màu |
| /san-pham/{danh-muc, thuong-hieu, xuat-xu, chat-lieu, do-cung, diem-can-bang, mau-sac, trong-luong, chu-vi} | 9 bảng thuộc tính (menu "Danh sách thuộc tính") |
| /khach-hang, /khach-hang/them | Khách hàng (kèm địa chỉ). Chọn địa chỉ theo **địa giới sau sáp nhập 07/2025** (API v2 của provinces.open-api.vn: Tỉnh/Thành phố -> Phường/Xã, không còn Quận/Huyện), dùng cho trang Thêm khách hàng và cửa sổ Địa chỉ (cần có mạng internet). Địa chỉ cũ trước sáp nhập vẫn hiện đủ, khi sửa thì nhắc chọn lại. Cửa sổ Địa chỉ: mỗi địa chỉ có 3 nút icon **Xem chi tiết** (xem chỉ đọc) / **Sửa** / **Xóa** |
| /dot-giam-gia, /dot-giam-gia/them, /dot-giam-gia/{id} | Đợt giảm giá: danh sách (lọc, xuất Excel, công tắc bật/tắt, xem chi tiết, nút "Sản phẩm áp dụng" để thêm / gỡ nhanh từng biến thể). Trạng thái hiển thị tính theo khoảng ngày: Sắp diễn ra / Đang diễn ra / Đã kết thúc / Ngừng hoạt động; công tắc luôn hiện, kể cả đợt đã kết thúc: đợt quá ngày kết thúc mà công tắc bật thì hiện "Đã kết thúc", tắt công tắc thì hiện "Ngừng hoạt động" (bật lại thì về "Đã kết thúc"). Khi soạn đợt (thêm / sửa) có khung "Lịch giảm giá khi đợt chồng nhau" xem trước theo dữ liệu đang nhập: biến thể nằm trong đợt khác cùng thời gian thì liệt kê từng khoảng ngày kèm mức Cao nhất / Trung bình, ô xanh là mức đang áp dụng. Trang xem / sửa còn có khung "Lịch giảm giá theo ngày" lấy từ backend (đã lưu), tính cả các đợt khác chồng lên (chế độ MAX hoặc TRUNG_BINH theo cấu hình). Trang thêm / xem / sửa: mã tự sinh dạng ngắn theo số thứ tự (DGG09, nút làm mới), tên, %, từ ngày - đến ngày, mô tả, chọn sản phẩm - biến thể áp dụng (lọc theo màu, trọng lượng, chu vi cán), hộp xác nhận trước khi lưu |
| /phieu-giam-gia, /phieu-giam-gia/them, /phieu-giam-gia/{id}/sua | Phiếu giảm giá: công tắc bật/tắt luôn hiện, kể cả phiếu đã kết thúc (phiếu đã quá ngày kết thúc: công tắc hiện ở trạng thái tắt và bị khóa; nhãn "Đã kết thúc" nếu phiếu đang bật, "Ngưng hoạt động" nếu đã tắt trước đó). Ô tiền có định dạng 1.500.000 ₫. Phiếu cá nhân gửi mail khi tạo (mail phiếu mới) và khi sửa phiếu còn "Sắp diễn ra" (phiếu đã / đang diễn ra thì sửa không gửi mail): đổi thông tin phiếu → khách giữ lại nhận mail cập nhật; thêm khách → mail phiếu mới; bỏ khách → mail hủy; chỉ đổi danh sách khách (thông tin phiếu giữ nguyên) → khách giữ lại không nhận mail |
| /nhan-vien, /nhan-vien/them, /nhan-vien/{id} | Nhân viên. Thêm nhân viên: **mã tự tạo theo họ tên** (chữ cái đầu của mỗi từ + số thứ tự tối thiểu 2 chữ số: Lê Quốc Tuấn -> `LQT01`, người tiếp theo trùng chữ cái đầu -> `LQT02`, quá 99 thì `LQT100`; mã mẫu `NV01`–`NV10` giữ nguyên), hiện ngay khi nhập họ tên; **nhiều địa chỉ** (Tỉnh/Thành phố -> Phường/Xã, chọn 1 địa chỉ chính, không có nút xóa); nút **Quét căn cước** đọc mã QR trên thẻ CCCD (camera / chọn ảnh / máy quét mã vạch) và điền họ tên, ngày sinh, giới tính, số CCCD, đồng thời **tự thêm địa chỉ trên thẻ (địa chỉ cũ có Quận/Huyện) vào danh sách địa chỉ mới**: tỉnh bị sáp nhập tự đổi sang tỉnh mới (vd Thái Bình -> Hưng Yên), Phường/Xã khớp thì dùng tên mới, không khớp thì giữ chữ cũ để bấm sửa chọn lại (Quận/Huyện cũ nằm ở cuối địa chỉ cụ thể); thẻ thiếu số nhà / đường thì mở sẵn ô nhập; quét lại cùng thẻ không thêm trùng. Trạng thái nhân viên: **Còn làm / Đã nghỉ** (nút "Cho nghỉ việc" / "Cho làm lại"). Không còn "Vị trí làm việc" |


**Cập nhật theo nhận xét của cô (self-review 12/10/2026, sửa lần 2 theo 2 video mẫu):**
- **Header:** chỉ còn tiêu đề trang, Ca làm việc, chuông Thông báo, Tài khoản (bỏ ô 75%, ô "Quản lý", nút sáng/tối).
- **Hóa đơn:** có **mã QR hóa đơn** (QR chứa mã hóa đơn, tạo ngay trên trình duyệt bằng thư viện `qrcode`, cần chạy lại `npm install`) thành một khối riêng ở trang chi tiết và ở góc trên bên phải của bản in; trang chi tiết tách khối: Trạng thái đơn hàng, Thông tin đơn hàng (hàng 1) — Khách hàng, Giao hàng, Lịch sử thanh toán, Mã QR (hàng 2) — Danh sách sản phẩm + tổng tiền (hàng 3); nút *In hóa đơn* / *Về danh sách* ở thanh trên cùng; bản in tách thành các phần riêng (Thông tin đơn hàng, Khách hàng, Giao hàng, Sản phẩm, Lịch sử thanh toán, Tổng tiền). **Xuất Excel** danh sách hóa đơn: cột tiền là SỐ (định dạng 3.861.000) để Excel cộng / lọc được.
- **Thêm sản phẩm - biến thể (làm lại theo 2 video mẫu):** chọn màu (và trọng lượng, chu vi cán với vợt) → dòng chữ "Sẽ tạo 4 màu × 3 trọng lượng × 2 chu vi = 24 biến thể" → bấm **Tạo biến thể**. Biến thể **nhóm theo từng màu** (tiêu đề màu có chấm màu, ô tích cả màu, số biến thể của màu; bên dưới là bảng Trọng lượng × Chu vi, Số lượng, Giá bán, Xóa). Có ô **Số lượng mặc định** + **Giá bán mặc định** + nút **Áp dụng** (chỉ áp cho biến thể đang tích), ô **Chọn tất cả biến thể (x/y)**, tích / xóa từng dòng. Nút **"Bảng chung"** chuyển sang một bảng phẳng (cột Màu sắc) nếu muốn xem tất cả trong một bảng — mặc định là nhóm theo màu (đổi hằng `CHE_DO_HIEN_THI_MAC_DINH` trong `frontend/src/utils/bienThe.js` thành `'chung'` nếu muốn mặc định là bảng chung).
  Phần **"Ảnh sản phẩm theo màu"**: mỗi màu một ô "Ảnh sản phẩm màu Đen – Áp dụng cho 6 biến thể cùng màu" + nút Thêm ảnh (tối đa 6 ảnh / màu, ảnh đầu là ảnh chính). Ảnh lưu ở `hinh_anh_san_pham.id_mau_sac`; trang Biến thể, chi tiết sản phẩm, hóa đơn, đợt giảm giá hiện ảnh đúng màu của biến thể (màu chưa có ảnh thì dùng ảnh chung). Trang **Sửa sản phẩm** cũng sửa được ảnh theo màu cho các màu sản phẩm đã có biến thể.
  **Giá bán** có định dạng tiền (1.500.000 ₫); giá trống / 0 / dưới 1.000 ₫ hoặc số lượng sai thì báo "Có N biến thể chưa hợp lệ…" và viền đỏ đúng ô. Danh mục không phải vợt: chỉ chọn màu (mỗi màu 1 biến thể), trọng lượng / chu vi tự gán "Không áp dụng".
- **Phiếu giảm giá:** các ô tiền (Giá trị giảm theo số tiền, Giảm tối đa, Đơn tối thiểu) có định dạng 1.500.000 ₫. Phiếu **cá nhân**: số lượng tự bằng số khách đã chọn; danh sách khách hiện email (khách chưa có email có nhãn "Chưa có email"); dòng báo "Khi lưu, hệ thống gửi mail tặng phiếu cho N khách"; khung trạng thái Gmail (chưa cấu hình / đăng nhập lỗi + lý do / sẵn sàng); sau khi lưu tự báo **đã gửi** hay **gửi lỗi + lý do** (3 loại mail Mới / Cập nhật / Hủy như mục Gửi mail ở trên). Sửa phiếu đã bắt đầu thì form báo trước là sẽ không gửi mail.
- **Đợt giảm giá (làm lại phần chọn):** một ô tìm kiếm (lọc ngay khi gõ), tích sản phẩm = chọn hết biến thể, bấm "+" rồi bấm nhãn biến thể (Màu · Trọng lượng · Chu vi) để chọn / bỏ từng biến thể. Thẻ **"Tổng kết đợt giảm giá"** (mức giảm, số sản phẩm, số biến thể, ngày bắt đầu, ngày kết thúc + số ngày, tổng giá trước / sau giảm). Bảng **"Sản phẩm & biến thể đã chọn áp dụng"**: mỗi dòng một biến thể (Ảnh, Sản phẩm, Biến thể, Giá bán, Giá sau giảm, Xóa), lọc theo màu sắc / trọng lượng / chu vi, nút "Gỡ N biến thể đang lọc". Giá sau giảm dùng chung một công thức làm tròn tới đồng (`tinhGiaSauGiam`) cho danh sách sản phẩm, biến thể và trang soạn đợt.

## API
| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| GET | /api/hoa-don?maHoaDon&tuNgay&denNgay&loaiDon&trangThai&page&size | Danh sách hóa đơn (lọc + phân trang) |
| GET | /api/hoa-don/dem-trang-thai?maHoaDon&tuNgay&denNgay&loaiDon | Số hóa đơn theo từng trạng thái `{ tatCa, 0..6 }` (áp cùng bộ lọc, trừ trạng thái) để hiện số trên các tab |
| GET | /api/hoa-don/{id} | Chi tiết hóa đơn (khách, giao hàng, sản phẩm, thanh toán, lịch sử) |
| PUT | /api/hoa-don/{id}/trang-thai | Chuyển sang trạng thái kế tiếp hoặc hủy đơn (bắt buộc ghi lý do hủy). Không xóa dữ liệu |
| GET | /api/phieu-giam-gia | Danh sách phiếu |
| GET | /api/phieu-giam-gia/{id} | Chi tiết (kèm khách hàng được tặng) |
| GET | /api/phieu-giam-gia/khach-hang | Khách hàng có thể chọn khi tặng phiếu cá nhân |
| POST / PUT | /api/phieu-giam-gia[/{id}] | Tạo / sửa. Kết quả có thêm `ketQuaMail` (câu tóm tắt số mail đã xếp gửi) |
| GET | /api/phieu-giam-gia/trang-thai-mail | Trạng thái gửi mail `{ daCauHinh, dangNhap: CHUA_CAU_HINH / DANG_KIEM_TRA / THANH_CONG / THAT_BAI, lyDo }` (kết quả đăng nhập thử Gmail lúc khởi động) |
| GET | /api/phieu-giam-gia/{id}/ket-qua-mail | Kết quả lô mail của lần lưu phiếu gần nhất `{ tong, daGui, loi, giaLap, xong, lyDo }` (giao diện hỏi lại sau khi lưu để báo đã gửi / gửi lỗi) |
| GET | /api/phieu-giam-gia/thu-gui-mail | Gửi 1 thư thử đến chính email gửi và trả về lỗi thật nếu có |
| PUT | /api/phieu-giam-gia/{id}/trang-thai | Ẩn / hiện phiếu (không xóa dữ liệu) |
| GET | /api/nhan-vien, /api/nhan-vien/{id} | Danh sách / chi tiết nhân viên |
| POST / PUT | /api/nhan-vien[/{id}] | Thêm: nhập đủ (mật khẩu băm BCrypt, không trả về); `canCuoc` (số căn cước 12 số, không trùng, lưu ở cột `can_cuoc_cong_dan`), `diaChis[]` (ít nhất 1 địa chỉ); **mã nhân viên do backend tự sinh theo họ tên** (chữ cái đầu của mỗi từ + số thứ tự tối thiểu 2 chữ số, vd `LQT01`). Sửa: chỉ đổi họ tên, giới tính, ngày sinh, SĐT, căn cước, địa chỉ, vai trò; mã, email, ngày vào làm, mật khẩu giữ nguyên |
| PUT | /api/nhan-vien/{id}/trang-thai | Ẩn / hiện nhân viên (không xóa dữ liệu) |
| GET | /api/vai-tro | Danh sách vai trò |
| GET | /api/khach-hang?search=, /api/khach-hang/{id} | Danh sách (tìm theo mã, tên, SĐT, email) / chi tiết khách hàng (kèm danh sách địa chỉ) |
| POST / PUT | /api/khach-hang[/{id}] | Thêm (kèm địa chỉ mặc định nếu gửi tinhThanh, phuongXa, diaChiCuThe; `quanHuyen` không còn dùng cho địa chỉ mới vì đã sáp nhập) / sửa. Mật khẩu tạm được băm, không trả về |
| PUT | /api/khach-hang/{id}/trang-thai | Bật / tắt hoạt động khách hàng |
| GET | /api/dia-chi/khach-hang/{idKhachHang} | Địa chỉ của khách hàng |
| POST / PUT / DELETE | /api/dia-chi/khach-hang/{idKhachHang}, /api/dia-chi/{id} | Thêm / sửa / xóa địa chỉ (luôn có đúng 1 địa chỉ mặc định) |
| GET | /api/dot-giam-gia | Danh sách đợt giảm giá, đợt mới thêm nằm trên cùng |
| GET | /api/dot-giam-gia/ma-moi | Mã đợt do hệ thống sinh (DGG + số thứ tự, vd DGG09, DGG100), không trùng mã đã có |
| GET | /api/dot-giam-gia/{id} | Chi tiết một đợt giảm giá |
| POST / PUT | /api/dot-giam-gia[/{id}] | Thêm / sửa đợt giảm giá. Body: `maDot, tenDot, phanTramGiamDot, ngayBatDau, ngayKetThuc, moTa, idBienThe[]` (nhận cả tên cũ `idBienThes`; ngày dạng `yyyy-MM-dd` hoặc `yyyy-MM-ddTHH:mm:ss`). Lưu đợt và đồng bộ biến thể áp dụng trong một lần (thêm bắt buộc có ít nhất 1 biến thể đang bán; sửa không gửi `idBienThe` thì giữ nguyên biến thể; sửa không đổi mã đợt). Đổi % của đợt thì áp % mới cho mọi biến thể; không đổi % thì giữ mức giảm riêng của từng biến thể |
| PUT | /api/dot-giam-gia/{id}/trang-thai | Bật / tắt hoạt động đợt giảm giá (1: đang hoạt động, 0: ngừng hoạt động). Đợt đã qua ngày kết thúc thì không đổi được (409) |
| GET | /api/dot-giam-gia/{id}/lich-giam-gia | Lịch giảm giá theo khoảng ngày của từng biến thể trong đợt, có tính các đợt đang bật chồng lên (mỗi đoạn: `tuNgay, denNgay, phanTramApDung` = mức áp dụng theo cấu hình MAX / TRUNG_BINH, `phanTramCaoNhat`, `phanTramTrungBinh`, `cacDot`, `giaSauGiam`) |
| GET | /api/dot-giam-gia/ap-dung | Mức giảm của từng biến thể theo các đợt đang bật mà chưa kết thúc `{ chinhSach, muc: [{ idBienThe, idDot, maDot, tenDot, phanTram, ngayBatDau, ngayKetThuc }] }`; trang soạn đợt dùng để xem trước lịch chồng đợt |
| GET | /api/dot-giam-gia/ap-dung-hom-nay | Mức giảm đang áp dụng hôm nay của từng biến thể `{ idBienThe: phanTram }` (nhiều đợt chồng nhau thì gộp theo MAX / TRUNG_BINH; dùng cho giá gạch ngang ở danh sách sản phẩm / biến thể) |
| GET | /api/giam-gia/ap-dung?ngay=yyyy-MM-dd | Mức giảm của từng biến thể tại một ngày bất kỳ (bỏ `ngay` thì lấy hôm nay): `{ cheDo, ngay, phanTram: { idBienThe: % } }` |
| GET | /api/giam-gia/bien-the/{id}/lich | Lịch giảm giá theo khoảng ngày của MỘT biến thể (mọi đợt đang bật chứa biến thể đó) |
| GET / POST / DELETE | /api/dot-giam-gia-chi-tiet/campaign/{id}, /api/dot-giam-gia-chi-tiet/{id} | Biến thể áp dụng trong đợt giảm giá (chọn từ /api/bien-the-san-pham) |
| GET | /api/san-pham, /api/san-pham/{id} | Danh sách / chi tiết sản phẩm (kèm ảnh chính, ảnh phụ = ảnh chung; `anhTheoMau: [{ idMauSac, anh: [...] }]`; `anhDaiDien` = ảnh chính chung, không có thì ảnh chính của màu đầu tiên) |
| POST / PUT | /api/san-pham[/{id}] | Thêm / sửa sản phẩm (sửa không đổi mã). Gửi kèm `anhTheoMau: [{ idMauSac, anh: [url...] }]` để lưu ảnh theo màu (tối đa 6 ảnh / màu; không gửi = giữ nguyên; gửi `anh: []` = bỏ ảnh của màu đó). Thêm có thể gửi kèm `bienThes[]` (màu, trọng lượng, chu vi, giá bán, tồn; mã biến thể tự sinh) để lưu sản phẩm + biến thể trong một lần. Sản phẩm đã tồn tại (cùng tên + danh mục + thương hiệu) → trả 409 mã `SAN_PHAM_DA_TON_TAI`; gửi lại với `xacNhanCapNhat = true` để cập nhật sản phẩm cũ (biến thể chưa có thì thêm, đã có thì cập nhật giá + tồn) |
| PUT | /api/san-pham/{id}/trang-thai | Ngưng / kích hoạt bán sản phẩm (không xóa dữ liệu) |
| GET | /api/bien-the-san-pham[/{id}] | Danh sách / chi tiết biến thể (bảng san_pham_chi_tiet), kèm `hinhAnh` = ảnh chính của màu biến thể (không có thì ảnh chung của sản phẩm) |
| POST / PUT | /api/bien-the-san-pham[/{id}] | Thêm: nếu sản phẩm đã có biến thể cùng màu + trọng lượng + chu vi → 409 mã `BIEN_THE_DA_TON_TAI`, gửi lại với `xacNhanCapNhat = true` để cập nhật giá + tồn. Thêm / sửa biến thể (khi sửa được đổi sản phẩm, vd SP08 → SP07; mã không sửa tay, chỉ phần đầu mã tự đổi theo sản phẩm mới) |
| PUT | /api/bien-the-san-pham/{id}/trang-thai | Ngưng / kích hoạt biến thể |
| GET / POST | /api/thuoc-tinh/{loai} | Danh sách / thêm thuộc tính. `loai` = danh-muc, thuong-hieu, xuat-xu, chat-lieu, do-cung, diem-can-bang, mau-sac, trong-luong, chu-vi |
| PUT | /api/thuoc-tinh/{loai}/{id} | Sửa tên thuộc tính (mã giữ nguyên) |
| PUT | /api/thuoc-tinh/{loai}/{id}/trang-thai | Ngưng / kích hoạt thuộc tính |

Lưu ý: hệ thống không xóa cứng dữ liệu. Nút "Ẩn" chỉ đổi cột `trang_thai`, có thể "Hiện lại" bất cứ lúc nào.

## Ghi chú cập nhật (chất liệu theo danh mục, giảm giá biến thể, hóa đơn, địa chỉ khách hàng)

- **Chất liệu theo danh mục**: bảng `chat_lieu` có thêm cột `nhom_ap_dung` (VOT, QUAN_AO, TUI_BALO, PHU_KIEN, QUA_CAU). Form sản phẩm chỉ hiện chất liệu hợp danh mục đã chọn
  (Quần áo thể thao -> Cotton, Polyester, Thun lạnh...; Vợt -> Carbon, Graphite, Nhôm, Titanium) và backend cũng chặn lưu sai. Quản lý chất liệu có thêm phần chọn "Dùng cho danh mục".
  Cột này nằm trong `database/SD_17_du_lieu_va_nang_cap.sql` (vì `SD_17.sql` giữ nguyên); khi chạy backend, `SchemaUpgradeRunner` cũng tự thêm cột + chất liệu mới.
- **Cột "Giảm giá" (trang Biến thể sản phẩm)** = % giảm của đợt giảm giá **đang diễn ra hôm nay** mà biến thể nằm trong đó. Thêm biến thể ở Đợt giảm giá -> "Sản phẩm áp dụng":
  đợt đang diễn ra thì hiện ngay; đợt sắp diễn ra hiện "Sắp giảm -x% từ ngày..."; đợt ngừng hoạt động / đã kết thúc thì không giảm. Đợt đã kết thúc thì không thêm được biến thể mới;
  biến thể đang ngừng bán không áp dụng giảm giá được (backend chặn, giao diện báo rõ).
- **Hóa đơn**: cột SĐT KH lấy SĐT hồ sơ khách, không có thì lấy SĐT người nhận ghi trên hóa đơn (kể cả khách vãng lai); khách vãng lai hiện tên người nhận nếu có.
- **Thêm sản phẩm**: nút tạo biến thể tên là "Tạo biến thể".

# Chạy project FootStyle (GĐ3: đăng nhập / phân quyền, bán tại quầy, giao ca, bán hàng online, chat, thống kê + các trang quản lý GĐ2)

> **Cập nhật 25/10/2026 (GĐ3):** thư mục `database/` hiện có 4 file, chạy trong SSMS (mở file → Execute / F5):
> 1. `database/SD_17.sql` — tạo database `shop_cau_long_footstyle` và toàn bộ bảng theo ERD (đã gồm bảng GĐ3: `ca_lam_viec`, `lich_lam_viec`, `giao_ca`, `hoi_thoai`, `thanh_vien_hoi_thoai`, `tin_nhan`). Chỉ chạy **một lần** cho CSDL mới.
> 2. `database/SD_17_du_lieu_va_nang_cap.sql` — dữ liệu mẫu + nâng cấp (chạy **sau** file 1; chạy lại nhiều lần không lỗi). Gồm tài khoản nhân viên, sản phẩm, phiếu / đợt giảm giá, hóa đơn mẫu, 3 ca làm việc 07:00 - 12:00 / 12:00 - 17:00 / 17:00 - 22:00.
> 3. `database/SD_17_giai_doan_3.sql` — chỉ dùng cho **CSDL tạo từ bản cũ** (chưa có bảng GĐ3): thêm bảng / cột GĐ3, ca mặc định và lịch làm mẫu, không xóa dữ liệu cũ.
> 4. `database/SD_17_footstyle_full.sql` — bản gộp đầy đủ (cấu trúc + dữ liệu demo) để dựng nhanh trên máy mới **thay cho** file 1 + 2.
>
> Khởi động lại backend cũng tự bổ sung bảng / cột còn thiếu. **Không sửa cấu trúc SQL** (giữ đúng ERD). Phần chữ bên dưới mục này là ghi chép của GĐ2, tên file SQL ở đó là bản cũ.

1. SQL Server — *(ghi chép GĐ2, 16/10/2026; tên file nay đã đổi như khung trên)* gộp còn đúng 2 file trong thư mục `database/`, chạy lần lượt trong SSMS (mở file → Execute / F5):
   - `database/SD_17_1_cau_truc.sql` — **chỉ CẤU TRÚC**: tạo database `shop_cau_long_footstyle`, toàn bộ 29 bảng, khóa chính, khóa ngoại, UNIQUE, chỉ mục chống trùng (email / SĐT / căn cước nhân viên, email / SĐT khách hàng, biến thể không trùng màu - trọng lượng - chu vi). **Không có dữ liệu.**
     Gồm PHẦN 1 – 4 tạo bảng (theo thứ tự khóa ngoại), PHẦN 5 thêm cột còn thiếu cho CSDL tạo từ bản cũ (`can_cuoc_cong_dan`, `anh_dai_dien`, `nhom_ap_dung`, `id_mau_sac`...), PHẦN 6 chỉ mục.
   - `database/SD_17_2_du_lieu.sql` — **chỉ DỮ LIỆU** (không tạo / sửa bảng): PHẦN 0 sửa dữ liệu cũ (rút gọn mã `HD000001` → `HD01`, chép số căn cước cột cũ), PHẦN 1 vai trò + 10 nhân viên + 6 khách hàng (+ địa chỉ), PHẦN 2 sản phẩm (9 bảng thuộc tính, mục "Không áp dụng", 6 sản phẩm, 14 biến thể), PHẦN 3 11 phiếu giảm giá + 8 đợt giảm giá, PHẦN 4 sửa dữ liệu mẫu cũ (giày → vợt, đơn tại quầy còn phí vận chuyển), PHẦN 5 phương thức thanh toán + hóa đơn mẫu HD01 – HD18 và **HD19 "Giao hàng thất bại"**, PHẦN 6 thêm **25 hóa đơn Chờ xác nhận + 20 hóa đơn Đang giao hàng** (ngẫu nhiên từ dữ liệu đang có, chỉ thêm 1 lần; đặt `@them_lai = 1` nếu muốn thêm lô nữa, cuối file có khối lệnh đang comment để xóa lô này). Cuối file in bảng đếm số dòng các bảng chính.
   **Cả 2 file dùng được cho CSDL mới lẫn CSDL đang có dữ liệu** và chạy lại nhiều lần không lỗi: cái gì đã có thì bỏ qua, chỉ thêm phần còn thiếu, không xóa dữ liệu của bạn. Vì vậy các file cũ (`SD_17.sql`, `SD_17_du_lieu_va_nang_cap.sql`, `SD_17_them_anh_dai_dien.sql`, `SD_17_them_hoa_don_cho_xac_nhan_dang_giao.sql`) đã được gộp vào 2 file này và bỏ đi. Test `FileSqlTest` giữ đúng quy tắc: file cấu trúc không có lệnh dữ liệu, file dữ liệu không tạo / sửa bảng, mọi cột của entity đều có trong bảng.
   Mật khẩu nhân viên mẫu: `Footstyle@123`.
   **Đã có CSDL từ trước?** Cứ chạy lần lượt 2 file như trên (database đã tồn tại thì file 1 không tạo lại), hoặc chỉ cần khởi động lại backend (backend tự bổ sung bảng / cột / chỉ mục còn thiếu, ghi dòng "Đã kiểm tra / nâng cấp cấu trúc CSDL" vào log; riêng dữ liệu mẫu mới như HD19 thì phải chạy file 2).
   Phần nâng cấp bổ sung cho CSDL cũ: 2 cột `ngay_tao`, `ngay_cap_nhat` của `dia_chi_khach_hang`; cột `anh_dai_dien` của `khach_hang`; cột `can_cuoc_cong_dan` cho `nhan_vien` (CSDL cũ đang có cột `cccd` thì số đã nhập được CHÉP sang, cột `cccd` cũ vẫn giữ); bảng `dia_chi_nhan_vien` (địa chỉ cũ ở cột `nhan_vien.dia_chi` thành địa chỉ chính); cột `mo_ta` cho `dot_giam_gia`; cột `nhom_ap_dung` cho `chat_lieu`; cột `id_mau_sac` cho `hinh_anh_san_pham` (ảnh cũ có `id_mau_sac = NULL` = ảnh chung, vẫn hiện như trước).
   Chỉ mục UNIQUE `ux_khach_hang_email`, `ux_khach_hang_sdt` chỉ tạo được khi dữ liệu hiện có chưa trùng; nếu đang trùng thì file 1 in danh sách trùng ra để sửa rồi chạy lại.
   **Thuộc tính theo danh mục sản phẩm:** CSDL cần 4 mục "Không áp dụng" (`DC00`, `CB00`, `TL00`, `CV00`) cho độ cứng, điểm cân bằng, trọng lượng, chu vi. File `SD_17_2_du_lieu.sql` đã có sẵn; CSDL cũ thì khởi động lại backend (tự bổ sung) hoặc chạy file này.
   **Quy tắc mã (HD01, NV01, SP01, DGG01...):** mọi mã tự cấp = tiền tố + số thứ tự tối thiểu 2 chữ số, KHÔNG đệm thêm số 0; quá 99 thì viết thẳng (HD99 -> HD100). Mã nhân viên = chữ cái đầu của họ tên + số thứ tự (Lê Quốc Tuấn -> `LQT01`). **Mã cũ nhiều số 0** (`HD000001`, `NV0001`, `SP001`, `DGG001`, `SP001-DEN-4U-G5`...) được đổi sang mã gọn tự động khi khởi động lại backend (log: "Đã rút gọn N mã cũ nhiều số 0"), hoặc chạy PHẦN 0 của `SD_17_2_du_lieu.sql`. Chỉ đổi cột mã, không mất liên kết dữ liệu (các bảng nối nhau bằng id); chạy lại nhiều lần vẫn an toàn. Mã phiếu giảm giá do người dùng đặt / sinh ngẫu nhiên (`VCH...`) không bị đổi.
   **Gặp lỗi "Invalid column name 'can_cuoc_cong_dan'" / "Invalid column name 'id_mau_sac'" / "Invalid object name 'dia_chi_nhan_vien'" / trang Hóa đơn báo "Không tải được danh sách hóa đơn"?** Nghĩa là CSDL được tạo từ bản SQL cũ.
   Cách xử lý: khởi động lại backend (backend tự bổ sung phần còn thiếu), hoặc chạy `database/SD_17_1_cau_truc.sql` rồi `database/SD_17_2_du_lieu.sql`.
2. Backend (JDK 17 trở lên, cổng 8081; xem mục "Phiên bản được hỗ trợ" bên dưới): sửa user/mật khẩu SQL Server trong `backend/src/main/resources/application.properties`
   (hoặc đặt biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), rồi `cd backend && ./mvnw spring-boot:run`.
   **Chạy thêm một backend khi backend đã chạy sẵn / báo "Port 8081 was already in use" / backend "tự nhiên biến mất"?** Từ 13/10/2026 backend **không bao giờ tự tắt bản đang chạy**
   (bản trước từng tự tắt bản cũ khi bạn chạy thêm một bản, nên backend đang chạy bị tắt ngầm). Khi khởi động, nếu cổng 8081 đang bị chính backend FootStyle giữ
   (tiến trình Java và trả lời đúng API `/api/phieu-giam-gia/trang-thai-mail`) thì bản đang chạy được **giữ nguyên**, bản chạy thêm in
   `[FootStyle] Backend FootStyle DA CHAY SAN o cong 8081 (PID ..., khoi dong luc ...)` rồi thoát êm (mã thoát 0, **không phải lỗi**; web vẫn gọi được http://localhost:8081).
   Muốn chạy lại bản mới (vd vừa sửa code Java): bấm **Stop** ở bản cũ (IntelliJ, hoặc Ctrl+C ở cửa sổ terminal) rồi chạy lại; hoặc chạy `giai-phong-cong-8081.bat`;
   hoặc cho bản mới tự thay bản cũ: `$env:FOOTSTYLE_THAY_BAN_CU='true'; ./mvnw spring-boot:run` (hoặc `-Dfootstyle.thay-ban-cu=true`).
   Chương trình khác chiếm cổng thì **không** bị tắt, backend in hướng dẫn rồi dừng (mã thoát 1): tắt chương trình đó (hoặc chạy `giai-phong-cong-8081.bat`),
   hoặc chạy cổng khác `$env:SERVER_PORT=8082; ./mvnw spring-boot:run` kèm `$env:VITE_API_TARGET='http://localhost:8082'` cho frontend.
   Không muốn kiểm tra cổng: chạy với `-Dfootstyle.tu-giai-phong-cong=false` hoặc biến môi trường `FOOTSTYLE_TU_GIAI_PHONG_CONG=false`.
   Code: `backend/src/main/java/com/footstyle/demo/config/GiaiPhongCong.java` (test: `GiaiPhongCongTest`).
   **Tự biết backend tắt vì sao:** tắt bình thường (Stop, Ctrl+C) thì log in dòng `[FootStyle] Backend (PID ...) da tat luc ...`. Backend biến mất mà **không có** dòng này = bị tắt
   cưỡng bức từ bên ngoài (đóng tab / cửa sổ terminal đang chạy nó, đóng IntelliJ, Task Manager, `giai-phong-cong-8081.bat`, hoặc bản khác bật `FOOTSTYLE_THAY_BAN_CU=true`).
   **DevTools đã tắt tự khởi động lại** (`spring.devtools.restart.enabled=false`, `spring.devtools.livereload.enabled=false` trong `application.properties`): trước đây mỗi lần IntelliJ Build / Run hoặc
   `mvn compile` làm đổi `backend/target/classes` là backend tự khởi động lại, mất kết nối vài giây (đang build dở có thể không lên lại). Giờ sửa code Java xong hãy Stop rồi Run lại;
   muốn bật lại tự khởi động lại thì đổi `spring.devtools.restart.enabled=true`.
   Log tiếng Việt trong cửa sổ PowerShell bị lỗi font (vd `Ä'Äƒng nháº­p`) chỉ là do cửa sổ chưa ở chế độ UTF-8: gõ `chcp 65001` trước khi chạy là hiện đúng dấu.
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
   Muốn lấy TRUNG BÌNH CỘNG (10% và 15% → 12,5%) thì đặt `app.giam-gia.che-do-chong-dot=TRUNG_BINH`, muốn CỘNG DỒN (10% và 15% → 25%, tối đa 100%) thì đặt `CONG_DON`, trong `application.properties`
   (hoặc biến môi trường `GIAM_GIA_CHE_DO=TRUNG_BINH` / `CONG_DON`). Mỗi **đơn hàng chỉ dùng 1 phiếu giảm giá**, đợt giảm giảm trước rồi phiếu mới giảm trên số tiền còn lại. Cả giá hiển thị ở danh sách sản phẩm / biến thể lẫn khung lịch giảm giá đều theo cấu hình này.
3. Frontend (cổng 5173): `cd frontend && npm install && npm run dev` (lần đầu cần `npm install` lại vì có thêm thư viện `jsqr` để đọc mã QR căn cước và bộ công cụ kiểm thử), mở http://localhost:5173.
   **Lưu ý:** `./mvnw spring-boot:run` chiếm luôn cửa sổ terminal cho tới khi backend tắt — bấm Ctrl+C ở đó là **tắt backend**. Vì vậy backend và frontend phải
   chạy ở **2 terminal khác nhau**: terminal 1 `cd backend` → `./mvnw spring-boot:run` (để nguyên); bấm dấu **+** ở khung Terminal của IntelliJ mở terminal 2 →
   `cd frontend` → `npm install` → `npm run dev`.
   **Chạy nhanh bằng 1 lệnh (Windows):** ở thư mục gốc project gõ `.\chay-du-an.bat` (hoặc bấm đúp file). Script mở backend ở cửa sổ riêng "FootStyle - Backend",
   chờ backend trả lời API rồi chạy `npm install` + `npm run dev` ngay trong terminal đang gõ. Ctrl+C ở terminal này chỉ tắt frontend; đóng cửa sổ "FootStyle - Backend"
   mới tắt backend. Backend đang chạy sẵn thì script dùng luôn, không mở thêm bản thứ hai.
   **macOS / Linux:** `./chay-du-an.sh` (báo "Permission denied" thì gõ `bash chay-du-an.sh`) làm đúng như bản Windows, chỉ khác là backend chạy nền và ghi log vào `backend/backend.log`
   (script in sẵn lệnh `kill <PID>` để tắt backend; Ctrl+C ở terminal chỉ tắt frontend).
   Cả hai script đều **kiểm tra Node.js, Java và SQL Server trước khi chạy** và nói rõ thiếu / quá cũ cái nào, cần làm gì (chi tiết ở mục "Phiên bản được hỗ trợ").
   Kiểm thử giao diện: `cd frontend && npm test` (vitest, không cần backend). Kiểm thử backend: `cd backend && ./mvnw test`
   (cần SQL Server đang chạy cho test `BackendApplicationTests.contextLoads`, các test còn lại không cần CSDL; muốn bỏ qua test đó: `./mvnw test -Dtest='!BackendApplicationTests'`).
   **Kết quả lần kiểm cuối (25/10/2026, GĐ3):** giao diện **857 test / 49 file đạt**, `npm run build` thành công; backend **619 test**, đạt hết trừ 2 test của `CuaHangServiceTest`
   (loại test chạy thẳng vào CSDL thật: báo lỗi khi biến thể đầu tiên trong CSDL hết hàng, nhập thêm tồn kho là đạt).
   *Kết quả GĐ2 (16/10/2026):* giao diện **759 test / 36 file đạt** và `npm run build` thành công; backend **449 test đạt** (38 nhóm test, đã loại `BackendApplicationTests` vì nó khởi động cả ứng dụng và cần SQL Server; khi bạn chạy `./mvnw test` thật thì test đó cũng được chạy). Lưu ý: môi trường làm bài không tải được thư viện từ Maven Central nên phần Java được biên dịch và chạy test bằng bộ giả lập Spring + JUnit; bạn hãy chạy `./mvnw test` một lần trên máy có Internet để chắc chắn.
   **Địa chỉ Tỉnh/Thành phố → Phường/Xã** (thêm khách hàng, địa chỉ khách hàng, địa chỉ nhân viên) lấy từ API đơn vị hành chính **sau sáp nhập 07/2025**
   `https://provinces.open-api.vn/api/v2` (không còn cấp Quận/Huyện) nên máy chạy giao diện cần có Internet. Địa chỉ cũ đã lưu (có Quận/Huyện) vẫn hiển thị bình thường,
   khi sửa thì giao diện nhắc chọn lại theo danh sách mới.
   **Quét căn cước (thêm nhân viên):** camera chỉ mở được khi truy cập bằng `http://localhost` hoặc https; nếu không có camera thì dùng "Tải ảnh QR" hoặc máy quét mã vạch cầm tay.

## Phiên bản được hỗ trợ (chạy trên máy khác)
Khi chạy bằng `chay-du-an.bat` (Windows) hoặc `./chay-du-an.sh` (macOS / Linux), script `scripts/kiem-tra-moi-truong.cjs` tự kiểm tra máy trước và in từng dòng `[OK]` / `[CHU Y]` / `[LOI]` kèm cách sửa
(chạy riêng được: `node scripts/kiem-tra-moi-truong.cjs`). Chỉ `[LOI]` mới chặn chạy; `[CHU Y]` (vd SQL Server chưa bật) chỉ nhắc. Muốn bỏ qua hẳn bước này: đặt biến môi trường `FOOTSTYLE_BO_QUA_KIEM_TRA=1`.

| Thành phần | Yêu cầu | Đã kiểm chứng ở môi trường làm bài |
|---|---|---|
| **Java** | **JDK** (không phải JRE) **17 trở lên**. Khuyên dùng JDK 21 (LTS). | Biên dịch với `--release 17`; toàn bộ test backend đạt trên **JDK 17 và JDK 21**. JDK 22 trở lên: `pom.xml` đã nâng Lombok (1.18.48) và Byte Buddy (1.17.8) để biên dịch được trên JDK mới, **nhưng chưa thử thật** vì không có các JDK đó. |
| **Spring Boot** | 3.3.13 (bản vá cuối của nhánh 3.3, chính thức hỗ trợ Java 17 – 23). | Không tải được Maven Central ở môi trường làm bài nên **chưa chạy `./mvnw` thật** và chưa khởi động Spring thật; hãy chạy `./mvnw test` một lần trên máy có Internet. |
| **Node.js** | **20.19 trở lên** hoặc **22.12 trở lên** (Vite 8 yêu cầu). Muốn chạy `npm test` cần **22.12 trở lên**; khuyên dùng Node 24 LTS (`frontend/.nvmrc`). Node 18, 21 và 22.0 – 22.11 **không** chạy được. | Test giao diện đạt trên Node 22.12, 22.22, 24.15, 26.0. Node 20.19: `npm run build` và `npm run dev` chạy được (bộ test thì không chạy được, vì vitest 5 / jsdom 30 cần Node 22.12 trở lên). |
| **SQL Server** | 2016 trở lên (2017 / 2019 / 2022, bản Express cũng được). Cần bật **TCP/IP cổng 1433** (Express mặc định tắt: mở "SQL Server Configuration Manager" → Network Configuration → TCP/IP → Enabled → khởi động lại dịch vụ) và cho phép đăng nhập `sa` (Mixed Mode). | **Chưa chạy thật** vì môi trường làm bài không có SQL Server; câu lệnh SQL chỉ dùng T-SQL cơ bản (không dùng hàm mới của 2017+). SQL Server quá cũ chưa vá TLS 1.2 kết hợp JDK 17+ có thể báo lỗi SSL: thêm `encrypt=false` vào `DB_URL`. |
| **Trình duyệt** | Chrome / Edge / Firefox / Safari bản mới (khoảng 2 năm gần đây); không hỗ trợ Internet Explorer. Quét căn cước bằng camera chỉ mở được ở `http://localhost` hoặc https. | Chỉ kiểm bằng jsdom và `vite build`; chưa mở bằng Firefox / Safari thật. |
| **Hệ điều hành** | Windows 10 / 11, macOS, Linux. | Linux; script `.bat` và `.sh` chưa chạy trên Windows / macOS thật. |

**Những chỗ hay lỗi khi đổi máy đã được xử lý trong code:**
- **Ngôn ngữ / vùng của hệ điều hành:** mã tự sinh (HD01, NV03...), tiền (`1.250.000 ₫`), chữ hoa / thường không còn phụ thuộc vùng của máy (trước đây máy tiếng Ả Rập / Ba Tư / Thái có thể sinh mã `NV٠٣` bằng chữ số của nước đó, máy tiếng Thổ Nhĩ Kỳ đổi `i` thành `İ`). Backend dùng `Locale.ROOT` / `vi-VN` cố định. Test canh giữ: `KhongPhuThuocMayTest` (báo đỏ nếu ai thêm `toUpperCase()`, `String.format(...)`... không chỉ định Locale) và `MaUtilTest`.
- **Bảng mã:** mã nguồn UTF-8 (`project.build.sourceEncoding` trong `pom.xml`); test backend đạt cả khi máy dùng bảng mã ASCII / windows-1252.
- **Múi giờ:** backend và giao diện dùng múi giờ của máy (cố ý không ép, để giờ hiển thị luôn khớp giờ lưu khi chạy cùng một máy). Test đạt ở các múi giờ từ UTC−8 đến UTC+14 (Los Angeles, Kiritimati, Kolkata, Auckland, Hồ Chí Minh, London...).
- **Xuống dòng:** `.gitattributes` giữ file `.bat` / `.cmd` ở CRLF, file khác ở LF; test cũng đạt khi toàn bộ project bị chuyển sang CRLF.
- **JDK mới hơn thư viện biết:** Hibernate (qua Byte Buddy) báo "Java NN is not supported" khi JDK mới hơn thư viện; `BackendApplication` bật sẵn `net.bytebuddy.experimental` khi khởi động và `pom.xml` bật cho lúc chạy test (đặt `-Dnet.bytebuddy.experimental=false` khi chạy backend nếu muốn tắt).
- **Máy chậm / nặng tải:** các test giao diện đợi mã QR bằng `vi.waitFor` thay vì ngủ cố định 30 ms nên không còn đỏ ngẫu nhiên trên máy chậm.
- **macOS / Linux:** `backend/mvnw` đã có quyền chạy; giải nén bằng công cụ làm mất quyền thì gõ `chmod +x backend/mvnw` (hoặc dùng `sh ./mvnw ...`, `chay-du-an.sh` đã tự dùng cách này).

**Lỗi thường gặp khi đổi máy:**
- `UnsupportedClassVersionError` / `invalid target release: 17` / `release version 17 not supported`: đang dùng JDK cũ hơn 17 (kể cả khi đã cài JDK mới nhưng biến `JAVA_HOME` hoặc PATH vẫn trỏ về bản cũ). Gõ `java -version` và `./mvnw -v` xem Maven đang dùng Java nào, sửa `JAVA_HOME` cho đúng.
- `Unsupported class file major version 6x` hoặc `Java NN is not supported`: JDK mới hơn các thư viện của Spring Boot 3.3 biết; dùng JDK 21 (hoặc 17) cho chắc.
- `No compiler is provided in this environment`: máy chỉ cài JRE, cần cài JDK đầy đủ.
- `The driver could not establish a secure connection to SQL Server ... TLS`: SQL Server quá cũ hoặc chứng chỉ lỗi, thêm `;encrypt=false` vào `DB_URL`.
- `Connection refused` / `connect timed out` tới `localhost:1433`: SQL Server chưa chạy hoặc chưa bật TCP/IP (xem bảng trên).
- `Vite requires Node.js version 20.19+ or 22.12+`: cài Node mới hơn (nên 24 LTS; có thể dùng `nvm use` trong thư mục `frontend` nhờ `frontend/.nvmrc`). Còn các dòng `npm warn EBADENGINE` (vd gói `@asamuzakjp/css-color` đòi Node 22.22.2 trở lên, đây là gói của bộ kiểm thử) chỉ là **cảnh báo**, không chặn `npm install` / `npm run dev`; hết khi dùng Node 24 LTS.

## Các trang trên giao diện (http://localhost:5173)
*(GĐ3: phải đăng nhập, menu hiện theo vai trò của tài khoản; xem mục "Giai đoạn 3" bên dưới. Đoạn sau là ghi chép GĐ2.)*
Giao diện quản trị chạy với vai trò **Quản lý** (thấy toàn bộ menu). Theo nhận xét của cô, header đã **bỏ ô phóng to 75%, ô chọn vai trò "Quản lý / Nhân viên" và nút sáng/tối chưa dùng**; header chỉ còn tiêu đề trang + Ca làm việc, Thông báo, Tài khoản; nội dung luôn hiển thị ở mức 75%. Menu theo vai trò (Nhân viên chỉ thấy Bán hàng tại quầy, Quản lý hóa đơn, Quản lý khách hàng) vẫn nằm trong code `MainLayout.vue` (`menuAll`, đổi hằng `role` nếu cần xem thử).

| Đường dẫn | Trang |
|---|---|
| /thong-ke | Thống kê (tổng hợp từ danh sách hóa đơn, chỉ Quản lý): chọn 7 / 30 / 90 ngày, Tất cả hoặc Tự chọn từ ngày - đến ngày; 4 chỉ số (doanh thu, tổng đơn, đơn hoàn thành, đơn hủy) kèm % tăng giảm so với kỳ liền trước; biểu đồ doanh thu theo ngày (tự gom theo tuần / tháng khi khoảng dài); cơ cấu trạng thái (có cả **Giao hàng thất bại**), doanh thu theo loại đơn, nhân viên bán tốt nhất, khách mua nhiều nhất, 8 đơn gần đây |
| /hoa-don, /hoa-don/{id} | Danh sách hóa đơn theo thiết kế: bộ lọc 1 hàng (mã hóa đơn, từ ngày - đến ngày **mặc định là ngày hiện tại và tự cập nhật khi qua ngày mới**, loại đơn Trực tuyến / Tại quầy, tự lọc khi đổi, nút Đặt lại đưa về hôm nay; xóa trống hai ô ngày để xem mọi ngày), các tab trạng thái kèm SỐ hóa đơn từng tab (Tất cả, Chờ xác nhận, Đã xác nhận, Chờ giao hàng, Đang giao hàng, **Giao hàng thất bại**, Đã giao hàng, Đã hoàn thành, Đã hủy, Hoàn tiền), bảng cột Mã HD / Mã NV / Tên KH / SĐT KH (SĐT trong hồ sơ khách; đơn giao hàng mà khách chưa có SĐT thì lấy SĐT người nhận; **khách lẻ mua tại quầy không có SĐT nên hiện —**) / Tổng tiền TT / Loại đơn / Ngày tạo / Trạng thái / Hành động, **phân trang rút gọn** (5 dòng mỗi trang, dạng `1 2 … 9 10 11 … 20`), xuất Excel toàn bộ danh sách đang lọc. **Trang chi tiết:** chỉ hiện đúng các nút chuyển trạng thái mà backend cho phép (`chuyenDuoc`); **Đã hoàn thành / Đã hủy là trạng thái cuối nên khóa nút**; đơn Đang giao hàng có nút **"Giao hàng thất bại"** (bắt buộc ghi lý do) rồi **giao lại** hoặc **hủy**; **chuyển sang Đã xác nhận thì tự mở bản in / lưu PDF** (trình duyệt chặn cửa sổ bật lên thì báo lỗi, hãy cho phép rồi bấm *In hóa đơn*); đơn **tại quầy phí vận chuyển 0, không đơn vị vận chuyển**; khách lẻ hiện "Khách vãng lai", không SĐT. **Bản in:** không có "Lịch sử thanh toán"; dòng giảm giá ghi **mã phiếu + %** (vd `Giảm giá (mã VCH7QX2YN – giảm 10%, tối đa 200.000 ₫)`), có **phí vận chuyển** (đơn giao hàng), **logo GHN / GHTK / Viettel Post** (chưa có file logo thì hiện ô chữ, xem `frontend/public/van-chuyen/README.txt`) và mã QR |
| /san-pham, /san-pham/them, /san-pham/{id}/sua | Sản phẩm: danh sách (hiện giá gốc gạch ngang + giá sau giảm nếu đang có đợt giảm giá), thêm, sửa. **Thuộc tính đổi theo Danh mục**: chọn *Vợt cầu lông* / *Vợt cầu lông người mới* thì có Độ cứng, Điểm cân bằng và biến thể theo Màu sắc + Trọng lượng + Chu vi cán; chọn *Phụ kiện*, *Túi - balo*, *Quần áo thể thao*, *Quả cầu lông* (mọi danh mục không phải vợt) thì ẩn các mục đó, chỉ còn Thương hiệu, Xuất xứ, Chất liệu và biến thể chỉ theo Màu sắc (hệ thống tự gán "Không áp dụng" cho phần ẩn, mục này không hiện trong ô chọn / danh sách thuộc tính). Trang Biến thể sản phẩm cũng theo quy tắc này. **Form thêm / sửa (theo lời cô):** mọi ô chọn là **Select2** (có ô tìm kiếm) kèm **"Thêm nhanh"** (gõ tên chưa có → bấm tạo luôn và tự chọn); biến thể **Màu × Trọng lượng × Chu vi** (2 × 2 × 2 thì ra đúng 8 biến thể, có dòng "Sẽ tạo …" xem trước); **bắt buộc có ảnh** (ảnh chung hoặc ảnh từng màu, backend cũng chặn); bảng biến thể **không còn cột Giảm giá** |
| /san-pham/bien-the | Biến thể sản phẩm: bảng phẳng có lọc theo sản phẩm, màu sắc, **trọng lượng, chu vi cán**, tồn kho, trạng thái; cột **Ảnh** (ảnh theo màu của biến thể); nút **Mã QR** từng biến thể (xem + tải ảnh QR chứa mã biến thể). Cửa sổ "Chi tiết sản phẩm" (nút mắt ở trang Sản phẩm) cũng có bảng biến thể phẳng + lọc màu / trọng lượng / chu vi + QR từng biến thể + ảnh theo màu |
| /san-pham/{danh-muc, thuong-hieu, xuat-xu, chat-lieu, do-cung, diem-can-bang, mau-sac, trong-luong, chu-vi} | 9 bảng thuộc tính (menu "Danh sách thuộc tính") |
| /khach-hang, /khach-hang/them | Khách hàng (kèm địa chỉ). Chọn địa chỉ theo **địa giới sau sáp nhập 07/2025** (API v2 của provinces.open-api.vn: Tỉnh/Thành phố -> Phường/Xã, không còn Quận/Huyện), dùng cho trang Thêm khách hàng và cửa sổ Địa chỉ (cần có mạng internet). Địa chỉ cũ trước sáp nhập vẫn hiện đủ, khi sửa thì nhắc chọn lại. Cửa sổ Địa chỉ: mỗi địa chỉ có 3 nút icon **Xem chi tiết** (xem chỉ đọc) / **Sửa** / **Xóa**. **Validate (theo lời cô):** bấm Lưu để trống hiện **lỗi đỏ ngay dưới từng ô, cùng lúc**; SĐT 10 số đầu 03 / 05 / 07 / 08 / 09, email đúng định dạng, họ tên không có chữ số, trùng email / SĐT báo ngay khi gõ; **ngày sinh (không bắt buộc) phải từ đủ 15 tuổi** (Bộ luật Dân sự 2015, Điều 21 khoản 4); form Sửa kiểm giống form Thêm. Căn cứ chi tiết: `CAN_CU_PHAP_LY_VA_QUY_TAC.md` |
| /dot-giam-gia, /dot-giam-gia/them, /dot-giam-gia/{id} | Đợt giảm giá: danh sách (lọc, xuất Excel, công tắc bật/tắt, xem chi tiết, nút "Sản phẩm áp dụng" để thêm / gỡ nhanh từng biến thể). Trạng thái hiển thị tính theo khoảng ngày: Sắp diễn ra / Đang diễn ra / Đã kết thúc / Ngừng hoạt động; công tắc luôn hiện, kể cả đợt đã kết thúc: đợt quá ngày kết thúc mà công tắc bật thì hiện "Đã kết thúc", tắt công tắc thì hiện "Ngừng hoạt động" (bật lại thì về "Đã kết thúc"). Khi soạn đợt (thêm / sửa) có khung "Lịch giảm giá khi đợt chồng nhau" xem trước theo dữ liệu đang nhập: biến thể nằm trong đợt khác cùng thời gian thì liệt kê từng khoảng ngày kèm mức Cao nhất / Trung bình, ô xanh là mức đang áp dụng. Trang xem / sửa còn có khung "Lịch giảm giá theo ngày" lấy từ backend (đã lưu), tính cả các đợt khác chồng lên (chế độ MAX hoặc TRUNG_BINH theo cấu hình). Trang thêm / xem / sửa: mã tự sinh dạng ngắn theo số thứ tự (DGG09, nút làm mới), tên, %, từ ngày - đến ngày, mô tả, chọn sản phẩm - biến thể áp dụng (lọc theo màu, trọng lượng, chu vi cán), hộp xác nhận trước khi lưu. **Làm lại theo lời cô:** ngày **giờ** bắt đầu / kết thúc, "Trạng thái dự kiến" hiện ngay khi chọn giờ, lỗi đỏ dưới từng ô, thẻ **"Khi nhiều đợt giảm giá chồng nhau thì tính thế nào?"** (cách tính đang dùng + ví dụ tự tính cho 3 cách `MAX` / `CONG_DON` / `TRUNG_BINH` + cảnh báo trùng đợt) |
| /phieu-giam-gia, /phieu-giam-gia/them, /phieu-giam-gia/{id}/sua | Phiếu giảm giá: công tắc bật/tắt luôn hiện, kể cả phiếu đã kết thúc (phiếu đã quá ngày kết thúc: công tắc hiện ở trạng thái tắt và bị khóa; nhãn "Đã kết thúc" nếu phiếu đang bật, "Ngưng hoạt động" nếu đã tắt trước đó). Ô tiền có định dạng 1.500.000 ₫. Phiếu cá nhân gửi mail khi tạo (mail phiếu mới) và khi sửa phiếu còn "Sắp diễn ra" (phiếu đã / đang diễn ra thì sửa không gửi mail): đổi thông tin phiếu → khách giữ lại nhận mail cập nhật; thêm khách → mail phiếu mới; bỏ khách → mail hủy; chỉ đổi danh sách khách (thông tin phiếu giữ nguyên) → khách giữ lại không nhận mail. **Form làm lại theo mẫu nhóm đầu (lời cô):** 2 khối *Thông tin chung* + *Đối tượng áp dụng*, **ngày giờ** bắt đầu / kết thúc kèm "Trạng thái dự kiến"; phiếu **Công khai** có ô Số lượng, phiếu **Cá nhân không có ô Số lượng** (tự bằng số khách chọn); bảng chọn khách có **Tổng đơn, Mua gần nhất, Ngày sinh, Giới tính** và bộ lọc theo các cột đó; khung "Khi có nhiều ưu đãi cùng lúc thì tính thế nào?" (mỗi đơn 1 phiếu, đợt giảm trước - phiếu giảm sau, chế độ chồng đợt đang dùng); lỗi đỏ dưới từng ô |
| /nhan-vien, /nhan-vien/them, /nhan-vien/{id} | Nhân viên. Thêm nhân viên: **mã tự tạo theo họ tên** (chữ cái đầu của mỗi từ + số thứ tự tối thiểu 2 chữ số: Lê Quốc Tuấn -> `LQT01`, người tiếp theo trùng chữ cái đầu -> `LQT02`, quá 99 thì `LQT100`; mã mẫu `NV01`–`NV10` giữ nguyên), hiện ngay khi nhập họ tên; **nhiều địa chỉ** (Tỉnh/Thành phố -> Phường/Xã, chọn 1 địa chỉ chính, không có nút xóa); nút **Quét căn cước** đọc mã QR trên thẻ CCCD (camera / chọn ảnh / máy quét mã vạch) và điền họ tên, ngày sinh, giới tính, số CCCD, đồng thời **tự thêm địa chỉ trên thẻ (địa chỉ cũ có Quận/Huyện) vào danh sách địa chỉ mới**: tỉnh bị sáp nhập tự đổi sang tỉnh mới (vd Thái Bình -> Hưng Yên), Phường/Xã khớp thì dùng tên mới, không khớp thì giữ chữ cũ để bấm sửa chọn lại (Quận/Huyện cũ nằm ở cuối địa chỉ cụ thể); thẻ thiếu số nhà / đường thì mở sẵn ô nhập; quét lại cùng thẻ không thêm trùng. Trạng thái nhân viên: **Còn làm / Đã nghỉ** (nút "Cho nghỉ việc" / "Cho làm lại"). Không còn "Vị trí làm việc". **Theo lời cô:** chỉ có **2 quyền** (Quản lý / Nhân viên); ngày sinh **bắt buộc, từ đủ 18 tuổi** (Bộ luật Lao động 2019, Điều 143); lỗi đỏ dưới từng ô, form Sửa kiểm giống form Thêm; nút **Nhập Excel** hàng loạt (xem mục "Nhập nhân viên từ Excel" bên dưới) |


**Cập nhật theo nhận xét của cô (self-review 12/10/2026, sửa lần 2 theo 2 video mẫu):**
- **Header:** chỉ còn tiêu đề trang, Ca làm việc, chuông Thông báo, Tài khoản (bỏ ô 75%, ô "Quản lý", nút sáng/tối).
- **Hóa đơn:** có **mã QR hóa đơn** (QR chứa mã hóa đơn, tạo ngay trên trình duyệt bằng thư viện `qrcode`, cần chạy lại `npm install`) thành một khối riêng ở trang chi tiết và ở góc trên bên phải của bản in; trang chi tiết tách khối: Trạng thái đơn hàng, Thông tin đơn hàng (hàng 1) — Khách hàng, Giao hàng, Lịch sử thanh toán, Mã QR (hàng 2) — Danh sách sản phẩm + tổng tiền (hàng 3); nút *In hóa đơn* / *Về danh sách* ở thanh trên cùng; bản in tách thành các phần riêng (Thông tin đơn hàng, Khách hàng, Giao hàng, Sản phẩm, Tổng tiền; **từ 15/10 không in "Lịch sử thanh toán"** theo lời cô). **Xuất Excel** danh sách hóa đơn: cột tiền là SỐ (định dạng 3.861.000) để Excel cộng / lọc được.
- **Bộ lọc hóa đơn — ngày hiện tại (bổ sung 13/10/2026):** ô *Từ ngày* và *Đến ngày* mặc định là **hôm nay** (lấy theo giờ máy, không lệch múi giờ), mở trang là thấy hóa đơn trong ngày, số trên các tab cũng tính theo hôm nay. Ngày được **cập nhật liên tục**: để trang mở qua 0h (trang tự kiểm tra mỗi 30 giây) hoặc máy ngủ qua đêm rồi quay lại tab, ô nào đang là "hôm nay" tự nhảy sang ngày mới và danh sách tải lại; ô đã tự chọn ngày khác thì giữ nguyên, ô đã xóa trống (xem mọi ngày) thì không tự điền lại. *Đặt lại bộ lọc* đưa về ngày hiện tại lúc bấm. Xuất Excel lấy đúng danh sách đang lọc. Phía backend, *Đến ngày* lấy trọn ngày (`ngay_tao < 0h ngày hôm sau`) nên không sót hóa đơn tạo lúc 23:59:59.
- **Ô chọn ngày dạng ngày/tháng/năm (bổ sung 13/10/2026):** mọi ô ngày trong ứng dụng (bộ lọc Hóa đơn, Thống kê *Tự chọn*, danh sách + form Phiếu giảm giá / Đợt giảm giá, ngày sinh Khách hàng, ngày sinh + ngày vào làm Nhân viên) dùng chung component `frontend/src/components/common/DateInput.vue`, **luôn hiện 13/10/2026** dù trình duyệt (Edge / Chrome) đang để tiếng Anh — ô `<input type="date">` cũ hiện theo ngôn ngữ trình duyệt nên máy tiếng Anh thấy 10/13/2026. Gõ tay được: gõ liền `13102026` tự thành `13/10/2026`, gõ `1/` tự thành `01/`; ngày không có thật (31/02) hoặc gõ dở rồi rời ô thì trả lại ngày cũ. Bấm vào ô hoặc biểu tượng lịch (hoặc phím mũi tên xuống) để mở **lịch tiếng Việt**: tuần bắt đầu thứ Hai (T2 … CN), chọn nhanh *Tháng* / *Năm*, nút *Hôm nay* và *Xóa*, ngày ngoài giới hạn (vd *Đến ngày* trước *Từ ngày*, ngày sinh nhân viên dưới 18 tuổi) bị gạch và không chọn được; phím mũi tên / PageUp / PageDown để di chuyển, Esc để đóng. Giá trị gửi lên API **vẫn là `yyyy-mm-dd`** nên backend và dữ liệu không đổi. Hàm chuyển đổi ở `frontend/src/utils/ngay.js`, test ở `frontend/tests/oChonNgay.test.js`.
- **Thêm sản phẩm - biến thể (làm lại theo 2 video mẫu):** chọn màu (và trọng lượng, chu vi cán với vợt) → dòng chữ "Sẽ tạo 4 màu × 3 trọng lượng × 2 chu vi = 24 biến thể" → bấm **Tạo biến thể**. Biến thể **nhóm theo từng màu** (tiêu đề màu có chấm màu, ô tích cả màu, số biến thể của màu; bên dưới là bảng Trọng lượng × Chu vi, Số lượng, Giá bán, Xóa). Có ô **Số lượng mặc định** + **Giá bán mặc định** + nút **Áp dụng** (chỉ áp cho biến thể đang tích), ô **Chọn tất cả biến thể (x/y)**, tích / xóa từng dòng. Nút **"Bảng chung"** chuyển sang một bảng phẳng (cột Màu sắc) nếu muốn xem tất cả trong một bảng — mặc định là nhóm theo màu (đổi hằng `CHE_DO_HIEN_THI_MAC_DINH` trong `frontend/src/utils/bienThe.js` thành `'chung'` nếu muốn mặc định là bảng chung).
  Phần **"Ảnh sản phẩm theo màu"**: mỗi màu một ô "Ảnh sản phẩm màu Đen – Áp dụng cho 6 biến thể cùng màu" + nút Thêm ảnh (tối đa 6 ảnh / màu, ảnh đầu là ảnh chính). Ảnh lưu ở `hinh_anh_san_pham.id_mau_sac`; trang Biến thể, chi tiết sản phẩm, hóa đơn, đợt giảm giá hiện ảnh đúng màu của biến thể (màu chưa có ảnh thì dùng ảnh chung). Trang **Sửa sản phẩm** cũng sửa được ảnh theo màu cho các màu sản phẩm đã có biến thể.
  **Giá bán** có định dạng tiền (1.500.000 ₫); giá trống / 0 / dưới 1.000 ₫ hoặc số lượng sai thì báo "Có N biến thể chưa hợp lệ…" và viền đỏ đúng ô. Danh mục không phải vợt: chỉ chọn màu (mỗi màu 1 biến thể), trọng lượng / chu vi tự gán "Không áp dụng".
- **Phiếu giảm giá:** các ô tiền (Giá trị giảm theo số tiền, Giảm tối đa, Đơn tối thiểu) có định dạng 1.500.000 ₫. Phiếu **cá nhân**: số lượng tự bằng số khách đã chọn; danh sách khách hiện email (khách chưa có email có nhãn "Chưa có email"); dòng báo "Khi lưu, hệ thống gửi mail tặng phiếu cho N khách"; khung trạng thái Gmail (chưa cấu hình / đăng nhập lỗi + lý do / sẵn sàng); sau khi lưu tự báo **đã gửi** hay **gửi lỗi + lý do** (3 loại mail Mới / Cập nhật / Hủy như mục Gửi mail ở trên). Sửa phiếu đã bắt đầu thì form báo trước là sẽ không gửi mail.
- **Đợt giảm giá (làm lại phần chọn):** một ô tìm kiếm (lọc ngay khi gõ), tích sản phẩm = chọn hết biến thể, bấm "+" rồi bấm nhãn biến thể (Màu · Trọng lượng · Chu vi) để chọn / bỏ từng biến thể. Thẻ **"Tổng kết đợt giảm giá"** (mức giảm, số sản phẩm, số biến thể, ngày bắt đầu, ngày kết thúc + số ngày, tổng giá trước / sau giảm). Bảng **"Sản phẩm & biến thể đã chọn áp dụng"**: mỗi dòng một biến thể (Ảnh, Sản phẩm, Biến thể, Giá bán, Giá sau giảm, Xóa), lọc theo màu sắc / trọng lượng / chu vi, nút "Gỡ N biến thể đang lọc". Giá sau giảm dùng chung một công thức làm tròn tới đồng (`tinhGiaSauGiam`) cho danh sách sản phẩm, biến thể và trang soạn đợt.

## Giai đoạn 3 (cập nhật 25/10/2026)

### Tài khoản demo (mật khẩu `Footstyle@123`)
| Tài khoản | Vai trò | Ghi chú |
|---|---|---|
| `NV01`, `NV02` | Quản lý | Thấy mọi menu, không bắt buộc vào ca (vẫn cần vào ca nếu tự bán tại quầy) |
| `NV03` - `NV10` | Nhân viên | Đăng nhập xong **bắt buộc vào ca** mới dùng được chức năng khác |

Đăng nhập bằng mã nhân viên, email hoặc số điện thoại. Khi chạy `npm run dev`, trang đăng nhập có 3 nút điền nhanh NV01 / NV03 / NV04.
Khách hàng đăng nhập ở trang riêng của web bán hàng: http://localhost:5173/shop/dang-nhap.

### Luồng chính để demo
1. **Đăng nhập** (`/dang-nhap`): một trang chung cho quản lý và nhân viên, vào trong tự phân quyền (nhân viên không thấy Thống kê, Sản phẩm, Giảm giá, Nhân viên, Xếp lịch).
   Sai mật khẩu 5 lần liên tiếp bị khóa 5 phút. Phiên đăng nhập hết hạn sau 12 giờ (`app.auth.thoi-han-gio`).
2. **Vào ca** (`/vao-ca`, bắt buộc với nhân viên): đếm tiền mặt trong két và đối soát chuyển khoản của ca trước (nút "Dùng số tiền này"); số khác ca trước thì phải ghi lý do.
   Chưa vào ca thì mọi trang chuyển về `/vao-ca` và backend trả 409 `CHUA_NHAN_CA` cho mọi API (trừ đăng nhập, giao ca, xem lịch của mình).
   **Kết ca / giao ca** ở nút "Ca làm việc" trên header: hệ thống tổng kết tiền mặt / chuyển khoản, lệch thì bắt buộc ghi lý do; còn hóa đơn chờ thì không kết ca được. Kết ca xong in được biên bản giao ca.
3. **Lịch làm việc** (menu "Ca làm việc"): quản lý xếp lịch theo ngày / tuần / tháng, nhập - xuất Excel; ca phải nằm trong giờ mở cửa **07:00 - 22:00**. Nhân viên xem "Lịch làm của tôi" và "Lịch sử giao ca".
4. **Bán tại quầy** (`/ban-hang`):
   - Hóa đơn chờ **lưu trong CSDL** (tối đa 10 / nhân viên), thêm sản phẩm là **trừ tồn kho ngay**, bỏ sản phẩm / hủy hóa đơn thì trả lại kho.
   - Nút **"Chọn sản phẩm"** mở hộp thoại: lọc danh mục, thương hiệu, màu, trọng lượng, chu vi, khoảng giá; mới nhất trước; phân trang; hết hàng tự ẩn; nhập số lượng lớn hơn tồn kho báo ngay. Có ô quét mã / camera.
   - Tự áp **phiếu giảm giá tốt nhất** cho khách (kể cả phiếu cá nhân), đổi được ở ô chọn phiếu.
   - **Case khó:** giá đổi sau khi đã thêm (đợt giảm giá bắt đầu / kết thúc) → cùng sản phẩm thêm tiếp thành dòng mới, phải bấm "Cập nhật giá" mới thanh toán được; phiếu bị sửa mức giảm khi hóa đơn đang chờ → báo `PHIEU_THAY_DOI`, không tự đổi số tiền.
   - Công tắc **Giao hàng**: khách lẻ nhập một địa chỉ; phí 30.000 ₫, đơn từ 1.000.000 ₫ miễn phí; thanh toán xong đơn sang thẳng "Chờ giao hàng" (không cần xác nhận lại). Không giao hàng thì "Đã hoàn thành" ngay và in hóa đơn luôn.
   - Thanh toán tiền mặt / chuyển khoản (mã VietQR) / kết hợp.
5. **Đơn online** (`/hoa-don/{id}`): tồn kho chỉ trừ khi chuyển **Chờ xác nhận → Đã xác nhận**.
   - **Case khó:** nút "Chỉnh sửa đơn" (chỉ **một lần**, khi còn Chờ xác nhận; khách không tự sửa): đổi người nhận / địa chỉ, thêm - sửa - bỏ sản phẩm → tính lại phiếu + phí ship; khách đã chuyển khoản trước thì ghi **hoàn phí** (số tiền âm) hoặc **phụ phí** (thu khi giao). Hủy đơn đã trả trước → ghi hoàn phí.
   - Nút "Xác nhận đã nhận chuyển khoản" cho đơn khách chọn chuyển khoản.
6. **Thống kê** (`/thong-ke`, chỉ quản lý): mọi khối theo bộ lọc ở trên (Hôm nay / 7 / 30 / 90 ngày / Tất cả / Tự chọn); **doanh thu thực tế** (đơn Đã hoàn thành) và **doanh thu dự kiến** (mọi đơn chưa hủy), cả hai **không tính phí ship**; nút **Xuất Excel** (mỗi bảng một sheet).
   Nút **So sánh** (`/thong-ke/so-sanh`): theo tháng / quý / năm, so với kỳ trước và cùng kỳ năm trước, biểu đồ tăng trưởng, xuất Excel, **gửi email báo cáo kèm biểu đồ**; ngày 1 hằng tháng 8:00 tự gửi báo cáo tháng trước cho quản lý (đổi giờ bằng `app.thong-ke.cron-bao-cao`; cần cấu hình Gmail như phần "Gửi mail" ở trên).
7. **Trò chuyện:** "Hỗ trợ khách hàng" (khách chat từ nút "Tư vấn trực tuyến" trên `/shop` hoặc trang `/ho-tro`, trợ lý ảo trả lời trước, khách bấm gặp nhân viên) và "Chat nội bộ" giữa nhân viên; số tin chưa đọc hiện trên menu và chuông. Nút **Trợ lý AI** trên header trả lời nhanh về doanh thu, đơn chờ, tồn kho từ dữ liệu thật.
8. **Bán hàng online** (`/shop`): trang chủ, danh mục có bộ lọc + mega menu, chi tiết sản phẩm, giỏ hàng, thanh toán (COD / chuyển khoản VietQR), tra cứu đơn, gợi ý AI từ ảnh (`/shop/goi-y-ai`).
9. **Thuộc tính sản phẩm:** giá trị phải đúng chuẩn (màu chỉ gồm chữ; trọng lượng dạng `4U (80-84g)` đúng khoảng gram của U; chu vi `G1` - `G6`); form báo lỗi ngay, backend cũng chặn.

**Không làm phần app (ứng dụng di động)** ở GĐ3: dự án chỉ gồm web quản trị, web bán hàng online và backend.

### API mới của GĐ3 (tóm tắt)
| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| POST | /api/auth/dang-nhap, /api/auth/khach-hang/dang-nhap | Đăng nhập nhân viên / khách hàng, trả token |
| GET / POST | /api/giao-ca/hien-tai, /api/giao-ca/nhan-ca, /api/giao-ca/ket-ca | Ca đang mở + đối soát ca trước, vào ca (`tienDauCa`, `chuyenKhoanCaTruoc`, `ghiChu`), kết ca |
| GET / POST / PUT / DELETE | /api/ban-hang/hoa-don-cho[/{id}[/san-pham, /chi-tiet/{idCt}, /khach-hang, /cap-nhat-gia, /thanh-toan]] | Hóa đơn chờ tại quầy lưu CSDL (trừ / trả kho ngay), thanh toán kèm `giaoHang` |
| GET | /api/ban-hang/san-pham/loc | Bảng chọn sản phẩm: lọc + khoảng giá + phân trang, chỉ hàng còn tồn |
| PUT / POST | /api/hoa-don/{id}/sua-don, /api/hoa-don/{id}/xac-nhan-thanh-toan | Sửa đơn online một lần (hoàn phí / phụ phí), xác nhận đã nhận chuyển khoản |
| GET / POST | /api/thong-ke/so-sanh?kieu=THANG\|QUY\|NAM&ky=, /api/thong-ke/so-sanh/gui-email | Số liệu so sánh, gửi email báo cáo kèm biểu đồ |

## Sửa theo lời cô review giai đoạn 2 (15/10/2026)

Cả 21 điểm cô nhắc đã được làm; bảng đối chiếu từng điểm (làm ở đâu, test nào kiểm) nằm trong `KE_HOACH_SUA_THEO_LOI_CO.md`,
còn **căn cứ pháp lý và quy tắc nghiệp vụ** (vì sao khách từ 15 tuổi, nhân viên từ 18 tuổi, vì sao lấy mức giảm cao nhất...) nằm trong `CAN_CU_PHAP_LY_VA_QUY_TAC.md`. Tóm tắt:

- **Ô chọn:** mọi combobox là Select2 (`frontend/src/components/common/Select2.vue`, có ô tìm kiếm); các ô thuộc tính sản phẩm có **"Thêm nhanh"**.
- **Tìm kiếm Thương hiệu / Xuất xứ (cố ý đúng từng chữ):** ở hai trang `/san-pham/thuong-hieu` và `/san-pham/xuat-xu`, ô tìm kiếm phân biệt chữ hoa / thường và dấu vì đây là tên riêng: gõ `Việt Nam` thì ra, gõ `việt nam`, `Viet Nam` hay `VIỆT NAM` thì **không** ra (gõ dở như `Việt` vẫn ra; dấu gõ kiểu nào cũng được). Trang có sẵn dòng gợi ý này dưới ô tìm kiếm. Các trang thuộc tính khác, các ô chọn Select2 và các ô tìm kiếm còn lại vẫn tìm thoải mái (không phân biệt hoa / thường và dấu). Muốn đổi, sửa cờ `timChinhXac` trong `frontend/src/constants/thuocTinh.js`. Test: `frontend/tests/timKiemChinhXac.test.js`.
- **Sản phẩm:** biến thể Màu × Trọng lượng × Chu vi, **ảnh bắt buộc**, bỏ cột Giảm giá ở bảng biến thể; **phân trang rút gọn** `1 2 … 9 10 11 … 20` ở mọi danh sách.
- **Hóa đơn:** trạng thái mới **Giao hàng thất bại** (mã 8), trạng thái cuối khóa nút, **tự mở bản in PDF khi chuyển sang Đã xác nhận**, bản in ghi mã phiếu + %, phí vận chuyển, logo hãng vận chuyển;
  đơn tại quầy phí ship 0, khách lẻ không SĐT.
- **Phiếu giảm giá / Đợt giảm giá:** làm lại form (ngày **giờ**, "Trạng thái dự kiến", lỗi dưới từng ô), quy tắc chồng đợt `MAX` / `CONG_DON` / `TRUNG_BINH`, bảng chọn khách có lọc.
- **Khách hàng / Nhân viên:** validate đủ ở form Thêm **và** Sửa, tuổi có căn cứ, chỉ 2 quyền, **Nhập Excel hàng loạt**.

### Nhập nhân viên từ Excel
1. Trang **Nhân viên** → nút **Nhập Excel** → **Tải file mẫu (.xlsx)** (có 2 dòng ví dụ, xóa trước khi nhập thật). Cột có `(*)` là bắt buộc: Họ và tên, Giới tính (Nam / Nữ), Ngày sinh (dd/mm/yyyy), Số điện thoại, Email,
   Địa chỉ cụ thể, Phường/Xã, Tỉnh/Thành phố; không bắt buộc: Số căn cước công dân, Ngày vào làm (bỏ trống = hôm nay), Vai trò (chỉ Quản lý / Nhân viên, bỏ trống = Nhân viên). Thứ tự cột không quan trọng.
2. Chọn file (chỉ `.xlsx`, tối đa 5 MB, tối đa 100 nhân viên) → **Kiểm tra file**. **Sai dù chỉ một dòng thì cả file bị từ chối**, chưa có ai được thêm, bảng lỗi nêu rõ **dòng nào, cột nào, sai gì**
   (kiểm giống form Thêm nhân viên, kèm trùng email / SĐT / căn cước ngay trong file và với nhân viên đã có). Sửa file rồi chọn lại.
3. File hợp lệ → xem trước (mã dự kiến, họ tên, email, vai trò) → **Nhập vào hệ thống**: ghi tất cả trong một giao dịch, tạo **mật khẩu tạm ngẫu nhiên** và **tự gửi mail tài khoản** cho từng nhân viên
   (dùng chung cấu hình Gmail `mail.properties` như mail phiếu giảm giá), hộp thoại hiện tiến độ gửi. Chưa cấu hình Gmail thì có cảnh báo và nút **Tải danh sách tài khoản** (mật khẩu tạm chỉ hiện đúng một lần) để gửi cho nhân viên theo cách khác.
4. Mã nhân viên tự tạo theo họ tên như form Thêm (`LQT01`, `LQT02`...). Code: `NhanVienImportService.java`, `frontend/src/components/nhanVien/NhanVienImportModal.vue`.

### Logo hãng vận chuyển trên bản in hóa đơn
Logo GHN / GHTK / Viettel Post là nhãn hiệu của các hãng nên **không đóng gói trong mã nguồn**. Chưa có file thì bản in và trang chi tiết hiện ô chữ thay thế (đủ để phân biệt hãng).
Muốn dùng logo thật: tải bộ nhận diện chính thức của hãng, đặt đúng tên `ghn.png`, `ghtk.png`, `vtp.png` vào `frontend/public/van-chuyen/` (xem `README.txt` trong thư mục đó), chạy lại frontend - không phải sửa code.

### Ngày giờ của phiếu / đợt giảm giá
Phiếu và đợt có cả **giờ, phút**: API nhận `yyyy-MM-ddTHH:mm` hoặc `yyyy-MM-ddTHH:mm:ss` (đợt giảm giá còn nhận dạng ngày `yyyy-MM-dd` của dữ liệu cũ, hiểu là 00:00 - 23:59). Tính theo phút: giờ bắt đầu tính từ đầu phút, giờ kết thúc tính tới hết phút. Tạo mới không được bắt đầu trong quá khứ (cho lệch 5 phút), sửa phiếu / đợt đã bắt đầu thì không đổi giờ bắt đầu.
Quy ước chung ở `ThoiGianPhut.java`.

## API
| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| GET | /api/hoa-don?maHoaDon&tuNgay&denNgay&loaiDon&trangThai&page&size | Danh sách hóa đơn (lọc + phân trang) |
| GET | /api/hoa-don/dem-trang-thai?maHoaDon&tuNgay&denNgay&loaiDon | Số hóa đơn theo từng trạng thái `{ tatCa, 0..8 }` (áp cùng bộ lọc, trừ trạng thái) để hiện số trên các tab |
| GET | /api/hoa-don/{id} | Chi tiết hóa đơn (khách, giao hàng, sản phẩm, thanh toán, lịch sử) kèm `hoaDon.chuyenDuoc` (các nút chuyển trạng thái được phép, giao diện chỉ hiện đúng các nút này), `laTrangThaiCuoi`, `khachHang.khachLe`, mã / loại / giá trị phiếu giảm giá đã dùng, phí vận chuyển, đơn vị vận chuyển |
| PUT | /api/hoa-don/{id}/trang-thai | Chuyển trạng thái theo luật trong `HoaDonLuongTrangThai` (body `{ trangThaiMoi, ghiChu, nguoiThaoTac }`): đơn giao hàng 1 → 2 → 3 → 4 → 5 → 6, đơn tại quầy 1 → 6; hủy được khi chưa giao cho vận chuyển; đang giao (4) báo được **Giao hàng thất bại (8)** rồi giao lại hoặc hủy. **Hủy đơn và giao hàng thất bại bắt buộc ghi lý do**; hủy thì trả số lượng về tồn kho. Đã hoàn thành / Đã hủy là trạng thái cuối, chuyển trái luật trả 400. Không xóa dữ liệu |
| GET | /api/phieu-giam-gia | Danh sách phiếu |
| GET | /api/phieu-giam-gia/{id} | Chi tiết (kèm khách hàng được tặng) |
| GET | /api/phieu-giam-gia/khach-hang | Khách hàng có thể chọn khi tặng phiếu cá nhân, kèm `tongDon` (số đơn Đã hoàn thành), `muaGanNhat`, `ngaySinh`, `gioiTinh` để lọc |
| POST / PUT | /api/phieu-giam-gia[/{id}] | Tạo / sửa. Kết quả có thêm `ketQuaMail` (câu tóm tắt số mail đã xếp gửi) |
| GET | /api/phieu-giam-gia/trang-thai-mail | Trạng thái gửi mail `{ daCauHinh, dangNhap: CHUA_CAU_HINH / DANG_KIEM_TRA / THANH_CONG / THAT_BAI, lyDo }` (kết quả đăng nhập thử Gmail lúc khởi động) |
| GET | /api/phieu-giam-gia/{id}/ket-qua-mail | Kết quả lô mail của lần lưu phiếu gần nhất `{ tong, daGui, loi, giaLap, xong, lyDo }` (giao diện hỏi lại sau khi lưu để báo đã gửi / gửi lỗi) |
| GET | /api/phieu-giam-gia/thu-gui-mail | Gửi 1 thư thử đến chính email gửi và trả về lỗi thật nếu có |
| PUT | /api/phieu-giam-gia/{id}/trang-thai | Ẩn / hiện phiếu (không xóa dữ liệu) |
| GET | /api/nhan-vien, /api/nhan-vien/{id} | Danh sách / chi tiết nhân viên |
| POST / PUT | /api/nhan-vien[/{id}] | Thêm: nhập đủ (mật khẩu băm BCrypt, không trả về); `canCuoc` (số căn cước 12 số, không trùng, lưu ở cột `can_cuoc_cong_dan`), `diaChis[]` (ít nhất 1 địa chỉ); **mã nhân viên do backend tự sinh theo họ tên** (chữ cái đầu của mỗi từ + số thứ tự tối thiểu 2 chữ số, vd `LQT01`). Sửa: chỉ đổi họ tên, giới tính, ngày sinh, SĐT, căn cước, địa chỉ, vai trò; mã, email, ngày vào làm, mật khẩu giữ nguyên |
| PUT | /api/nhan-vien/{id}/trang-thai | Ẩn / hiện nhân viên (không xóa dữ liệu) |
| POST | /api/nhan-vien/import | Nhập HÀNG LOẠT từ file Excel (multipart: `file` .xlsx, `chiKiemTra` = true là chỉ kiểm tra chưa lưu). **Sai dù một dòng → 422 mã `FILE_KHONG_HOP_LE`** kèm `chiTiet.loi[{ dong, truong, cot, thongBao }]`, chưa ghi gì; hợp lệ → ghi TẤT CẢ trong một giao dịch, tạo mật khẩu tạm, xếp mail tài khoản, trả `taiKhoan[]` (mật khẩu tạm chỉ trả một lần) và `maLo`. Tối đa 100 dòng |
| GET | /api/nhan-vien/import/ket-qua-mail?lo= | Tiến độ gửi mail tài khoản của một lần nhập `{ tong, daGui, loi, giaLap, xong, lyDo }` |
| GET | /api/vai-tro | Danh sách vai trò (chỉ có Quản lý và Nhân viên) |
| GET | /api/khach-hang?search=, /api/khach-hang/{id} | Danh sách (tìm theo mã, tên, SĐT, email) / chi tiết khách hàng (kèm danh sách địa chỉ) |
| POST / PUT | /api/khach-hang[/{id}] | Thêm (kèm địa chỉ mặc định nếu gửi tinhThanh, phuongXa, diaChiCuThe; `quanHuyen` không còn dùng cho địa chỉ mới vì đã sáp nhập) / sửa. Mật khẩu tạm được băm, không trả về |
| PUT | /api/khach-hang/{id}/trang-thai | Bật / tắt hoạt động khách hàng |
| GET | /api/dia-chi/khach-hang/{idKhachHang} | Địa chỉ của khách hàng |
| POST / PUT / DELETE | /api/dia-chi/khach-hang/{idKhachHang}, /api/dia-chi/{id} | Thêm / sửa / xóa địa chỉ (luôn có đúng 1 địa chỉ mặc định; tối đa 10 địa chỉ mỗi khách, địa chỉ trùng bị chặn 409, xóa địa chỉ mặc định thì địa chỉ còn lại đầu tiên thành mặc định) |
| PUT | /api/dia-chi/{id}/mac-dinh | Chỉ đặt địa chỉ làm mặc định (không cần gửi lại cả form; địa chỉ cũ nhập thiếu thông tin vẫn đặt được) |
| GET | /api/dot-giam-gia | Danh sách đợt giảm giá, đợt mới thêm nằm trên cùng |
| GET | /api/dot-giam-gia/ma-moi | Mã đợt do hệ thống sinh (DGG + số thứ tự, vd DGG09, DGG100), không trùng mã đã có |
| GET | /api/dot-giam-gia/{id} | Chi tiết một đợt giảm giá |
| POST / PUT | /api/dot-giam-gia[/{id}] | Thêm / sửa đợt giảm giá. Body: `maDot, tenDot, phanTramGiamDot, ngayBatDau, ngayKetThuc, moTa, idBienThe[]` (nhận cả tên cũ `idBienThes`; ngày dạng `yyyy-MM-dd` hoặc `yyyy-MM-ddTHH:mm:ss`). Lưu đợt và đồng bộ biến thể áp dụng trong một lần (thêm bắt buộc có ít nhất 1 biến thể đang bán; sửa không gửi `idBienThe` thì giữ nguyên biến thể; sửa không đổi mã đợt). Đổi % của đợt thì áp % mới cho mọi biến thể; không đổi % thì giữ mức giảm riêng của từng biến thể |
| PUT | /api/dot-giam-gia/{id}/trang-thai | Bật / tắt hoạt động đợt giảm giá (1: đang hoạt động, 0: ngừng hoạt động). Đợt đã qua ngày kết thúc thì không đổi được (409) |
| GET | /api/dot-giam-gia/{id}/lich-giam-gia | Lịch giảm giá theo khoảng ngày của từng biến thể trong đợt, có tính các đợt đang bật chồng lên (mỗi đoạn: `tuNgay, denNgay, phanTramApDung` = mức áp dụng theo cấu hình MAX / TRUNG_BINH, `phanTramCaoNhat`, `phanTramTrungBinh`, `cacDot`, `giaSauGiam`) |
| GET | /api/dot-giam-gia/ap-dung | Mức giảm của từng biến thể theo các đợt đang bật mà chưa kết thúc `{ chinhSach, muc: [{ idBienThe, idDot, maDot, tenDot, phanTram, ngayBatDau, ngayKetThuc }] }`; trang soạn đợt dùng để xem trước lịch chồng đợt |
| GET | /api/dot-giam-gia/ap-dung-hom-nay | Mức giảm đang áp dụng hôm nay của từng biến thể `{ idBienThe: phanTram }` (nhiều đợt chồng nhau thì gộp theo MAX / TRUNG_BINH; dùng cho giá gạch ngang ở danh sách sản phẩm / biến thể) |
| GET | /api/dot-giam-gia/che-do | Cách gộp khi nhiều đợt chồng nhau đang cấu hình `{ cheDo: MAX / CONG_DON / TRUNG_BINH, moTa }` (form Phiếu / Đợt giảm giá hiển thị cho người dùng) |
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
  Cột này nằm trong `database/SD_17_1_cau_truc.sql`, chất liệu mẫu theo nhóm trong `database/SD_17_2_du_lieu.sql`; khi chạy backend, `SchemaUpgradeRunner` cũng tự thêm cột + chất liệu mới.
- **Cột "Giảm giá" (trang Biến thể sản phẩm)** = % giảm của đợt giảm giá **đang diễn ra hôm nay** mà biến thể nằm trong đó. Thêm biến thể ở Đợt giảm giá -> "Sản phẩm áp dụng":
  đợt đang diễn ra thì hiện ngay; đợt sắp diễn ra hiện "Sắp giảm -x% từ ngày..."; đợt ngừng hoạt động / đã kết thúc thì không giảm. Đợt đã kết thúc thì không thêm được biến thể mới;
  biến thể đang ngừng bán không áp dụng giảm giá được (backend chặn, giao diện báo rõ).
- **Hóa đơn**: cột SĐT KH lấy SĐT hồ sơ khách; đơn giao hàng mà khách chưa có SĐT thì lấy SĐT người nhận; **khách lẻ mua tại quầy không có SĐT** (hiện —, theo lời cô). Tên khách: hồ sơ khách → (đơn giao hàng) tên người nhận → "Khách vãng lai".
- **Thêm sản phẩm**: nút tạo biến thể tên là "Tạo biến thể".

## Gộp với bản của nhóm trên git (15/10/2026)

Bản của nhóm là nhánh làm song song, **cũ hơn** ở hầu hết các trang (chưa có các sửa theo lời cô), nên không chép đè mà chỉ lấy phần **mới thật sự** và hợp với code hiện tại:

| Lấy từ bản nhóm | Ghi chú |
|---|---|
| **Ảnh đại diện khách hàng** | Thêm cột `khach_hang.anh_dai_dien` (entity, DTO, `SchemaUpgradeRunner` tự thêm khi chạy backend, `SD_17_1_cau_truc.sql`). Trang Thêm / Sửa khách hàng có nút "Tải ảnh lên / Đổi ảnh / Bỏ ảnh" (JPG, PNG, WEBP, GIF, tối đa 5 MB, dùng lại `POST /api/hinh-anh/upload`). Danh sách khách hàng hiện ảnh, chưa có ảnh hoặc ảnh hỏng thì hiện chữ cái đầu. Backend kiểm tra đường dẫn (chỉ `/api/uploads/...` hoặc `http(s)://`, tối đa 500 ký tự); khi sửa, không gửi trường này = giữ ảnh cũ, gửi chuỗi rỗng = bỏ ảnh. |
| **Cột "Tên người nhận"** ở danh sách khách hàng (và trong file Excel xuất) | Lấy theo địa chỉ mặc định; chưa có địa chỉ thì ghi "Chưa cập nhật". |
| **Hộp xác nhận có icon** (`ConfirmDialog` nhận thêm `icon="warning"` hoặc `"success"`, `BaseModal` nhận `contentClass` và slot `header`) | Chỉ bật ở nơi truyền `icon`: ngừng / kích hoạt khách hàng, ngừng / bật đợt giảm giá. Các trang khác giữ hộp thoại cũ. Nút "Ngừng hoạt động" vẫn màu đỏ (nhóm đổi sang xanh, bản này giữ như cũ). Có thêm tên ẩn cho trình đọc màn hình. |
| **CORS cho mọi cổng của localhost** | `CorsConfig` dùng `allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")`: Vite tự nhảy sang cổng 5174, 5175... khi 5173 đang bận mà gọi API vẫn không bị chặn. |

**Không lấy** (bản của nhóm là bản cũ hơn hoặc kém hơn bản đang có): mật khẩu tạm của khách hàng lưu chữ thường (bản này vẫn băm BCrypt); SĐT 10-11 số và bỏ kiểm tra email / họ tên (bản này đúng 10 số, đầu số 03/05/07/08/09); ô ngày thường `type="date"` thay `DateInput`; màn hình xem khách hàng cho sửa trực tiếp và bỏ phần địa chỉ; các trang Đợt giảm giá, Hóa đơn, Nhân viên, Phiếu giảm giá bản cũ (thiếu các sửa theo lời cô); mất `GiaiPhongCong`, `MethodArgumentTypeMismatchException` handler và nhiều test. Hai file ghi chú `GIAI_THICH_NGAY_2.md` (nhắc `SanPhamFormModal.vue` đã không còn) và `GIAI_THICH_SAN_PHAM.md` (ghi "chưa làm quét CCCD" trong khi đã có), `KIEM_TRA_HOP_NHAT.txt` (biên bản gộp cũ) và `logo.jpg` (trùng `backend/src/main/resources/static/logo.jpg`) cũng không đưa vào để khỏi gây hiểu nhầm.

CSDL đã có sẵn: khởi động lại backend là cột `anh_dai_dien` tự được thêm; hoặc chạy lại `database/SD_17_1_cau_truc.sql`.

### Gộp lần 2 với bản mới của nhóm (15/10/2026)

Bản nhóm gửi lần 2 vẫn gần giống bản đang có (chỉ khác 17 file, phần lớn là bản cũ của Đợt giảm giá / Khách hàng). Cũng chỉ lấy phần **mới thật sự, nhỏ và an toàn**, viết lại cho khớp cách báo lỗi theo từng ô và có test:

| Lấy từ bản nhóm | Ghi chú |
|---|---|
| **Xóa khách hàng đã có hóa đơn / phiếu giảm giá** | `KhachHangService.xoa` bắt lỗi khóa ngoại của SQL Server và trả **409** câu dễ hiểu: "Khách hàng đã phát sinh hóa đơn hoặc phiếu giảm giá nên không thể xóa. Hãy chuyển sang trạng thái "Ngừng hoạt động"." (trước đây lộ lỗi SQL). |
| **Sổ địa chỉ: tối đa 10 địa chỉ / khách** | Backend chặn (400) và giao diện khóa nút "Thêm địa chỉ mới" kèm lời nhắc xóa bớt. Sửa / xóa địa chỉ vẫn dùng được khi đã đủ 10. |
| **Sổ địa chỉ: chặn địa chỉ trùng** | Cùng Tỉnh/Thành phố + Phường/Xã + địa chỉ cụ thể (không phân biệt hoa thường) + SĐT người nhận với một địa chỉ khác của khách thì báo 409 "Địa chỉ này đã có trong sổ địa chỉ của khách hàng." Sửa chính địa chỉ đó mà giữ nguyên nội dung thì không bị tính là trùng. |
| **Nút "Đặt làm địa chỉ mặc định" ở danh sách + `PUT /api/dia-chi/{id}/mac-dinh`** | Đặt mặc định ngay tại danh sách bằng nút ngôi sao, không phải mở form sửa; API riêng chỉ đổi cờ mặc định nên địa chỉ cũ nhập thiếu thông tin vẫn đặt được. |
| **Địa chỉ cụ thể phải có chữ hoặc số** | Chặn các giá trị như `---`, `...` (báo ngay dưới ô, cả ở form lẫn backend). |

**Không lấy:** lớp kiểm tra `KhachHangRules` / `khachHangValidate.js` chạy song song (báo lỗi một câu chung, tuổi tối thiểu 10, mật khẩu chữ thường, SĐT cho phép +84 / dấu cách) — bản này đã có `QuyTacDuLieu` + `kiemTra.js` báo từng ô, tuổi tối thiểu 15, SĐT đúng 10 số theo yêu cầu của cô; **không cho xóa địa chỉ mặc định** (bản này cho xóa rồi tự chuyển mặc định sang địa chỉ còn lại, tiện hơn); tên / SĐT người nhận để trống thì lấy của khách (bản này bắt nhập rõ, form đã điền sẵn tên + SĐT khách); `KhachHangAdd.vue` và route cũ; danh sách khách hàng bản cũ; **toàn bộ Đợt giảm giá** (bản nhóm tính theo ngày, bản này theo ngày giờ đến phút, chế độ CONG_DON; bản nhóm còn thêm API đổi chế độ chống trùng lúc chạy và kế hoạch sửa TH1/TH2/TH3 — chưa cần); hộp xác nhận xóa địa chỉ bằng `confirm()` của trình duyệt (bản này dùng `ConfirmDialog`); `GIAI_THICH_*.md`, `KIEM_TRA_HOP_NHAT.txt`, `logo.jpg` (như lần trước).

Test thêm: backend +8 (`KhachHangServiceTest` xóa khách, `DiaChiKhachHangControllerTest` giới hạn / trùng / ký tự), giao diện +10 (`frontend/tests/diaChiKhachHangModal.test.js`).

## Sửa lỗi + case khó từng module (13/10/2026)

Backend chặn thêm các trường hợp trước đây lọt qua (kể cả khi gọi thẳng API, không qua form). Test: `backend/src/test/.../*Test.java` (đến 16/10/2026 là 449 test không cần CSDL, cộng thêm `BackendApplicationTests` cần SQL Server).
- **Chung:** tham số sai kiểu (`/api/hoa-don/abc`, `?tuNgay=13-10-2026`) trả 400 kèm câu báo lỗi thay vì 500.
- **Hóa đơn:** *Từ ngày* sau *Đến ngày* → 400 (giao diện hiện đúng lý do); tìm mã có `%` / `_` không còn bị hiểu là ký tự đại diện; **hủy đơn trả số lượng về tồn kho** của từng biến thể (nhiều dòng cùng biến thể được cộng gộp).
- **Phiếu giảm giá:** không tặng phiếu cá nhân cho khách đã ngừng hoạt động (khách đã được tặng từ trước thì vẫn giữ); sửa phiếu không được dời ngày kết thúc sang một ngày khác trong quá khứ (giữ nguyên ngày cũ hoặc gia hạn thì được).
- **Đợt giảm giá:** thêm đợt có ngày kết thúc ở quá khứ → lỗi (backend + form); sửa: giống phiếu; mã đợt chỉ gồm chữ / số / `-` / `_` (2-20 ký tự); trạng thái chỉ nhận 0 / 1.
- **Khách hàng:** kiểm tra định dạng email, ngày sinh không ở tương lai / không quá 120 năm, giới tính / trạng thái chỉ 0 / 1, mã khách không dấu / không khoảng trắng; **sửa lỗi** chỉ đổi hoa thường của email (An@Mail.vn → an@mail.vn) bị báo "Email đã tồn tại". Địa chỉ khách hàng: tên người nhận, SĐT, tỉnh, phường/xã, địa chỉ cụ thể bắt buộc (chỉ gõ dấu cách cũng tính là trống).
  **Số điện thoại (khách hàng, người nhận, nhân viên) chỉ nhận đúng 10 chữ số, đầu 03 / 05 / 07 / 08 / 09** (từ 15/10/2026 cả ba dùng chung một bộ luật `QuyTacDuLieu`); ô nhập giới hạn 10 ký tự.
- **Nhân viên:** ngày sinh **bắt buộc, từ đủ 18 tuổi, tối đa 100 tuổi** (backend + form; ngày vào làm không sớm hơn sinh nhật thứ 18); gửi cùng một địa chỉ hai lần trong danh sách → lỗi (trước đây dòng sau đè dòng trước).
- **Sản phẩm / biến thể / thuộc tính:** danh mục vợt phải chọn độ cứng, điểm cân bằng, trọng lượng, chu vi thật; danh mục khác phải là "Không áp dụng" (dữ liệu cũ giữ nguyên thì không bắt sửa); sửa sản phẩm thành trùng tên + danh mục + thương hiệu với sản phẩm khác → 409; sản phẩm đã có biến thể không đổi qua lại giữa danh mục vợt và danh mục khác; chuyển biến thể sang sản phẩm khác danh mục bị kiểm tra lại; mục "Không áp dụng" không đổi tên / không ngưng được; đổi tên danh mục làm nó chuyển giữa vợt và loại khác khi đã có sản phẩm → 409.

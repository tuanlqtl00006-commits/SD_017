# Chạy project FootStyle (Thống kê, Quản lý hóa đơn, sản phẩm, phiếu giảm giá, đợt giảm giá, khách hàng, nhân viên)

1. SQL Server: mở `database/SD_17_tong.sql` trong SSMS rồi chạy MỘT LẦN (F5). File tổng này gồm sẵn theo đúng thứ tự: cấu trúc CSDL (tạo database
   `shop_cau_long_footstyle`), dữ liệu mẫu (vai trò, 10 nhân viên, 6 khách hàng, 11 phiếu giảm giá), thuộc tính + 6 sản phẩm + 14 biến thể, địa chỉ khách hàng +
   8 đợt giảm giá, phương thức thanh toán + hóa đơn mẫu, và phần nâng cấp CSDL cũ (chạy lại nhiều lần được, CSDL mới tạo thì không làm gì).
   File này giữ nguyên, không cần sửa. Muốn nạp lại hoặc thêm dữ liệu mẫu: dùng `database/SD_17_du_lieu.sql` (chỉ có dữ liệu, không tạo database / bảng, chạy lại nhiều lần được,
   có sẵn phần "THÊM DỮ LIỆU MỚI Ở ĐÂY" ở cuối file).
   **Đã có CSDL từ trước?** Không chạy lại cả file (database đã tồn tại) mà chỉ chạy phần 6 "Nâng cấp CSDL cũ" ở cuối `database/SD_17_tong.sql` (chạy lại nhiều lần được): bổ sung 2 cột `ngay_tao`,
   `ngay_cap_nhat` của bảng `dia_chi_khach_hang` mà backend quản lý địa chỉ khách hàng cần dùng. Script này cũng thêm cột `cccd` cho `nhan_vien` (số căn cước, không trùng; nếu CSDL từng có cột tên cũ `can_cuoc_cong_dan` thì số đã nhập được chép sang `cccd`), 2 bảng `vi_tri` và `nhan_vien_vi_tri` (vị trí làm việc nhiều-nhiều, kèm 5 vị trí mẫu), bảng `dia_chi_nhan_vien` (nhân viên có nhiều địa chỉ; địa chỉ cũ ở cột `dia_chi` tự được chuyển thành địa chỉ chính) cho chức năng thêm / sửa nhân viên, thêm cột `mo_ta` cho `dot_giam_gia` (ô Mô tả ở trang Thêm đợt giảm giá) và đổi dữ liệu mẫu giày cũ (nếu có) thành vợt cầu lông.
   Mật khẩu nhân viên mẫu: `Footstyle@123`.
   **Thuộc tính theo danh mục sản phẩm:** CSDL cần thêm 4 mục "Không áp dụng" (`DC000`, `CB000`, `TL000`, `CV000`) cho độ cứng, điểm cân bằng, trọng lượng, chu vi. `database/SD_17_tong.sql` đã có sẵn; CSDL cũ thì khởi động lại backend (tự bổ sung) hoặc chạy phần 6 của file tổng.
   **Gặp lỗi "Invalid column name 'cccd'" / "Invalid object name 'vi_tri'" / trang Hóa đơn báo "Không tải được danh sách hóa đơn"?** Nghĩa là CSDL được tạo từ bản SQL cũ (thiếu cột `nhan_vien.cccd` và bảng `vi_tri`, `nhan_vien_vi_tri`, `dia_chi_nhan_vien`). Cách xử lý: khởi động lại backend (backend tự bổ sung phần còn thiếu khi chạy và ghi dòng "Đã kiểm tra / nâng cấp cấu trúc CSDL" vào log), hoặc chạy phần 6 "Nâng cấp CSDL cũ" của `database/SD_17_tong.sql`. Phần dữ liệu mẫu trong file tổng cũng tự bổ sung phần thiếu trước khi chèn dữ liệu nên chạy trên CSDL cũ không còn báo lỗi.
2. Backend (Java 17, cổng 8081): sửa user/mật khẩu SQL Server trong `backend/src/main/resources/application.properties`
   (hoặc đặt biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), rồi `cd backend && ./mvnw spring-boot:run`.
   **Gửi mail phiếu giảm giá cá nhân (không bắt buộc):** muốn gửi mail thật thì đặt biến môi trường `SPRING_MAIL_HOST` (vd `smtp.gmail.com`),
   `SPRING_MAIL_PORT` (587), `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` (Gmail: dùng "Mật khẩu ứng dụng"), rồi bật
   `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true` và `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`. Chưa cấu hình thì hệ thống KHÔNG gửi mail thật,
   chỉ ghi nội dung mail vào log của backend (dòng `[MAIL GIẢ LẬP ...]`) để thử chức năng; khách chưa có email thì bị bỏ qua.
   **Đợt giảm giá chồng nhau:** một biến thể nằm trong nhiều đợt chạy cùng ngày thì mặc định lấy mức giảm CAO NHẤT (vd 10% và 15% → 15%).
   Muốn lấy TRUNG BÌNH CỘNG (10% và 15% → 12,5%) thì đặt `app.giam-gia.che-do-chong-dot=TRUNG_BINH` trong `application.properties`
   (hoặc biến môi trường `GIAM_GIA_CHE_DO=TRUNG_BINH`). Cả giá hiển thị ở danh sách sản phẩm / biến thể lẫn khung lịch giảm giá đều theo cấu hình này.
3. Frontend (cổng 5173): `cd frontend && npm install && npm run dev` (lần đầu cần `npm install` lại vì có thêm thư viện `jsqr` để đọc mã QR căn cước và bộ công cụ kiểm thử), mở http://localhost:5173.
   Kiểm thử giao diện: `cd frontend && npm test` (vitest, không cần backend).
   **Địa chỉ Tỉnh/Thành phố → Phường/Xã** (thêm khách hàng, địa chỉ khách hàng, địa chỉ nhân viên) lấy từ API đơn vị hành chính **sau sáp nhập 07/2025**
   `https://provinces.open-api.vn/api/v2` (không còn cấp Quận/Huyện) nên máy chạy giao diện cần có Internet. Địa chỉ cũ đã lưu (có Quận/Huyện) vẫn hiển thị bình thường,
   khi sửa thì giao diện nhắc chọn lại theo danh sách mới.
   **Quét căn cước (thêm nhân viên):** camera chỉ mở được khi truy cập bằng `http://localhost` hoặc https; nếu không có camera thì dùng "Tải ảnh QR" hoặc máy quét mã vạch cầm tay.

## Các trang trên giao diện (http://localhost:5173)
Vai trò chọn ở đầu trang (nhớ lại sau khi tải lại): **Quản lý** thấy toàn bộ menu, **Nhân viên** chỉ thấy Bán hàng tại quầy (chưa có trang), Quản lý hóa đơn, Quản lý khách hàng (theo sơ đồ use case).

| Đường dẫn | Trang |
|---|---|
| /thong-ke | Thống kê (tổng hợp từ danh sách hóa đơn, chỉ Quản lý) |
| /hoa-don, /hoa-don/{id} | Danh sách hóa đơn theo thiết kế: bộ lọc 1 hàng (mã hóa đơn, từ ngày - đến ngày, loại đơn Trực tuyến / Tại quầy, tự lọc khi đổi, nút Đặt lại), các tab trạng thái kèm SỐ hóa đơn từng tab (Tất cả, Chờ xác nhận, Đã xác nhận, Chờ giao hàng, Đang giao hàng, Đã giao hàng, Đã hoàn thành, Đã hủy, Hoàn tiền), bảng cột Mã HD / Mã NV / Tên KH / SĐT KH (lấy SĐT hồ sơ khách; khách chưa có SĐT hoặc khách vãng lai thì lấy SĐT người nhận ghi trên hóa đơn, vẫn không có thì hiện —) / Tổng tiền TT / Loại đơn / Ngày tạo / Trạng thái / Hành động, phân trang (5 dòng mỗi trang, không có ô chọn số dòng), xuất Excel toàn bộ danh sách đang lọc; trang chi tiết hóa đơn |
| /san-pham, /san-pham/them, /san-pham/{id}/sua | Sản phẩm: danh sách (hiện giá gốc gạch ngang + giá sau giảm nếu đang có đợt giảm giá), thêm, sửa. **Thuộc tính đổi theo Danh mục**: chọn *Vợt cầu lông* / *Vợt cầu lông người mới* thì có Độ cứng, Điểm cân bằng và biến thể theo Màu sắc + Trọng lượng + Chu vi cán; chọn *Phụ kiện*, *Túi - balo*, *Quần áo thể thao*, *Quả cầu lông* (mọi danh mục không phải vợt) thì ẩn các mục đó, chỉ còn Thương hiệu, Xuất xứ, Chất liệu và biến thể chỉ theo Màu sắc (hệ thống tự gán "Không áp dụng" cho phần ẩn, mục này không hiện trong ô chọn / danh sách thuộc tính). Trang Biến thể sản phẩm cũng theo quy tắc này |
| /san-pham/bien-the | Biến thể sản phẩm |
| /san-pham/{danh-muc, thuong-hieu, xuat-xu, chat-lieu, do-cung, diem-can-bang, mau-sac, trong-luong, chu-vi} | 9 bảng thuộc tính (menu "Danh sách thuộc tính") |
| /khach-hang, /khach-hang/them | Khách hàng (kèm địa chỉ). Chọn địa chỉ theo **địa giới sau sáp nhập 07/2025** (API v2 của provinces.open-api.vn: Tỉnh/Thành phố -> Phường/Xã, không còn Quận/Huyện), dùng cho trang Thêm khách hàng và cửa sổ Địa chỉ (cần có mạng internet). Địa chỉ cũ trước sáp nhập vẫn hiện đủ, khi sửa thì nhắc chọn lại |
| /dot-giam-gia, /dot-giam-gia/them, /dot-giam-gia/{id} | Đợt giảm giá: danh sách (lọc, xuất Excel, công tắc bật/tắt, xem chi tiết, nút "Sản phẩm áp dụng" để thêm / gỡ nhanh từng biến thể). Trạng thái hiển thị tính theo khoảng ngày: Sắp diễn ra / Đang diễn ra / Đã kết thúc / Ngừng hoạt động; công tắc còn hiện khi đợt chưa quá ngày kết thúc (đúng ngày kết thúc vẫn bật / tắt được), sang ngày hôm sau là "Đã kết thúc" và công tắc biến mất. Khi soạn đợt (thêm / sửa) có khung "Lịch giảm giá khi đợt chồng nhau" xem trước theo dữ liệu đang nhập: biến thể nằm trong đợt khác cùng thời gian thì liệt kê từng khoảng ngày kèm mức Cao nhất / Trung bình, ô xanh là mức đang áp dụng. Trang xem / sửa còn có khung "Lịch giảm giá theo ngày" lấy từ backend (đã lưu), tính cả các đợt khác chồng lên (chế độ MAX hoặc TRUNG_BINH theo cấu hình). Trang thêm / xem / sửa: mã tự sinh dạng ngắn theo số thứ tự (DGG009, nút làm mới), tên, %, từ ngày - đến ngày, mô tả, chọn sản phẩm - biến thể áp dụng (lọc theo màu, trọng lượng, chu vi cán), hộp xác nhận trước khi lưu |
| /phieu-giam-gia, /phieu-giam-gia/them, /phieu-giam-gia/{id}/sua | Phiếu giảm giá: công tắc bật/tắt (hiện khi phiếu chưa quá ngày kết thúc). Phiếu cá nhân gửi mail khi tạo (mail phiếu mới) và khi sửa phiếu còn "Sắp diễn ra" (phiếu đã / đang diễn ra thì sửa không gửi mail): đổi thông tin phiếu → khách giữ lại nhận mail cập nhật; thêm khách → mail phiếu mới; bỏ khách → mail hủy; chỉ đổi danh sách khách (thông tin phiếu giữ nguyên) → khách giữ lại không nhận mail |
| /nhan-vien, /nhan-vien/them, /nhan-vien/{id} | Nhân viên. Thêm nhân viên: **mã tự tạo theo tên** (Nguyễn Hoàng Long -> `LongNH01`, trùng dạng mã thì `LongNH02`...), hiện ngay khi nhập họ tên; nút **Quét căn cước** đọc mã QR trên thẻ CCCD (camera / chọn ảnh / máy quét mã vạch) và điền họ tên, ngày sinh, giới tính, số CCCD (địa chỉ trên thẻ hiện làm gợi ý khi nhập địa chỉ cụ thể); **nhiều địa chỉ** (Tỉnh/Thành phố → Phường/Xã), chọn 1 địa chỉ chính, chỉ thêm / sửa / chọn lại, **không có nút xóa địa chỉ**; **Vị trí làm việc**: chọn nhiều vị trí đã thêm hoặc thêm vị trí mới (không có chức năng xóa vị trí). Sửa nhân viên được đổi: họ tên, giới tính, ngày sinh, SĐT, số CCCD, địa chỉ, vai trò, vị trí; không đổi: mã, email, ngày vào làm, mật khẩu. Danh sách nhân viên có cột Vị trí, tìm theo mã / tên / email / SĐT / CCCD |

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
| PUT | /api/phieu-giam-gia/{id}/trang-thai | Ẩn / hiện phiếu (không xóa dữ liệu) |
| GET | /api/nhan-vien, /api/nhan-vien/{id} | Danh sách / chi tiết nhân viên |
| POST / PUT | /api/nhan-vien[/{id}] | Thêm: nhập đủ (mật khẩu băm BCrypt, không trả về); `cccd` (12 số, không trùng) và `idViTri[]` (nhiều vị trí) không bắt buộc; `diaChis[]` = `{ id, tinhThanh, phuongXa, diaChiCuThe, macDinh }` (id null = địa chỉ mới, đúng 1 địa chỉ chính, không xóa địa chỉ nào; cột `dia_chi` luôn là địa chỉ chính ghép một dòng; vẫn nhận chuỗi `diaChi` kiểu cũ); **mã nhân viên do backend tự sinh theo họ tên** (`Tên` + chữ cái đầu họ/đệm + số thứ tự 2 chữ số, `MaNhanVienUtil`). Sửa: chỉ đổi họ tên, giới tính, ngày sinh, SĐT, cccd, địa chỉ, vai trò, vị trí; mã, email, ngày vào làm, mật khẩu giữ nguyên |
| GET / POST | /api/vi-tri | Danh sách vị trí làm việc / thêm vị trí mới (body `{ten}`). Không có API xóa vị trí |
| PUT | /api/nhan-vien/{id}/trang-thai | Ẩn / hiện nhân viên (không xóa dữ liệu) |
| GET | /api/vai-tro | Danh sách vai trò |
| GET | /api/khach-hang?search=, /api/khach-hang/{id} | Danh sách (tìm theo mã, tên, SĐT, email) / chi tiết khách hàng (kèm danh sách địa chỉ) |
| POST / PUT | /api/khach-hang[/{id}] | Thêm (kèm địa chỉ mặc định nếu gửi tinhThanh, phuongXa, diaChiCuThe; `quanHuyen` không còn dùng cho địa chỉ mới vì đã sáp nhập) / sửa. Mật khẩu tạm được băm, không trả về |
| PUT | /api/khach-hang/{id}/trang-thai | Bật / tắt hoạt động khách hàng |
| GET | /api/dia-chi/khach-hang/{idKhachHang} | Địa chỉ của khách hàng |
| POST / PUT / DELETE | /api/dia-chi/khach-hang/{idKhachHang}, /api/dia-chi/{id} | Thêm / sửa / xóa địa chỉ (luôn có đúng 1 địa chỉ mặc định) |
| GET | /api/dot-giam-gia | Danh sách đợt giảm giá, đợt mới thêm nằm trên cùng |
| GET | /api/dot-giam-gia/ma-moi | Mã đợt do hệ thống sinh theo số thứ tự (DGG009), không trùng mã đã có |
| GET | /api/dot-giam-gia/{id} | Chi tiết một đợt giảm giá |
| POST / PUT | /api/dot-giam-gia[/{id}] | Thêm / sửa đợt giảm giá. Body: `maDot, tenDot, phanTramGiamDot, ngayBatDau, ngayKetThuc, moTa, idBienThe[]` (nhận cả tên cũ `idBienThes`; ngày dạng `yyyy-MM-dd` hoặc `yyyy-MM-ddTHH:mm:ss`). Lưu đợt và đồng bộ biến thể áp dụng trong một lần (thêm bắt buộc có ít nhất 1 biến thể đang bán; sửa không gửi `idBienThe` thì giữ nguyên biến thể; sửa không đổi mã đợt). Đổi % của đợt thì áp % mới cho mọi biến thể; không đổi % thì giữ mức giảm riêng của từng biến thể |
| PUT | /api/dot-giam-gia/{id}/trang-thai | Bật / tắt hoạt động đợt giảm giá (1: đang hoạt động, 0: ngừng hoạt động). Đợt đã qua ngày kết thúc thì không đổi được (409) |
| GET | /api/dot-giam-gia/{id}/lich-giam-gia | Lịch giảm giá theo khoảng ngày của từng biến thể trong đợt, có tính các đợt đang bật chồng lên (mỗi đoạn: `tuNgay, denNgay, phanTramApDung` = mức áp dụng theo cấu hình MAX / TRUNG_BINH, `phanTramCaoNhat`, `phanTramTrungBinh`, `cacDot`, `giaSauGiam`) |
| GET | /api/dot-giam-gia/ap-dung | Mức giảm của từng biến thể theo các đợt đang bật mà chưa kết thúc `{ chinhSach, muc: [{ idBienThe, idDot, maDot, tenDot, phanTram, ngayBatDau, ngayKetThuc }] }`; trang soạn đợt dùng để xem trước lịch chồng đợt |
| GET | /api/dot-giam-gia/ap-dung-hom-nay | Mức giảm đang áp dụng hôm nay của từng biến thể `{ idBienThe: phanTram }` (nhiều đợt chồng nhau thì gộp theo MAX / TRUNG_BINH; dùng cho giá gạch ngang ở danh sách sản phẩm / biến thể) |
| GET | /api/giam-gia/ap-dung?ngay=yyyy-MM-dd | Mức giảm của từng biến thể tại một ngày bất kỳ (bỏ `ngay` thì lấy hôm nay): `{ cheDo, ngay, phanTram: { idBienThe: % } }` |
| GET | /api/giam-gia/bien-the/{id}/lich | Lịch giảm giá theo khoảng ngày của MỘT biến thể (mọi đợt đang bật chứa biến thể đó) |
| GET / POST / DELETE | /api/dot-giam-gia-chi-tiet/campaign/{id}, /api/dot-giam-gia-chi-tiet/{id} | Biến thể áp dụng trong đợt giảm giá (chọn từ /api/bien-the-san-pham) |
| GET | /api/san-pham, /api/san-pham/{id} | Danh sách / chi tiết sản phẩm (kèm ảnh chính, ảnh phụ) |
| POST / PUT | /api/san-pham[/{id}] | Thêm / sửa sản phẩm (sửa không đổi mã). Thêm có thể gửi kèm `bienThes[]` (màu, trọng lượng, chu vi, giá bán, tồn; mã biến thể tự sinh) để lưu sản phẩm + biến thể trong một lần. Sản phẩm đã tồn tại (cùng tên + danh mục + thương hiệu) → trả 409 mã `SAN_PHAM_DA_TON_TAI`; gửi lại với `xacNhanCapNhat = true` để cập nhật sản phẩm cũ (biến thể chưa có thì thêm, đã có thì cập nhật giá + tồn) |
| PUT | /api/san-pham/{id}/trang-thai | Ngưng / kích hoạt bán sản phẩm (không xóa dữ liệu) |
| GET | /api/bien-the-san-pham[/{id}] | Danh sách / chi tiết biến thể (bảng san_pham_chi_tiet) |
| POST / PUT | /api/bien-the-san-pham[/{id}] | Thêm: nếu sản phẩm đã có biến thể cùng màu + trọng lượng + chu vi → 409 mã `BIEN_THE_DA_TON_TAI`, gửi lại với `xacNhanCapNhat = true` để cập nhật giá + tồn. Thêm / sửa biến thể (khi sửa được đổi sản phẩm, vd SP008 → SP007; mã không sửa tay, chỉ phần đầu mã tự đổi theo sản phẩm mới) |
| PUT | /api/bien-the-san-pham/{id}/trang-thai | Ngưng / kích hoạt biến thể |
| GET / POST | /api/thuoc-tinh/{loai} | Danh sách / thêm thuộc tính. `loai` = danh-muc, thuong-hieu, xuat-xu, chat-lieu, do-cung, diem-can-bang, mau-sac, trong-luong, chu-vi |
| PUT | /api/thuoc-tinh/{loai}/{id} | Sửa tên thuộc tính (mã giữ nguyên) |
| PUT | /api/thuoc-tinh/{loai}/{id}/trang-thai | Ngưng / kích hoạt thuộc tính |

Lưu ý: hệ thống không xóa cứng dữ liệu. Nút "Ẩn" chỉ đổi cột `trang_thai`, có thể "Hiện lại" bất cứ lúc nào.

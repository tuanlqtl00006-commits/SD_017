# Giải thích phần Quản lý sản phẩm (để trả lời thầy cô)

## 1. Các file và nhiệm vụ

| File | Làm gì |
|---|---|
| `services/sanPhamService.js` | Gọi API backend `/api/san-pham` và `/api/bien-the-san-pham`: lấy / thêm / sửa / đổi trạng thái. |
| `services/thuocTinhService.js` | Gọi API `/api/thuoc-tinh/{loai}` cho 9 bảng thuộc tính (danh mục, thương hiệu, ...). |
| `components/SanPhamManager.vue` | Trang danh sách: tải dữ liệu, lọc, phân trang, bảng, xuất Excel, xem chi tiết, ngưng / kích hoạt. |
| `components/sanPham/SanPhamFormPage.vue` | Trang riêng Thêm / Sửa sản phẩm (có nút "Quay lại danh sách" như video), kiểm tra dữ liệu nhập, tải ảnh từ máy. |
| `components/sanPham/SanPhamDetailModal.vue` | Xem chi tiết sản phẩm và danh sách biến thể. |
| `utils/text.js` | `includesText` (tìm không phân biệt dấu), `layTen` (id -> tên), `layLuaChon` (danh sách cho dropdown). |

## 2. Luồng chạy của trang

1. Trang mở ra -> `onMounted` gọi `taiDuLieu()`: lấy sản phẩm, biến thể và 9 bảng thuộc tính.
2. `sanPhamKemThongKe` (computed) tính thêm cho mỗi sản phẩm: số biến thể, tổng tồn, giá thấp nhất (chỉ tính biến thể đang hoạt động).
3. `danhSachDaLoc` (computed) lọc theo từ khóa, danh mục, thương hiệu, trạng thái.
4. `dongHienThi` (computed) cắt đúng các dòng của trang hiện tại bằng `slice`.
5. Bấm "Thêm sản phẩm" -> chuyển sang trang `/san-pham/them`; bấm "Chỉnh sửa" trong màn chi tiết -> chuyển sang `/san-pham/:id/sua`. Trang form tự tải dữ liệu, kiểm tra, gọi service rồi quay về danh sách (sản phẩm mới nằm đầu danh sách).
6. Bấm nút nguồn -> hộp xác nhận -> `toggleActive` đổi `hoatDong` (không xóa dữ liệu).

## 3. Câu hỏi thầy cô hay hỏi

**Vì sao dùng `ref` và `reactive`?**
Hai cách làm biến "có phản ứng" của Vue: khi giá trị đổi thì giao diện tự cập nhật. `ref` cho 1 giá trị đơn (`page`, `dangTai`), `reactive` cho 1 object (`filters`, `form`). Với `ref` trong script phải viết `.value`, trong template thì không.

**`computed` khác hàm thường chỗ nào?**
`computed` tự tính lại khi dữ liệu nó dùng thay đổi và nhớ kết quả (cache). Ví dụ đổi từ khóa tìm kiếm -> `danhSachDaLoc` tự cập nhật -> bảng tự đổi, không cần gọi tay.

**`watch` dùng để làm gì?**
Để chạy một việc khi dữ liệu đổi. Ở đây: đổi bộ lọc hoặc đổi số dòng/trang thì về trang 1 (tránh đang ở trang 3 mà lọc chỉ còn 1 trang). Trong form: sau khi bấm Lưu, người dùng sửa ô nào thì kiểm tra lại ngay.

**`async/await` và `try/catch/finally`?**
Gọi API backend mất thời gian nên dùng `await` để chờ. `try` chạy bình thường, `catch` bắt lỗi (ví dụ trùng mã) rồi báo toast, `finally` luôn chạy để tắt trạng thái "đang tải / đang lưu".

**Component cha và con nói chuyện thế nào?**
Cha -> con bằng `props` (`item`, `all`, `thuoc-tinh`, `saving`). Con -> cha bằng `emit` (`save`, `close`, `edit`). Con không tự sửa dữ liệu của cha.

**Vì sao form dùng `v-if="hienForm"`?**
Form chỉ tồn tại khi mở. Mỗi lần mở là một form mới, nên giá trị ban đầu (rỗng hoặc dữ liệu cũ) luôn đúng.

**Phân biệt thêm và sửa ra sao?**
Biến `spDangSua`: `null` là thêm mới, có giá trị là đang sửa sản phẩm đó. Khi sửa thì ô Mã bị khóa (`readonly`) vì mã không được đổi.

**Vì sao không xóa sản phẩm?**
Sản phẩm đã có biến thể, hóa đơn... xóa sẽ mất dữ liệu liên quan. Hệ thống chỉ đổi cột trạng thái (`hoatDong`), có thể kích hoạt lại.

**Giá "từ" lấy ở đâu?**
Là giá bán thấp nhất trong các biến thể đang hoạt động của sản phẩm. Chưa có biến thể thì hiện dấu "—".

**Biến thể là gì, vì sao không được trùng?**
Theo ERD, biến thể = sản phẩm + màu sắc + trọng lượng + chu vi (bảng `san_pham_chi_tiet`). Cùng bộ 4 giá trị mà tạo 2 lần là trùng nên `laTrungBienThe` chặn lại.

**Dữ liệu lưu ở đâu?**
Lưu trong SQL Server (bảng `san_pham`, `san_pham_chi_tiet`, `hinh_anh_san_pham` và 9 bảng thuộc tính), tải lại trang không mất. Frontend chỉ gọi API trong `services/`, mọi quy tắc quan trọng được backend kiểm tra lại (frontend kiểm tra chỉ để báo lỗi nhanh cho người dùng).

**Validate làm những gì?**
Mã: bắt buộc, 3-20 ký tự chữ/số, không trùng. Tên: bắt buộc, tối đa 255 ký tự (khớp cột SQL). 6 ô chọn: bắt buộc. Ảnh: không bắt buộc, tối đa 10 ảnh (gồm ảnh chính), ảnh đầu tiên là ảnh chính. Backend chỉ nhận ảnh tải lên từ máy (`/api/uploads/...`) hoặc link http(s).

**Các quy tắc nghiệp vụ backend giữ (hợp ERD)?**
- Danh mục, thương hiệu, xuất xứ, chất liệu, độ cứng, điểm cân bằng, màu sắc, trọng lượng, chu vi: khi chọn mới thì mục đó phải đang hoạt động. Sản phẩm cũ đang dùng mục đã ngưng thì vẫn sửa được nếu không đổi mục đó.
- Biến thể: sản phẩm phải đang hoạt động mới thêm được biến thể mới hoặc kích hoạt lại biến thể. Không trùng mã, không trùng bộ (sản phẩm, màu, trọng lượng, chu vi). Giá bán từ 1.000 ₫, tồn kho là số nguyên không âm.
- Sửa biến thể: được đổi sản phẩm (vd SP008 → SP007), nhưng mã biến thể không sửa tay được (ô mã bị khóa, backend bỏ qua mã gửi lên). Sản phẩm mới phải đang hoạt động và chưa có biến thể cùng màu + trọng lượng + chu vi. Khi đổi sản phẩm, phần đầu mã (vd `SP008-`) tự đổi theo (`SP007-`); mã mới không được trùng biến thể khác.
- Không xóa cứng sản phẩm, biến thể, thuộc tính vì đã có dữ liệu liên quan (hóa đơn chi tiết, đợt giảm giá chi tiết). Chỉ đổi cột `trang_thai`.
- Ảnh: bảng `hinh_anh_san_pham` có đúng 1 ảnh `la_anh_chinh = 1` cho mỗi sản phẩm. Ảnh bị bỏ khỏi form được chuyển `trang_thai = 0`, không xóa dòng.

## 4. Lombok ở backend (câu hỏi có thể gặp)

- `@Getter @Setter` trên entity: Lombok tự sinh getter/setter lúc biên dịch, nên file không còn viết tay.
- `@RequiredArgsConstructor` trên service/controller: Lombok tự sinh constructor nhận tất cả field `private final`. Spring dùng constructor này để tiêm (inject) repository/service vào (Dependency Injection qua constructor).
- Không dùng `@Data` cho entity vì `equals/hashCode/toString` tự sinh có thể gây lỗi với JPA.

## 5. Backend sản phẩm (SanPhamService, BienTheService, ThuocTinhService)

- Entity khớp ERD: `SanPham` (6 khóa ngoại tới thuộc tính), `SanPhamChiTiet` (khóa ngoại tới sản phẩm, màu, trọng lượng, chu vi), `HinhAnhSanPham`.
- 9 bảng thuộc tính cùng cấu trúc nên dùng chung lớp cha `ThuocTinh` (`@MappedSuperclass`) và `ThuocTinhRepository`. Mỗi bảng chỉ là một class rỗng, tên cột `ma_...`, `ten_...` khai báo bằng `@AttributeOverride`. Một `ThuocTinhService` và một `ThuocTinhController` phục vụ cả 9 bảng.
- `ThuocTinhChon.chon(...)`: kiểm tra một thuộc tính được chọn (có tồn tại, mục mới thì phải đang hoạt động), dùng cho cả sản phẩm và biến thể.
- Danh sách sản phẩm lấy ảnh bằng 1 câu truy vấn cho tất cả sản phẩm rồi gom theo id, để không bị gọi DB lặp từng dòng.

## 6. Service backend (NhanVienService, PhieuGiamGiaService)

- Tên hàm theo kiểu bài mẫu của lớp: `getAll`, `getById`, `them`, `sua`, `doiTrangThai` (không có `xoa` vì hệ thống chỉ ẩn / hiện bằng cột trạng thái).
- Dùng vòng `for` và `List` thay cho `stream`, `Set` để dễ đọc.
- Hàm `loi(dieuKien, thongBao)`: nếu điều kiện đúng thì báo lỗi 400. Nhờ vậy mỗi quy tắc kiểm tra chỉ cần 1 dòng.
- Luồng: Controller nhận request -> Service kiểm tra dữ liệu (`napDuLieu`) -> Repository lưu DB -> Service đổi entity thành Response trả về.

## 4. Tải ảnh từ máy (trang Thêm / Sửa sản phẩm)

**Tải ảnh chạy ra sao?**
1. Admin bấm "Thêm ảnh" và chọn file. Frontend kiểm tra nhanh (JPG, PNG, WEBP, GIF, tối đa 5 MB, tối đa 10 ảnh).
2. `hinhAnhService.upload` gửi file (multipart) tới `POST /api/hinh-anh/upload`.
3. Backend (`HinhAnhService`) kiểm tra lại dung lượng, đọc vài byte đầu file để biết đúng định dạng (không tin đuôi file), lưu vào thư mục `uploads/` với tên ngẫu nhiên, trả về `/api/uploads/<tên>.jpg`.
4. Đường dẫn được thêm vào danh sách ảnh của form. Khi bấm Lưu, ảnh đầu tiên gửi đi là `anhChinh`, các ảnh còn lại là `anhPhu`, backend lưu vào bảng `hinh_anh_san_pham` như cũ (không đổi cấu trúc bảng).
5. `WebConfig` cho phép xem ảnh qua `GET /api/uploads/<tên>`; Vite đã proxy `/api` nên `<img>` hiển thị bình thường.

**Vì sao lưu tên ngẫu nhiên mà không giữ tên gốc?**
Tránh trùng tên làm ghi đè ảnh cũ, và tránh tên file có ký tự lạ (đường dẫn kiểu `../`).

**Điểm còn hạn chế (nói thật nếu thầy cô hỏi)?**
Ảnh đã tải lên nhưng admin bỏ ra trước khi lưu vẫn nằm trong thư mục `uploads/` (chưa có bước dọn). Backend chưa có đăng nhập nên API upload chưa kiểm tra quyền.

## 5. Trang Thêm / Chi tiết nhân viên (giống video)

- Thêm nhân viên là trang riêng `/nhan-vien/them`; bấm nút mắt ở danh sách mở trang `/nhan-vien/:id` (xem và sửa cùng một trang). File: `components/nhanVien/NhanVienFormPage.vue`.
- Bên trái là thẻ hồ sơ (chữ cái đầu của họ tên, email, trạng thái) và nút Khóa / Mở khóa tài khoản (đổi cột `trang_thai`, không xóa dữ liệu). Bên phải là thông tin cơ bản và địa chỉ.
- Quy định sửa không đổi: không sửa mã, email, ngày vào làm; mật khẩu chỉ nhập lúc thêm mới.
- Danh sách chỉ còn một ô tìm kiếm chung (mã, họ tên, email, SĐT) cùng bộ lọc vai trò và trạng thái, như video.
- Chưa làm vì ERD bảng `nhan_vien` không có cột tương ứng: ảnh đại diện tải lên, quét CCCD, chọn tỉnh / xã riêng (ERD chỉ có một cột `dia_chi`), đổi mật khẩu.

## 6. Hiệu ứng khi rê chuột

Nằm ở cuối file `frontend/src/assets/admin.css` (mục "Hiệu ứng khi rê chuột"), dùng chung cho mọi màn hình:
- Mục menu bên trái: nền xanh nhạt, chữ và icon chuyển xanh, trượt nhẹ sang phải.
- Nút lớn: nhấc lên 1px và bóng đậm hơn; bấm xuống thì hạ lại. Nút xanh có bóng xanh.
- Nút tròn nhỏ trong bảng: phóng to nhẹ, bấm thì thu lại.
- Dòng trong bảng: nền xanh rất nhạt và vạch xanh bên trái.
- Ô nhập, ô chọn: viền chuyển xanh khi rê chuột; nút Nam / Nữ và phân trang cũng đổi màu nhẹ.
- Người dùng bật "giảm chuyển động" trong hệ điều hành thì các hiệu ứng chuyển động tự tắt.


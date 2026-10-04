# Ngày 2: Quản lý sản phẩm – THÊM + SỬA

Phạm vi hôm nay: hiển thị danh sách, **thêm sản phẩm**, **sửa sản phẩm**.
Chưa làm (các ngày sau): lọc, phân trang, ngưng/kích hoạt, xem chi tiết, xuất Excel.

## Các file của ngày 2

| File | Làm gì |
|---|---|
| `frontend/src/components/SanPhamManager.vue` | Trang chính: tải danh sách, bảng, nút Thêm / Sửa, gọi service khi lưu. |
| `frontend/src/components/sanPham/SanPhamFormModal.vue` | Form dùng chung cho thêm và sửa, kiểm tra dữ liệu nhập. |
| `frontend/src/services/sanPhamService.js` | `getAll`, `create` (POST), `update` (PUT) gọi `/api/san-pham`. |
| `backend/.../controller/SanPhamController.java` | 4 đường dẫn: GET danh sách, GET theo id, POST thêm, PUT sửa. |
| `backend/.../service/SanPhamService.java` | `them`, `sua`, kiểm tra dữ liệu (`napDuLieu`), lưu ảnh (`dongBoAnh`). |

## Luồng THÊM
1. Bấm **Thêm sản phẩm** -> `moFormThem()`: `spDangSua = null`, mở form (mã tự gợi ý SP00x).
2. Bấm **Thêm sản phẩm** trong form -> `kiemTra()` báo lỗi từng ô; hợp lệ thì `emit('save', dữ liệu)`.
3. Trang chính nhận `save` -> `luuSanPham()` -> `sanPhamService.create()` -> `POST /api/san-pham`.
4. Backend `SanPhamService.them()`: kiểm tra lại (mã không trùng, tên, 6 thuộc tính, ảnh) -> lưu `san_pham` -> lưu `hinh_anh_san_pham`.
5. Thành công: đóng form, toast, tải lại danh sách.

## Luồng SỬA
1. Bấm nút bút chì trên dòng -> `moFormSua(sp)`: `spDangSua = sp`, mở form với dữ liệu cũ.
2. Form biết đang sửa nhờ `isEdit` (`item !== null`): ô **Mã bị khóa** (`readonly`), nút đổi thành "Lưu thay đổi".
3. `luuSanPham()` thấy `spDangSua` có giá trị -> `sanPhamService.update(id, dữ liệu)` -> `PUT /api/san-pham/{id}`.
4. Backend `SanPhamService.sua()`: tìm sản phẩm theo id, kiểm tra lại, **giữ nguyên mã** (chỉ xử lý mã khi thêm mới), cập nhật `ngay_cap_nhat`.

## Thuộc tính được sửa / không được sửa
- Sửa được: tên, danh mục, thương hiệu, xuất xứ, chất liệu, độ cứng, điểm cân bằng, mô tả, ảnh chính, ảnh phụ.
- Không sửa: **mã sản phẩm** (định danh), ngày tạo (hệ thống ghi).

## Câu hỏi thầy cô có thể hỏi
- **Làm sao biết form đang thêm hay sửa?** Prop `item`: `null` là thêm, có giá trị là sửa.
- **Vì sao kiểm tra cả ở frontend lẫn backend?** Frontend báo lỗi nhanh cho người dùng; backend là lớp bảo vệ thật vì có thể gọi API không qua form.
- **Vì sao sau khi lưu phải tải lại danh sách?** Để thấy dữ liệu mới nhất từ database (kể cả id, ngày tạo do backend cấp).
- **Ảnh lưu thế nào?** Bảng `hinh_anh_san_pham`, mỗi sản phẩm có đúng 1 ảnh chính. Ảnh bị bỏ khi sửa được chuyển `trang_thai = 0`, không xóa dòng.

# Chạy project FootStyle (phần Quản lý sản phẩm + Phiếu giảm giá + Nhân viên)

1. SQL Server: chạy `database/SD_17.sql`, sau đó `database/SD_17_du_lieu_mau.sql` (vai trò, 10 nhân viên, 6 khách hàng, 11 phiếu giảm giá)
   và `database/SD_17_du_lieu_san_pham.sql` (9 bảng thuộc tính, 6 sản phẩm, 14 biến thể, chỉ mục chống trùng biến thể).
   Mật khẩu nhân viên mẫu: `Footstyle@123`.
2. Backend (Java 17, cổng 8081): sửa user/mật khẩu SQL Server trong `backend/src/main/resources/application.properties`
   (hoặc đặt biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), rồi `cd backend && ./mvnw spring-boot:run`.
3. Frontend (cổng 5173): `cd frontend && npm install && npm run dev`, mở http://localhost:5173.

## API
| Phương thức | Đường dẫn | Chức năng |
|---|---|---|
| GET | /api/phieu-giam-gia | Danh sách phiếu |
| GET | /api/phieu-giam-gia/{id} | Chi tiết (kèm khách hàng được tặng) |
| GET | /api/phieu-giam-gia/khach-hang | Khách hàng có thể chọn khi tặng phiếu cá nhân |
| POST / PUT | /api/phieu-giam-gia[/{id}] | Tạo / sửa |
| PUT | /api/phieu-giam-gia/{id}/trang-thai | Ẩn / hiện phiếu (không xóa dữ liệu) |
| GET | /api/nhan-vien, /api/nhan-vien/{id} | Danh sách / chi tiết nhân viên |
| POST / PUT | /api/nhan-vien[/{id}] | Thêm: nhập đủ (mật khẩu băm BCrypt, không trả về). Sửa: chỉ đổi họ tên, giới tính, ngày sinh, SĐT, địa chỉ, vai trò; mã, email, ngày vào làm, mật khẩu giữ nguyên |
| PUT | /api/nhan-vien/{id}/trang-thai | Ẩn / hiện nhân viên (không xóa dữ liệu) |
| GET | /api/vai-tro | Danh sách vai trò |
| GET | /api/san-pham, /api/san-pham/{id} | Danh sách / chi tiết sản phẩm (kèm ảnh chính, ảnh phụ) |
| POST / PUT | /api/san-pham[/{id}] | Thêm / sửa sản phẩm (sửa không đổi mã) |
| PUT | /api/san-pham/{id}/trang-thai | Ngưng / kích hoạt bán sản phẩm (không xóa dữ liệu) |
| GET | /api/bien-the-san-pham[/{id}] | Danh sách / chi tiết biến thể (bảng san_pham_chi_tiet) |
| POST / PUT | /api/bien-the-san-pham[/{id}] | Thêm / sửa biến thể (khi sửa được đổi sản phẩm, vd SP008 → SP007; mã không sửa tay, chỉ phần đầu mã tự đổi theo sản phẩm mới) |
| PUT | /api/bien-the-san-pham/{id}/trang-thai | Ngưng / kích hoạt biến thể |
| GET / POST | /api/thuoc-tinh/{loai} | Danh sách / thêm thuộc tính. `loai` = danh-muc, thuong-hieu, xuat-xu, chat-lieu, do-cung, diem-can-bang, mau-sac, trong-luong, chu-vi |
| PUT | /api/thuoc-tinh/{loai}/{id} | Sửa tên thuộc tính (mã giữ nguyên) |
| PUT | /api/thuoc-tinh/{loai}/{id}/trang-thai | Ngưng / kích hoạt thuộc tính |

Lưu ý: hệ thống không xóa cứng dữ liệu. Nút "Ẩn" chỉ đổi cột `trang_thai`, có thể "Hiện lại" bất cứ lúc nào.

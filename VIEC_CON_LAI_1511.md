# Việc còn lại sau ngày 15/11 (rà lại từ KE_HOACH_SUA_1411.md, KE_HOACH_SUA_1411_GOP.md, video 22:22 và ghi chú cũ)

Đã đối chiếu từng mục với code hiện tại. Mục nào đã làm thì không ghi lại ở đây.

## A. Còn sửa được — ĐÃ LÀM hết ngày 15/11 (chưa commit)

1. **Bán quầy: báo kết quả khi thêm / đổi số lượng** (R1 [06:52]). Nhập sai thì đã báo lỗi; thêm thành công thì chưa báo gì.
   → Đã làm: thêm / đổi số lượng thành công thì báo "Đã thêm 2 × … vào HD…", "Đã đổi số lượng … thành …".
2. **Tab "Đơn tại quầy": ghi rõ hình thức mua** (R1 [11:32], cô nói "tùy"). Cột "Loại đơn" đang ghi "Tại quầy" cho cả đơn mua trực tiếp lẫn đơn quầy có giao hàng. Thêm "Hình thức mua": Mua tại quầy / Giao tận nơi, suy ra từ dữ liệu sẵn có, không đổi SQL.
   → Đã làm: cột "Hình thức mua" (Mua tại quầy / Giao tận nơi) ở tab Đơn tại quầy và file Excel; tab online giữ "Loại đơn". Không đổi SQL.
3. **Đơn chuyển khoản tự nhảy "Đã xác nhận"** (R1 [30:53], cô hỏi "tại sao lại là đã xác nhận?"). Tiền về thì hệ thống tự xác nhận đơn và trừ kho, người làm ghi "HỆ THỐNG". Sửa: tiền về chỉ ghi nhận "đã thanh toán", bước xác nhận vẫn do nhân viên bấm.
   → Đã làm: tiền về (VietQR / giả lập) chỉ ghi "Xác nhận thanh toán", đơn vẫn Chờ xác nhận, chưa trừ kho; nhân viên bấm "Xác nhận đơn".
4. **Ô mã giảm giá ở trang thanh toán phải hiện mã đang áp** (R1 [26:19]). Hiện chỉ thấy ở dòng tiền phía dưới, ô mã trống.
   → Đã làm: dòng "Đang áp dụng mã … · giảm …" ngay trên ô mã, có nút X bỏ mã.
5. **Phiếu online hết lượt vẫn được giảm** (ghi chú 28/10). Đặt hàng online bỏ qua kết quả trừ lượt phiếu: 2 khách cùng dùng lượt cuối thì cả 2 đều được giảm.
   → Đã làm: trừ lượt không được (lượt cuối vừa bị người khác dùng) thì báo "Mã … vừa hết lượt", không đặt đơn với giá giảm.
6. **Sửa đơn làm đơn dưới mức tối thiểu thì bỏ phiếu nhưng không trả lại lượt** (ghi chú 28/10).
   → Đã làm: bỏ phiếu thì trả lại lượt (và phiếu cá nhân của khách), lịch sử ghi "Trả lại lượt phiếu …".
7. **Xác nhận đã hoàn tiền chưa gửi thư cho khách** (GOP mục B, CK [08:21]: "khách nhận được email"). Thư "sẽ hoàn tiền" đã có lúc hủy; thư "đã hoàn tiền" khi nhân viên bấm xác nhận thì chưa.
   → Đã làm: bấm "Xác nhận đã hoàn tiền" thì gửi thư "Đã hoàn tiền" (số tiền, mã giao dịch), lịch sử ghi đã gửi tới email nào.
8. **Màn quầy còn 2 hàm không ai gọi** (`xoaTrangDiaChiGiao`, `moThemDiaChiMoi`) sau khi gộp nút địa chỉ.
   → Đã làm: bỏ 2 hàm.
9. **Địa chỉ / hotline cửa hàng ghi 2 kiểu** (GOP mục D): chân trang, trang Liên hệ ghi "123 Đường Cầu Diễn… 0977 508 430", còn trợ lý ảo đọc từ `application.properties`. Thống nhất về một chỗ.
   → Đã làm: trợ lý ảo dùng cùng địa chỉ / hotline / giờ mở cửa với chân trang (123 Đường Cầu Diễn, 0977 508 430, 8:00 - 21:30); trang Hướng dẫn bỏ câu "bất kỳ showroom".
10. **Case khó tồn kho: quầy bán nốt hàng của đơn online chờ thanh toán** (R1 [29:22–34:07]). Đơn online chưa trừ kho nên quầy vẫn bán được. Hướng xử lý đã có từ hôm nay (không hủy đơn, báo hết hàng, mail chăm sóc khách). Còn thiếu: ở quầy báo cho nhân viên biết sản phẩm đang nằm trong đơn online chờ xử lý.
   → Đã làm: bảng chọn sản phẩm ở quầy hiện "N chờ đơn online" dưới tồn kho; thêm quá phần còn trống thì nhắc nhân viên (vẫn cho bán vì đơn online chưa trừ kho).

## B. Không sửa được vì nghe không rõ (cần hỏi lại người dự buổi review)

- R1 [26:29–26:41] "Con phông đâu… chưa kiểm…".
- R1 [26:41–26:53] cô hỏi mail ("mail đâu, gửi mail chưa").
- R1 [32:57–33:26] đoạn trước khi cô bấm xác nhận HD73.
- Video 22:22 [10:24] "Sửa thống kê nhiều chữ": không rõ chỗ nào.

## C. Đã cân nhắc, giữ nguyên (ghi để nhóm biết)

- Tra cứu đơn khi chưa đăng nhập: che người nhận / SĐT / địa chỉ — cô nói "tùy".
- Video 22:22 [07:24] "free ship nên là mã giảm giá": trái với màn Cài đặt free ship cô yêu cầu.
- Ngưỡng free ship 2.000.000 ₫ là số tạm, quản lý tự sửa ở Cài đặt free ship.
- Phí ship Hà Nội và "hết hàng không hủy" là sửa vào phần Phương Minh: cần báo bạn ấy (không phải việc code).

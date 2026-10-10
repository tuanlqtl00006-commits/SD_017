package com.footstyle.demo.dto;

/**
 * xacNhanCapNhat: khi THÊM mà sản phẩm đã có biến thể cùng màu + trọng lượng + chu vi thì backend trả 409 mã BIEN_THE_DA_TON_TAI;
 * gửi lại với xacNhanCapNhat = true để cập nhật giá bán / số lượng tồn của biến thể đã có thay vì tạo mới.
 *
 * Thêm / sửa biến thể (san_pham_chi_tiet). Khi sửa: được đổi sản phẩm; trường ma bị bỏ qua (mã không sửa tay, chỉ phần đầu mã đổi theo sản phẩm mới). */
public record BienTheRequest(
        Integer idSanPham,
        Integer idMauSac,
        Integer idTrongLuong,
        Integer idChuVi,
        String ma,
        Long giaBan,
        Integer soLuongTon,
        Boolean xacNhanCapNhat) {
}

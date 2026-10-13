package com.footstyle.demo.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Dữ liệu thêm / sửa khách hàng. Các trường địa chỉ (tinhThanh... diaChiCuThe) chỉ dùng khi thêm mới:
 * nếu có thì tạo luôn địa chỉ mặc định cho khách hàng.
 */
@Getter
@Setter
public class KhachHangRequest {
    private String maKhachHang;
    private String hoTen;
    private String sdt;
    private String email;
    private LocalDate ngaySinh;
    /** 0: Nữ, 1: Nam. */
    private Integer gioiTinh;
    private Integer trangThai;

    private String tenNguoiNhan;
    private String sdtNguoiNhan;
    private String tinhThanh;
    private String quanHuyen;
    private String phuongXa;
    private String diaChiCuThe;
}

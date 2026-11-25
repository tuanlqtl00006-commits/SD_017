package com.footstyle.demo.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HoaDonRequest {
    private Integer id;
    private String maHoaDon;
    private String tenKhachHang;
    private String sdtKhachHang;
    private String maNhanVien;
    private String tenNhanVien;
    private BigDecimal tongTien;
    /** Tiền hàng sau giảm giá, không gồm phí vận chuyển (dùng cho thống kê doanh thu). */
    private BigDecimal doanhThu;
    private LocalDateTime ngayTao;
    private String loaiDon;
    /** Hình thức mua (cô review 14/11 [11:32]): "Mua tại quầy" / "Giao tận nơi" (bán quầy có giao hàng) / "Đặt online". */
    private String hinhThucMua;
    /** Tên phương thức thanh toán (góp ý 25/11, ghi giống chi tiết hóa đơn): "Chuyển khoản VietQR", "Chuyển khoản ngân hàng", "Tiền mặt"... */
    private String hinhThucThanhToan;
    private Integer trangThai;
}
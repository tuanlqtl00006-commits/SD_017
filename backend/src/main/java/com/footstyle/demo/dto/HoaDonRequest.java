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
    private LocalDateTime ngayTao;
    private String loaiDon;
    private Integer trangThai;
}
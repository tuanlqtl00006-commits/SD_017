package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "hoa_don_chi_tiet")
@Getter
@Setter
public class HoaDonChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    @com.fasterxml.jackson.annotation.JsonIgnore // THÊM ĐÚNG DÒNG NÀY VÀO ĐÂY!
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_san_pham_chi_tiet") // <-- Kẻ tình nghi số 1 thường bị ghi nhầm thành id_san_pham
    private SanPhamChiTiet sanPhamChiTiet;

    private Integer soLuong;
    private BigDecimal donGia;
}

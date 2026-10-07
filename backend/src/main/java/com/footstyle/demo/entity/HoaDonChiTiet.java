package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Bảng hoa_don_chi_tiet: mỗi dòng là một biến thể sản phẩm trong hóa đơn. */
@Entity
@Table(name = "hoa_don_chi_tiet")
@Getter
@Setter
public class HoaDonChiTiet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private HoaDon hoaDon;

    @ManyToOne
    @JoinColumn(name = "id_san_pham_chi_tiet")
    private SanPhamChiTiet sanPhamChiTiet;

    @Column(name = "so_luong")
    private Integer soLuong;

    /** Giá bán thực tế lúc mua (đã trừ đợt giảm giá nếu có). */
    @Column(name = "don_gia")
    private BigDecimal donGia;

    @Column(name = "thanh_tien")
    private BigDecimal thanhTien;

    @Column(name = "trang_thai")
    private Integer trangThai;
}

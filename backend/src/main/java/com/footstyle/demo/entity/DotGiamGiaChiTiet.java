package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dot_giam_gia_chi_tiet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DotGiamGiaChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dot_giam_gia", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DotGiamGia dotGiamGia;

    @Column(name = "id_san_pham_chi_tiet", nullable = false)
    private Long idSanPhamChiTiet; // Temporary mapping as ID until SanPhamChiTiet is fully implemented

    @Column(name = "phan_tram_giam_bien_the", nullable = false)
    private Integer phanTramGiamBienThe;

    @Column(name = "trang_thai")
    private Integer trangThai = 1;
}

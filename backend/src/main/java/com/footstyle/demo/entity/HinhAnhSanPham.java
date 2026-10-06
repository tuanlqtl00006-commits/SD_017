package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "hinh_anh_san_pham")
public class HinhAnhSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_san_pham")
    private SanPham sanPham;

    @Column(name = "duong_dan_hinh_anh")
    private String duongDanHinhAnh;

    @Column(name = "la_anh_chinh")
    private Boolean laAnhChinh;

    @Column(name = "trang_thai")
    private Integer trangThai;

    public static final int HOAT_DONG = 1;        
    public static final int NGUNG_HOAT_DONG = 0;
}

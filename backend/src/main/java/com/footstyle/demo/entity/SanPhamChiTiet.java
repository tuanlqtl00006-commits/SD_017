package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "san_pham_chi_tiet")
public class SanPhamChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_san_pham") 
    private SanPham sanPham;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_mau_sac")
    private MauSac mauSac;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_trong_luong")
    private TrongLuong trongLuong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_chu_vi")
    private ChuVi chuVi;

    @Column(name = "ma_spct")
    private String maSpct;

    @Column(name = "gia_ban")
    private Long giaBan;

    @Column(name = "so_luong_ton")
    private Integer soLuongTon;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "trang_thai")
    private Integer trangThai;

    public static final int HOAT_DONG = 1;        
    public static final int NGUNG_HOAT_DONG = 0;
}

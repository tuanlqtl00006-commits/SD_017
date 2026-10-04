package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hoa_don")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_hoa_don")
    private String maHoaDon;

    
    @ManyToOne
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    private LocalDateTime ngayTao;
    private BigDecimal tongTien;
    private BigDecimal phiVanChuyen;
    private BigDecimal tienGiamGia;
    @Column(name = "loai_hoa_don")
    private Integer loaiDon;
    private Integer trangThai; 

    @Column(name = "dia_chi_giao_hang")
    private String diaChiGiaoHang;

    private String ghiChu;

    
    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL)
    private List<HoaDonChiTiet> danhSachChiTiet;
}

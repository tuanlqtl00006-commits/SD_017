package com.footstyle.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Địa chỉ của nhân viên (bảng dia_chi_nhan_vien). Một nhân viên có nhiều địa chỉ, đúng 1 địa chỉ chính.
 * Địa chỉ theo đơn vị hành chính sau sáp nhập: Tỉnh/Thành phố -> Phường/Xã (không có Quận/Huyện).
 * Địa chỉ không bị xóa: chỉ thêm, sửa hoặc chọn lại địa chỉ chính.
 */
@Entity
@Getter
@Setter
@Table(name = "dia_chi_nhan_vien")
public class DiaChiNhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @Column(name = "tinh_thanh", columnDefinition = "NVARCHAR(100)")
    private String tinhThanh;

    @Column(name = "phuong_xa", columnDefinition = "NVARCHAR(100)")
    private String phuongXa;

    @Column(name = "dia_chi_chi_tiet", columnDefinition = "NVARCHAR(500)")
    private String diaChiCuThe;

    @Column(name = "la_mac_dinh")
    private Boolean macDinh;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @PrePersist
    void truocKhiThem() {
        ngayTao = LocalDateTime.now();
        ngayCapNhat = ngayTao;
        if (trangThai == null) trangThai = 1;
        if (macDinh == null) macDinh = false;
    }

    @PreUpdate
    void truocKhiSua() {
        ngayCapNhat = LocalDateTime.now();
    }
}

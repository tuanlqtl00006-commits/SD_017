package com.footstyle.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dia_chi_khach_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaChiKhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang")
    @JsonIgnore
    private KhachHang khachHang;

    @Column(name = "ho_ten_nguoi_nhan", columnDefinition = "NVARCHAR(255)")
    private String tenNguoiNhan;

    @Column(name = "sdt_nguoi_nhan", length = 15)
    private String sdtNguoiNhan;

    @Column(name = "tinh_thanh", columnDefinition = "NVARCHAR(100)")
    private String tinhThanhPho;

    @Column(name = "quan_huyen", columnDefinition = "NVARCHAR(100)")
    private String quanHuyen;

    @Column(name = "phuong_xa", columnDefinition = "NVARCHAR(100)")
    private String phuongXa;

    @Column(name = "dia_chi_chi_tiet", columnDefinition = "NVARCHAR(255)")
    private String diaChiCuThe;

    @Column(name = "la_mac_dinh")
    private Boolean laDiaChiMacDinh;

    @Column(name = "trang_thai")
    private Integer trangThai;

    // Optional: Keep ngayTao/ngayCapNhat if we want, or remove them if they aren't in the DB natively. 
    // Wait, the DB didn't originally have ngay_tao and ngay_cap_nhat!
    // Let's remove them to prevent issues, or leave them since Hibernate already added them.
    // I will leave them mapped to the new columns Hibernate just added.
    public Integer getTrangThai() { return trangThai; }
    public void setTrangThai(Integer trangThai) { this.trangThai = trangThai; }
}

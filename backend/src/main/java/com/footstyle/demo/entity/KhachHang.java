package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "khach_hang")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KhachHang {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ma_khach_hang", unique = true, nullable = false)
    private String maKH;
    
    @Column(name = "ho_ten", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String ten;
    
    @Column(name = "sdt", length = 15)
    private String sdt;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;
    
    @Column(name = "gioi_tinh")
    private Integer gioiTinh;
    
    @Column(name = "mat_khau")
    private String matKhau;
    
    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;
    
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;
    
    @Column(name = "trang_thai")
    private Integer trangThai;
    
    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private java.util.List<DiaChiKhachHang> diaChiList = new java.util.ArrayList<>();

    @Transient
    private String tinhThanh;

    @Transient
    private String quanHuyen;

    @Transient
    private String phuongXa;

    @Transient
    private String diaChiCuThe;

    public java.util.List<DiaChiKhachHang> getDiaChiList() { return diaChiList; }
    public void setDiaChiList(java.util.List<DiaChiKhachHang> diaChiList) { this.diaChiList = diaChiList; }
    public String getTinhThanh() { return tinhThanh; }
    public void setTinhThanh(String tinhThanh) { this.tinhThanh = tinhThanh; }
    public String getQuanHuyen() { return quanHuyen; }
    public void setQuanHuyen(String quanHuyen) { this.quanHuyen = quanHuyen; }
    public String getPhuongXa() { return phuongXa; }
    public void setPhuongXa(String phuongXa) { this.phuongXa = phuongXa; }
    public String getDiaChiCuThe() { return diaChiCuThe; }
    public void setDiaChiCuThe(String diaChiCuThe) { this.diaChiCuThe = diaChiCuThe; }

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
        ngayCapNhat = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }
}

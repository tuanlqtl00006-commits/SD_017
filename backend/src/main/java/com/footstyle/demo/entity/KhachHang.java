package com.footstyle.demo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "khach_hang")
public class KhachHang {

    /** Tuổi tối thiểu của khách hàng: từ đủ 15 tuổi (Bộ luật Dân sự 2015, Điều 21 khoản 4). Xem QuyTacDuLieu. */
    public static final int TUOI_TOI_THIEU = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "ma_khach_hang")
    private String maKhachHang;

    @Column(name = "ho_ten")
    private String hoTen;

    @Column(name = "sdt")
    private String sdt;

    @Column(name = "email")
    private String email;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    /** 0: Nữ, 1: Nam. */
    @Column(name = "gioi_tinh")
    private Integer gioiTinh;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    /** Chỉ ghi, không bao giờ trả ra API. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "mat_khau")
    private String matKhau;

    @Column(name = "trang_thai")
    private Integer trangThai;

    /** Ảnh đại diện: đường dẫn ảnh đã tải lên (/api/uploads/...) hoặc link http(s). Có thể để trống. */
    @Column(name = "anh_dai_dien")
    private String anhDaiDien;

    @OneToMany(mappedBy = "khachHang", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private java.util.List<DiaChiKhachHang> diaChiList = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (ngayTao == null) ngayTao = now;
        ngayCapNhat = now;
        if (trangThai == null) trangThai = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }
}

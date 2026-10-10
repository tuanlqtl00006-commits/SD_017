package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "nhan_vien")
public class NhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_vai_tro")
    private VaiTro vaiTro;

    @Column(name = "ma_nhan_vien")
    private String maNhanVien;

    @Column(name = "ho_ten")
    private String hoTen;

    @Column(name = "gioi_tinh")
    private Integer gioiTinh;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "sdt")
    private String sdt;

    @Column(name = "email")
    private String email;

    @Column(name = "mat_khau")
    private String matKhau;

    @Column(name = "dia_chi")
    private String diaChi;

    /** Số căn cước công dân (12 số), có thể nhập tay hoặc quét mã QR trên thẻ. */
    @Column(name = "cccd")
    private String cccd;

    @Column(name = "ngay_vao_lam")
    private LocalDate ngayVaoLam;

    /** Các vị trí làm việc (một nhân viên có thể có nhiều vị trí). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "nhan_vien_vi_tri",
            joinColumns = @JoinColumn(name = "id_nhan_vien"),
            inverseJoinColumns = @JoinColumn(name = "id_vi_tri"))
    @OrderBy("id ASC")
    private Set<ViTri> viTriList = new LinkedHashSet<>();

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "trang_thai")
    private Integer trangThai;

    public static final int HOAT_DONG = 1;
    public static final int NGUNG_HOAT_DONG = 0;
    public static final int GIOI_TINH_NU = 0;
    public static final int GIOI_TINH_NAM = 1;

    @PrePersist
    @PreUpdate
    void capNhatThoiGian() {
        this.ngayCapNhat = LocalDateTime.now();
    }
}

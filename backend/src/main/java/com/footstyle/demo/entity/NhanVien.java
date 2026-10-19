package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /** Địa chỉ chính ghép thành một dòng (chép từ địa chỉ được chọn trong bảng dia_chi_nhan_vien). */
    @Column(name = "dia_chi")
    private String diaChi;

    /** Đường dẫn ảnh đại diện nhân viên (khớp cột anh trong sơ đồ ERD). */
    @Column(name = "anh")
    private String anh;

    /** Số căn cước công dân (12 số), có thể nhập tay hoặc quét mã QR trên thẻ. */
    @Column(name = "can_cuoc_cong_dan")
    private String canCuocCongDan;

    @Column(name = "ngay_vao_lam")
    private LocalDate ngayVaoLam;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "trang_thai")
    private Integer trangThai;

    /** Nhân viên phải từ đủ 18 tuổi: dưới 18 là "người lao động chưa thành niên" (Bộ luật Lao động 2019, Điều 143). Xem QuyTacDuLieu. */
    public static final int TUOI_TOI_THIEU = 18;
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

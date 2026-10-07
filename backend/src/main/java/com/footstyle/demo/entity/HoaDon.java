package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Bảng hoa_don. Tên cột khớp với database/SD_17.sql (ddl-auto=none nên phải khớp đúng). */
@Entity
@Table(name = "hoa_don")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class HoaDon {

    // Mã trạng thái hóa đơn (cột trang_thai)
    public static final int DA_HUY = 0;
    public static final int CHO_XAC_NHAN = 1;
    public static final int DA_XAC_NHAN = 2;
    public static final int CHO_LAY_HANG = 3;
    public static final int DANG_GIAO = 4;
    public static final int DA_GIAO = 5;
    public static final int HOAN_THANH = 6;

    // Loại hóa đơn (cột loai_hoa_don)
    public static final int TAI_QUAY = 1;
    public static final int GIAO_HANG = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_hoa_don")
    private String maHoaDon;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia;

    @Column(name = "id_phuong_thuc_thanh_toan")
    private Integer idPhuongThucThanhToan;

    @Column(name = "loai_hoa_don")
    private Integer loaiDon;

    @Column(name = "tong_tien")
    private BigDecimal tongTien;

    @Column(name = "so_tien_giam")
    private BigDecimal soTienGiam;

    @Column(name = "phi_van_chuyen")
    private BigDecimal phiVanChuyen;

    @Column(name = "thanh_tien")
    private BigDecimal thanhTien;

    @Column(name = "don_vi_van_chuyen")
    private String donViVanChuyen;

    @Column(name = "ho_ten_nguoi_nhan")
    private String hoTenNguoiNhan;

    @Column(name = "sdt_nguoi_nhan")
    private String sdtNguoiNhan;

    @Column(name = "dia_chi_giao_hang")
    private String diaChiGiaoHang;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "ngay_giao_du_kien")
    private LocalDateTime ngayGiaoDuKien;

    @Column(name = "ngay_giao_thuc_te")
    private LocalDateTime ngayGiaoThucTe;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "trang_thai")
    private Integer trangThai;

    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<HoaDonChiTiet> danhSachChiTiet;
}

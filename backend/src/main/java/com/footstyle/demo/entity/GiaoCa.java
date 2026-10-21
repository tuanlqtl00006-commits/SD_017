package com.footstyle.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Bảng giao_ca: một lần làm ca thực tế. Nhận ca (tiền đầu ca) -> bán hàng tại quầy (hóa đơn gắn id_giao_ca) -> kết ca:
 * hệ thống tổng kết, nhân viên đếm tiền mặt thực tế, để lại tiền bàn giao cho ca sau.
 */
@Entity
@Getter
@Setter
@Table(name = "giao_ca")
public class GiaoCa {

    public static final int DANG_TRONG_CA = 1;
    public static final int DA_GIAO_CA = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "ma_giao_ca")
    private String maGiaoCa;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_nhan_vien")
    private NhanVien nhanVien;

    /** Ca trong lịch làm việc mà lần nhận ca này thuộc về (null = nhận ca ngoài lịch). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_lich_lam_viec")
    private LichLamViec lichLamViec;

    @Column(name = "thoi_gian_nhan_ca")
    private LocalDateTime thoiGianNhanCa;

    @Column(name = "tien_dau_ca")
    private BigDecimal tienDauCa;

    @Column(name = "ghi_chu_nhan_ca")
    private String ghiChuNhanCa;

    @Column(name = "so_phut_di_muon")
    private Integer soPhutDiMuon;

    @Column(name = "thoi_gian_ket_ca")
    private LocalDateTime thoiGianKetCa;

    @Column(name = "so_hoa_don")
    private Integer soHoaDon;

    @Column(name = "doanh_thu")
    private BigDecimal doanhThu;

    @Column(name = "tien_mat_thu")
    private BigDecimal tienMatThu;

    @Column(name = "chuyen_khoan_thu")
    private BigDecimal chuyenKhoanThu;

    /** Tiền mặt phải có trong két theo hệ thống = tiền đầu ca + tiền mặt thu trong ca. */
    @Column(name = "tien_mat_he_thong")
    private BigDecimal tienMatHeThong;

    @Column(name = "tien_mat_thuc_te")
    private BigDecimal tienMatThucTe;

    /** Thực tế - hệ thống (âm = thiếu tiền, dương = thừa tiền). */
    @Column(name = "chenh_lech")
    private BigDecimal chenhLech;

    /** Tiền mặt để lại trong két cho ca sau (gợi ý thành tiền đầu ca của lần nhận ca kế tiếp). */
    @Column(name = "tien_ban_giao")
    private BigDecimal tienBanGiao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nhan_vien_nhan_ban_giao")
    private NhanVien nhanVienNhanBanGiao;

    @Column(name = "ghi_chu_ket_ca")
    private String ghiChuKetCa;

    @Column(name = "trang_thai")
    private Integer trangThai;
}

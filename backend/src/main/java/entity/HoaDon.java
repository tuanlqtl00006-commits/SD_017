package entity;
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

    // Quan hệ với Khách Hàng và Nhân Viên
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
    private Integer loaiDon; // 1: Tại quầy, 2: Giao hàng
    private Integer trangThai; // 0: Chờ xác nhận, 1: Đã xác nhận, 2: Đang giao, 3: Hoàn thành...

    @Column(name = "dia_chi_giao_hang")
    private String diaChiGiaoHang;

    private String ghiChu;

    // Quan hệ 1-N với Hóa Đơn Chi Tiết
    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL)
    private List<HoaDonChiTiet> danhSachChiTiet;
}
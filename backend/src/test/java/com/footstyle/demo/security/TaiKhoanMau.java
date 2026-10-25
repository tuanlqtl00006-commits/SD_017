package com.footstyle.demo.security;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.NhanVien;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.repository.GiaoCaRepository;
import com.footstyle.demo.repository.KhachHangRepository;
import com.footstyle.demo.repository.NhanVienRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Dữ liệu tài khoản mẫu + kho giả dùng chung cho các test đăng nhập / phân quyền. */
public class TaiKhoanMau {

    public static final String MAT_KHAU = "Footstyle@123";

    public final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public final List<NhanVien> nhanViens = new ArrayList<>();
    public final List<KhachHang> khachs = new ArrayList<>();
    /** Id nhân viên CHƯA vào ca (mặc định mọi người đều đang trong ca). */
    public final Set<Integer> chuaVaoCa = new HashSet<>();
    public final TokenXacThuc token = new TokenXacThuc("chuoi-bi-mat-de-test-123456", Duration.ofHours(12), Clock.systemUTC());

    public final NhanVien quanLy;
    public final NhanVien nhanVien;
    public final NhanVien nghiViec;
    public final KhachHang khach;

    public TaiKhoanMau() {
        VaiTro ql = new VaiTro();
        ql.setId(1);
        ql.setTenVaiTro(VaiTro.TEN_QUAN_LY);
        VaiTro nv = new VaiTro();
        nv.setId(2);
        nv.setTenVaiTro(VaiTro.TEN_NHAN_VIEN);
        quanLy = taoNv(1, "NV01", "Nguyễn Hoàng Long", "long.nh@footstyle.vn", "0912000001", ql, 1);
        nhanVien = taoNv(3, "NV03", "Lê Quốc Bảo", "bao.lq@footstyle.vn", "0912000003", nv, 1);
        nghiViec = taoNv(10, "NV10", "Đặng Mai Hương", "huong.dm@footstyle.vn", "0912000010", nv, 0);
        khach = new KhachHang();
        khach.setId(5);
        khach.setMaKhachHang("KH05");
        khach.setHoTen("Hoàng Đức Anh");
        khach.setEmail("anh.hd@gmail.com");
        khach.setSdt("0901000005");
        khach.setMatKhau(encoder.encode(MAT_KHAU));
        khach.setTrangThai(1);
        khachs.add(khach);
    }

    private NhanVien taoNv(int id, String ma, String ten, String email, String sdt, VaiTro vt, int tt) {
        NhanVien n = new NhanVien();
        n.setId(id);
        n.setMaNhanVien(ma);
        n.setHoTen(ten);
        n.setEmail(email);
        n.setSdt(sdt);
        n.setVaiTro(vt);
        n.setTrangThai(tt);
        n.setMatKhau(encoder.encode(MAT_KHAU));
        nhanViens.add(n);
        return n;
    }

    public NhanVienRepository nhanVienRepo() {
        return KhoGia.tao(NhanVienRepository.class, (ten, a) -> switch (ten) {
            case "findById" -> nhanViens.stream().filter(n -> n.getId().equals(a[0])).findFirst();
            case "findAll" -> new ArrayList<>(nhanViens);
            case "findFirstByEmailIgnoreCase" -> nhanViens.stream().filter(n -> n.getEmail().equalsIgnoreCase((String) a[0])).findFirst();
            case "findFirstBySdt" -> nhanViens.stream().filter(n -> n.getSdt().equals(a[0])).findFirst();
            case "findFirstByMaNhanVienIgnoreCase" -> nhanViens.stream().filter(n -> n.getMaNhanVien().equalsIgnoreCase((String) a[0])).findFirst();
            case "findByTrangThaiOrderByHoTenAsc" -> nhanViens.stream().filter(n -> n.getTrangThai().equals(a[0])).toList();
            case "save" -> a[0];
            default -> KhoGia.KHONG_CO;
        });
    }

    public KhachHangRepository khachHangRepo() {
        return KhoGia.tao(KhachHangRepository.class, (ten, a) -> switch (ten) {
            case "findById" -> khachs.stream().filter(k -> k.getId().equals(a[0])).findFirst();
            case "findFirstByEmailIgnoreCase" -> khachs.stream().filter(k -> k.getEmail() != null && k.getEmail().equalsIgnoreCase((String) a[0])).findFirst();
            case "findFirstBySdt" -> khachs.stream().filter(k -> a[0].equals(k.getSdt())).findFirst();
            case "save" -> a[0];
            default -> KhoGia.KHONG_CO;
        });
    }

    public XacThucInterceptor interceptor(boolean batDangNhap) {
        return new XacThucInterceptor(token, nhanVienRepo(), khachHangRepo(), giaoCaRepo(), batDangNhap);
    }

    public GiaoCaRepository giaoCaRepo() {
        return KhoGia.tao(GiaoCaRepository.class, (ten, a) -> switch (ten) {
            case "findFirstByNhanVienIdAndTrangThai" -> chuaVaoCa.contains(a[0]) ? Optional.empty() : Optional.of(new GiaoCa());
            default -> KhoGia.KHONG_CO;
        });
    }

    public String tokenCua(NhanVien n) {
        return token.tao(NguoiDung.LOAI_NHAN_VIEN, n.getId(), n.getMatKhau());
    }

    public static Optional<NhanVien> khong() {
        return Optional.empty();
    }
}

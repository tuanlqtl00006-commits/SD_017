package com.footstyle.demo.service;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import com.footstyle.demo.repository.SanPhamChiTietRepository;
import com.footstyle.demo.service.GiamGiaChongDot.CheDo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tính mức giảm giá của từng biến thể theo KHOẢNG THỜI GIAN (ngày giờ) khi nhiều đợt giảm giá chồng lên nhau.
 *
 * Ví dụ: đợt A giảm 10% từ 1/10 08:00 đến 10/10 17:00 cho SPCT1, SPCT5; đợt B giảm 15% từ 4/10 12:00 đến 20/10 23:59 cho SPCT1, SPCT3, SPCT4.
 * Với SPCT1 kết quả là 3 đoạn:
 *   1/10 08:00 - 4/10 11:59   : chỉ đợt A             -> 10%
 *   4/10 12:00 - 10/10 17:00  : đợt A và B chồng nhau -> MAX = 15% (cộng dồn 25%, trung bình cộng 12,5%)
 *   10/10 17:01 - 20/10 23:59 : chỉ đợt B             -> 15%
 *
 * Quy ước: khi nhiều đợt cùng áp dụng cho một biến thể trong một đoạn thì mức áp dụng (phanTramApDung) tính theo
 * cấu hình app.giam-gia.che-do-chong-dot: MAX (mặc định) = lấy mức cao nhất, CONG_DON = cộng dồn (tối đa 100%),
 * TRUNG_BINH = trung bình cộng các đợt. Cả ba mức (cao nhất, cộng dồn, trung bình cộng) đều được trả kèm để đối chiếu.
 * Chỉ tính các đợt đang bật (trang_thai = 1) và các dòng chi tiết đang bật.
 */
@Service
@RequiredArgsConstructor
public class LichGiamGiaService {

    public record DotTrongDoan(Long idDot, String maDot, String tenDot, int phanTram) {
    }

    /** Một đoạn thời gian liên tiếp (tuLuc, denLuc đều tính) có cùng tập đợt áp dụng. */
    public record DoanGiam(LocalDateTime tuLuc, LocalDateTime denLuc, double phanTramApDung, int phanTramCaoNhat,
                           double phanTramCongDon, double phanTramTrungBinh, List<DotTrongDoan> cacDot, Long giaSauGiam) {
    }

    public record LichBienThe(Long idBienThe, String maSpct, String tenSanPham, Long giaBan, List<DoanGiam> doan) {
    }

    private final DotGiamGiaRepository dotRepo;
    private final DotGiamGiaChiTietRepository chiTietRepo;
    private final SanPhamChiTietRepository bienTheRepo;
    private final GiamGiaApDungService apDungService; // giữ cấu hình MAX / TRUNG_BINH và tính giảm giá hôm nay

    /** Một dòng "đợt X giảm p% cho biến thể này từ lúc a đến lúc b". */
    private record MucGiam(DotGiamGia dot, int phanTram, LocalDateTime tu, LocalDateTime den) {
    }

    /* ===================== Lịch giảm giá của một đợt ===================== */

    /**
     * Với mỗi biến thể của đợt: chia khoảng thời gian của đợt thành các đoạn có mức giảm khác nhau
     * (có tính cả các đợt khác đang chồng lên).
     */
    @Transactional(readOnly = true)
    public List<LichBienThe> lichCuaDot(Long idDot) {
        DotGiamGia dot = dotRepo.findById(idDot)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đợt giảm giá."));
        LocalDateTime tu = dot.getNgayBatDau();
        LocalDateTime den = dot.getNgayKetThuc();

        Set<Long> idBienThes = new HashSet<>();
        List<Long> thuTu = new ArrayList<>();
        for (DotGiamGiaChiTiet ct : chiTietRepo.findByDotGiamGiaId(idDot)) {
            if (dangBat(ct.getTrangThai()) && idBienThes.add(ct.getIdSanPhamChiTiet())) {
                thuTu.add(ct.getIdSanPhamChiTiet());
            }
        }

        Map<Long, List<MucGiam>> mucTheoBienThe = gomMucGiam(idBienThes);
        CheDo cheDo = apDungService.cheDo();
        List<LichBienThe> ketQua = new ArrayList<>();
        for (Long idBt : thuTu) {
            SanPhamChiTiet bt = bienTheRepo.findById(idBt.intValue()).orElse(null);
            if (bt == null) {
                continue;
            }
            List<DoanGiam> doan = chiaDoan(mucTheoBienThe.getOrDefault(idBt, new ArrayList<>()), tu, den, bt.getGiaBan(), cheDo);
            ketQua.add(new LichBienThe(idBt, bt.getMaSpct(),
                    bt.getSanPham() == null ? null : bt.getSanPham().getTenSanPham(), bt.getGiaBan(), doan));
        }
        return ketQua;
    }

    /* ===================== Mức giảm đang áp dụng ngay lúc này ===================== */

    /**
     * { idBienThe -> % giảm ĐANG áp dụng (tính tới giờ phút hiện tại) } (nhiều đợt cùng áp dụng thì gộp theo MAX / CONG_DON /
     * TRUNG_BINH đã cấu hình). Đợt bắt đầu 18:00 thì trước 18:00 chưa giảm. Biến thể không giảm thì không có trong map.
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> giamHomNay() {
        return apDungService.phanTramTheoBienThe(LocalDateTime.now());
    }

    /* ===================== Hàm phụ ===================== */

    private boolean dangBat(Integer trangThai) {
        return trangThai == null || trangThai == 1;
    }

    // Gom mọi dòng giảm giá (của các đợt đang bật) theo từng biến thể. locIdBienThe = null: lấy tất cả biến thể.
    private Map<Long, List<MucGiam>> gomMucGiam(Set<Long> locIdBienThe) {
        Map<Long, DotGiamGia> dotDangBat = new HashMap<>();
        for (DotGiamGia d : dotRepo.findAll()) {
            if (d.getTrangThai() != null && d.getTrangThai() == 1) {
                dotDangBat.put(d.getId(), d);
            }
        }
        Map<Long, List<MucGiam>> ketQua = new LinkedHashMap<>();
        for (DotGiamGiaChiTiet ct : chiTietRepo.findAll()) {
            if (!dangBat(ct.getTrangThai()) || ct.getDotGiamGia() == null) {
                continue;
            }
            DotGiamGia d = dotDangBat.get(ct.getDotGiamGia().getId());
            if (d == null || (locIdBienThe != null && !locIdBienThe.contains(ct.getIdSanPhamChiTiet()))) {
                continue;
            }
            int pt = ct.getPhanTramGiamBienThe() == null ? 0 : ct.getPhanTramGiamBienThe();
            ketQua.computeIfAbsent(ct.getIdSanPhamChiTiet(), k -> new ArrayList<>())
                    .add(new MucGiam(d, pt, d.getNgayBatDau(), d.getNgayKetThuc()));
        }
        return ketQua;
    }

    // Chia [tu, den] thành các đoạn liên tiếp mà trong mỗi đoạn tập các đợt áp dụng không đổi.
    private List<DoanGiam> chiaDoan(List<MucGiam> mucs, LocalDateTime tu, LocalDateTime den, Long giaBan, CheDo cheDo) {
        // Các mốc đổi mức giảm: lúc bắt đầu mỗi đợt và 1 giây sau lúc kết thúc mỗi đợt (cắt trong [tu, den])
        TreeSet<LocalDateTime> moc = new TreeSet<>();
        moc.add(tu);
        moc.add(den.plusSeconds(1));
        for (MucGiam m : mucs) {
            LocalDateTime s = m.tu().isBefore(tu) ? tu : m.tu();
            LocalDateTime e = m.den().isAfter(den) ? den : m.den();
            if (s.isAfter(e)) {
                continue;
            }
            moc.add(s);
            moc.add(e.plusSeconds(1));
        }

        List<DoanGiam> ketQua = new ArrayList<>();
        List<LocalDateTime> ds = new ArrayList<>(moc);
        for (int i = 0; i + 1 < ds.size(); i++) {
            LocalDateTime dau = ds.get(i);
            LocalDateTime cuoi = ds.get(i + 1).minusSeconds(1);
            List<MucGiam> ap = new ArrayList<>();
            for (MucGiam m : mucs) {
                if (!m.tu().isAfter(dau) && !m.den().isBefore(cuoi)) {
                    ap.add(m);
                }
            }
            if (ap.isEmpty()) {
                continue;
            }
            int cao = 0;
            int tong = 0;
            List<DotTrongDoan> cacDot = new ArrayList<>();
            for (MucGiam m : ap) {
                cao = Math.max(cao, m.phanTram());
                tong += m.phanTram();
                cacDot.add(new DotTrongDoan(m.dot().getId(), m.dot().getMaDot(), m.dot().getTenDot(), m.phanTram()));
            }
            double trungBinh = Math.round((double) tong / ap.size() * 100.0) / 100.0;
            double congDon = Math.min(100, tong);
            double apDung = cheDo == CheDo.TRUNG_BINH ? trungBinh : cheDo == CheDo.CONG_DON ? congDon : cao;
            long gia = giaBan == null ? 0 : giaBan;
            long giaSau = giaSauGiam(gia, apDung);

            // Đoạn liền kề có cùng tập đợt (và cùng mức giảm) thì gộp lại cho gọn
            DoanGiam truoc = ketQua.isEmpty() ? null : ketQua.get(ketQua.size() - 1);
            if (truoc != null && truoc.denLuc().plusSeconds(1).equals(dau) && cungTapDot(truoc.cacDot(), cacDot)) {
                ketQua.set(ketQua.size() - 1, new DoanGiam(truoc.tuLuc(), cuoi, apDung, cao, congDon, trungBinh, cacDot, giaSau));
            } else {
                ketQua.add(new DoanGiam(dau, cuoi, apDung, cao, congDon, trungBinh, cacDot, giaSau));
            }
        }
        return ketQua;
    }

    /**
     * Giá sau giảm của một biến thể, làm tròn tới đồng (.5 làm tròn lên). Cùng công thức với frontend tinhGiaSauGiam
     * (danh sách sản phẩm / biến thể, ví dụ trên form phiếu và form đợt giảm giá) nên số trên lịch khớp từng đồng.
     * Vd 2.500.000 giảm 15% -> 2.125.000; giảm 12,5% -> 2.187.500; 5.000 giảm 39,67% -> 3.017.
     */
    static long giaSauGiam(long giaBan, double phanTram) {
        if (phanTram <= 0) return giaBan;
        if (phanTram >= 100) return 0;
        return Math.round((giaBan * (100 - phanTram)) / 100);
    }

    private boolean cungTapDot(List<DotTrongDoan> a, List<DotTrongDoan> b) {
        if (a.size() != b.size()) {
            return false;
        }
        Set<Long> ida = new HashSet<>();
        for (DotTrongDoan x : a) {
            ida.add(x.idDot());
        }
        for (DotTrongDoan x : b) {
            if (!ida.contains(x.idDot())) {
                return false;
            }
        }
        return true;
    }
}

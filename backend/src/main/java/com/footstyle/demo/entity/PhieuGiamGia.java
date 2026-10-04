package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "phieu_giam_gia")
public class PhieuGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "ma_phieu")
    private String maPhieu;

    @Column(name = "ten_phieu")
    private String tenPhieu;

    @Column(name = "doi_tuong_ap_dung")
    private Integer doiTuongApDung;

    @Column(name = "loai_giam")
    private Integer loaiGiam;

    @Column(name = "gia_tri_giam")
    private Long giaTriGiam;

    @Column(name = "giam_toi_da")
    private Long giamToiDa;

    @Column(name = "gia_tri_don_hang_toi_thieu")
    private Long giaTriDonHangToiThieu;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "so_luong_da_dung")
    private Integer soLuongDaDung;

    @Column(name = "gioi_han_moi_khach")
    private Integer gioiHanMoiKhach;

    @Column(name = "ngay_bat_dau")
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDateTime ngayKetThuc;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "trang_thai")
    private Integer trangThai;

    public static final int DOI_TUONG_TAT_CA = 1;       // doi_tuong_ap_dung: Tất cả (phiếu công khai)
    public static final int DOI_TUONG_KHACH_CU_THE = 2; // doi_tuong_ap_dung: Khách hàng cụ thể (phiếu cá nhân)
    public static final int LOAI_PHAN_TRAM = 1;         // loai_giam: Phần trăm
    public static final int LOAI_SO_TIEN = 2;           // loai_giam: Số tiền
    public static final int HOAT_DONG = 1;              // trang_thai
    public static final int NGUNG_HOAT_DONG = 0;
}

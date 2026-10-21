package com.footstyle.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Bảng hoi_thoai: một cuộc chat (khách hỏi cửa hàng, hoặc chat nội bộ 1-1 / nhóm giữa nhân viên). */
@Entity
@Getter
@Setter
@Table(name = "hoi_thoai")
public class HoiThoai {

    // loai
    public static final int HO_TRO_KHACH = 1;
    public static final int NOI_BO_RIENG = 2;
    public static final int NOI_BO_NHOM = 3;

    // che_do (chỉ hội thoại hỗ trợ khách)
    public static final int TRO_LY_AO = 1;
    public static final int CHO_NHAN_VIEN = 2;
    public static final int NHAN_VIEN_HO_TRO = 3;
    public static final int DA_DONG = 4;

    /** Nhóm chat chung của cả cửa hàng (mọi nhân viên đang hoạt động tự là thành viên). */
    public static final String KHOA_NHOM_CHUNG = "NHOM_CHUNG";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "loai")
    private Integer loai;

    @Column(name = "tieu_de")
    private String tieuDe;

    @Column(name = "id_khach_hang")
    private Integer idKhachHang;

    @Column(name = "ten_khach")
    private String tenKhach;

    @Column(name = "sdt_khach")
    private String sdtKhach;

    @Column(name = "ma_truy_cap")
    private String maTruyCap;

    @Column(name = "che_do")
    private Integer cheDo = TRO_LY_AO;

    @Column(name = "id_nhan_vien_phu_trach")
    private Integer idNhanVienPhuTrach;

    @Column(name = "khoa_rieng")
    private String khoaRieng;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    /** Thời điểm có tin nhắn mới nhất (để sắp xếp danh sách hội thoại). */
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;
}

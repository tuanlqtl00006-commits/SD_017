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

/** Bảng tin_nhan. */
@Entity
@Getter
@Setter
@Table(name = "tin_nhan")
public class TinNhan {

    // loai_nguoi_gui
    public static final int KHACH = 1;
    public static final int NHAN_VIEN = 2;
    public static final int TRO_LY_AO = 3;
    public static final int HE_THONG = 4;

    /** Độ dài tối đa của một tin nhắn (cột NVARCHAR(2000); giới hạn nhập 1.000 ký tự cho dễ đọc). */
    public static final int DO_DAI_TOI_DA = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_hoi_thoai")
    private Integer idHoiThoai;

    @Column(name = "loai_nguoi_gui")
    private Integer loaiNguoiGui;

    @Column(name = "id_nhan_vien")
    private Integer idNhanVien;

    @Column(name = "ten_nguoi_gui")
    private String tenNguoiGui;

    @Column(name = "noi_dung")
    private String noiDung;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;
}

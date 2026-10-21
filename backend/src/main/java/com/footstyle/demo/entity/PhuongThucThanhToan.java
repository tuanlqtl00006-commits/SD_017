package com.footstyle.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Bảng phuong_thuc_thanh_toan (PT01 tiền mặt, PT02 chuyển khoản, PT03 COD, PT04 VNPay, PT05 tiền mặt + chuyển khoản). */
@Entity
@Getter
@Setter
@Table(name = "phuong_thuc_thanh_toan")
public class PhuongThucThanhToan {

    public static final String TIEN_MAT = "PT01";
    public static final String CHUYEN_KHOAN = "PT02";
    public static final String COD = "PT03";
    public static final String VNPAY = "PT04";
    public static final String KET_HOP = "PT05";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_hinh_thuc_thanh_toan")
    private Integer idHinhThucThanhToan;

    @Column(name = "ma_phuong_thuc")
    private String maPhuongThuc;

    @Column(name = "ten_phuong_thuc")
    private String tenPhuongThuc;

    @Column(name = "trang_thai")
    private Integer trangThai;
}

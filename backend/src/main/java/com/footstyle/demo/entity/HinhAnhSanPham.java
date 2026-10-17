package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Bảng hinh_anh_san_pham: một sản phẩm có nhiều ảnh.
 *  - id_mau_sac = NULL: ảnh chung của sản phẩm (đúng một ảnh có la_anh_chinh = 1).
 *  - id_mau_sac = id màu: ảnh của riêng màu đó, dùng cho MỌI biến thể cùng màu (mỗi màu có một ảnh chính).
 * Cột id_mau_sac nằm trong database/SD_17_1_cau_truc.sql (CSDL cũ được thêm cột ở PHẦN 6 của file đó); backend cũng tự thêm khi khởi động.
 */
@Entity
@Getter
@Setter
@Table(name = "hinh_anh_san_pham")
public class HinhAnhSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_san_pham")
    private SanPham sanPham;

    /** NULL = ảnh chung của sản phẩm; có giá trị = ảnh của màu này (áp dụng cho mọi biến thể cùng màu). */
    @Column(name = "id_mau_sac")
    private Integer idMauSac;

    @Column(name = "duong_dan_hinh_anh")
    private String duongDanHinhAnh;

    @Column(name = "la_anh_chinh")
    private Boolean laAnhChinh;

    @Column(name = "trang_thai")
    private Integer trangThai;

    public static final int HOAT_DONG = 1;        // trang_thai (0 = ảnh đã bỏ khỏi sản phẩm)
    public static final int NGUNG_HOAT_DONG = 0;
}

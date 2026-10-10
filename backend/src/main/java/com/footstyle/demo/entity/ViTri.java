package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Vị trí làm việc của nhân viên (Bán hàng tại quầy, Thu ngân, Kho, Giao hàng...).
 * Một nhân viên có thể giữ nhiều vị trí. Vị trí chỉ được THÊM và CHỌN, không có chức năng xóa.
 */
@Entity
@Getter
@Setter
@Table(name = "vi_tri")
public class ViTri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "ten_vi_tri")
    private String tenViTri;

    @Column(name = "trang_thai")
    private Integer trangThai;

    public static final int DANG_DUNG = 1;
}

package com.footstyle.demo.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Bảng thuong_hieu: Thương hiệu. Các cột id, trạng thái nằm ở lớp cha ThuocTinh. */
@Entity
@Getter
@Setter
@Table(name = "thuong_hieu")
@AttributeOverride(name = "ma", column = @Column(name = "ma_thuong_hieu"))
@AttributeOverride(name = "ten", column = @Column(name = "ten_thuong_hieu"))
public class ThuongHieu extends ThuocTinh {
}

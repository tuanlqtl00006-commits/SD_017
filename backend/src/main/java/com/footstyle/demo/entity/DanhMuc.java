package com.footstyle.demo.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "danh_muc")
@AttributeOverride(name = "ma", column = @Column(name = "ma_danh_muc"))
@AttributeOverride(name = "ten", column = @Column(name = "ten_danh_muc"))
public class DanhMuc extends ThuocTinh {
}

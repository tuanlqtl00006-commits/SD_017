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
@Table(name = "diem_can_bang")
@AttributeOverride(name = "ma", column = @Column(name = "ma_diem_can_bang"))
@AttributeOverride(name = "ten", column = @Column(name = "ten_diem_can_bang"))
public class DiemCanBang extends ThuocTinh {
}

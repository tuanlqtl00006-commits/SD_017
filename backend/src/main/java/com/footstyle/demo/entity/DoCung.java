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
@Table(name = "do_cung")
@AttributeOverride(name = "ma", column = @Column(name = "ma_do_cung"))
@AttributeOverride(name = "ten", column = @Column(name = "ten_do_cung"))
public class DoCung extends ThuocTinh {
}

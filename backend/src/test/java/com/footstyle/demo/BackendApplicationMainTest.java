package com.footstyle.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Cờ chạy được trên JDK mới hơn thư viện Byte Buddy biết (xem BackendApplication.choPhepJdkMoiHon). Không cần Spring hay CSDL. */
class BackendApplicationMainTest {

    private static final String CO = "net.bytebuddy.experimental";
    private String cu;

    @BeforeEach
    void luu() {
        cu = System.getProperty(CO);
    }

    @AfterEach
    void khoiPhuc() {
        if (cu == null) System.clearProperty(CO);
        else System.setProperty(CO, cu);
    }

    @Test
    void chuaDatThiBatCo() {
        System.clearProperty(CO);
        BackendApplication.choPhepJdkMoiHon();
        assertEquals("true", System.getProperty(CO));
    }

    @Test
    void daTuDatThiGiuNguyenKhongGhiDe() {
        System.setProperty(CO, "false");
        BackendApplication.choPhepJdkMoiHon();
        assertEquals("false", System.getProperty(CO));
    }
}

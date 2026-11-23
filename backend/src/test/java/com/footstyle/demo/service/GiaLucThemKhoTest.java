package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Kho giá lúc thêm (21/11): ghi tệp, đọc lại khi khởi động; trong giao dịch chỉ ghi sau commit, rollback thì không ghi. */
class GiaLucThemKhoTest {

    @TempDir
    Path thuMuc;

    @AfterEach
    void don() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void khongCoGiaoDich_ghiNgay_khoMoiDocLaiDuoc() {
        GiaLucThemKho kho = new GiaLucThemKho(thuMuc.toString());
        kho.luu(7, new GiaLucThemKho.GiaLucThem(3_432_000L, 4_290_000L, 20.0));
        assertTrue(Files.exists(thuMuc.resolve("gia-luc-them.json")));

        GiaLucThemKho sauKhoiDong = new GiaLucThemKho(thuMuc.toString());
        assertEquals(new GiaLucThemKho.GiaLucThem(3_432_000L, 4_290_000L, 20.0), sauKhoiDong.lay(7));
        sauKhoiDong.xoaNhieu(List.of(7, 99));
        assertNull(new GiaLucThemKho(thuMuc.toString()).lay(7));
    }

    @Test
    void tepCuChuaCoGiaTruoc_vanDocDuoc_giaTruocLuuVaDocLai() throws Exception {
        Files.writeString(thuMuc.resolve("gia-luc-them.json"), "{\"5\":{\"donGia\":100,\"giaGoc\":200,\"phanTram\":50.0}}");
        GiaLucThemKho kho = new GiaLucThemKho(thuMuc.toString());
        assertEquals(new GiaLucThemKho.GiaLucThem(100L, 200L, 50.0), kho.lay(5), "tệp ghi trước 23/11 không có giaTruoc");
        kho.luu(6, new GiaLucThemKho.GiaLucThem(90L, 200L, 55.0, 100L));
        assertEquals(100L, new GiaLucThemKho(thuMuc.toString()).lay(6).giaTruoc());
    }

    @Test
    void trongGiaoDich_chiGhiSauCommit_rollbackThiKhongGhi() {
        GiaLucThemKho kho = new GiaLucThemKho(thuMuc.toString());
        kho.luu(1, new GiaLucThemKho.GiaLucThem(100L, 200L, 50.0));

        // Giao dịch bị rollback: xóa dòng 1 không có tác dụng, thêm dòng 2 không được ghi
        TransactionSynchronizationManager.initSynchronization();
        kho.xoa(1);
        kho.luu(2, new GiaLucThemKho.GiaLucThem(300L, 400L, 25.0));
        assertTrue(kho.coDong(1), "chưa commit thì chưa xóa");
        assertFalse(kho.coDong(2), "chưa commit thì chưa thêm");
        TransactionSynchronizationManager.getSynchronizations().forEach((s) -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        TransactionSynchronizationManager.clearSynchronization();
        assertTrue(new GiaLucThemKho(thuMuc.toString()).coDong(1));
        assertFalse(new GiaLucThemKho(thuMuc.toString()).coDong(2));

        // Giao dịch commit: áp dụng
        TransactionSynchronizationManager.initSynchronization();
        kho.luu(2, new GiaLucThemKho.GiaLucThem(300L, 400L, 25.0));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        TransactionSynchronizationManager.clearSynchronization();
        assertTrue(new GiaLucThemKho(thuMuc.toString()).coDong(2));
    }
}

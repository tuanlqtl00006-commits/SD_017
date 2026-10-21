package com.footstyle.demo.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Đồng hồ cho test, chỉnh được giờ (vd tiến 1 phút để thử giới hạn gửi tin). */
final class DongHoChinh extends Clock {

    private Instant luc;

    DongHoChinh(LocalDateTime luc) {
        dat(luc);
    }

    void dat(LocalDateTime moi) {
        luc = moi.atZone(CaLamMau.VN).toInstant();
    }

    void tien(long giay) {
        luc = luc.plusSeconds(giay);
    }

    @Override
    public ZoneId getZone() {
        return CaLamMau.VN;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return luc;
    }
}

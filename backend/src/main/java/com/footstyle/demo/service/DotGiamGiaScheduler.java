package com.footstyle.demo.service;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.repository.DotGiamGiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DotGiamGiaScheduler {

    @Autowired
    private DotGiamGiaRepository dotGiamGiaRepository;

    // Chạy tự động mỗi phút (60,000 milliseconds)
    @Scheduled(fixedRate = 60000)
    public void capNhatTrangThaiDotGiamGia() {
        List<DotGiamGia> dsDotGiamGia = dotGiamGiaRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        boolean hasUpdates = false;

        for (DotGiamGia dgg : dsDotGiamGia) {
            int oldStatus = dgg.getTrangThai() == null ? 1 : dgg.getTrangThai();
            int newStatus = oldStatus;

            // Logic switch-case dựa vào thời gian để tự động cập nhật trạng thái
            // Giả định trạng thái: 0 = Đã kết thúc, 1 = Đang diễn ra, 2 = Sắp diễn ra (giống với cách làm chuẩn)
            // Lưu ý: Nếu admin chủ động tắt (ví dụ set thành 3 hoặc trạng thái Hủy) thì có thể skip.
            // Tuy nhiên, theo yêu cầu tự động đổi trạng thái theo timeline:
            
            if (dgg.getNgayBatDau() != null && dgg.getNgayKetThuc() != null) {
                if (now.isBefore(dgg.getNgayBatDau())) {
                    newStatus = 2; // Sắp diễn ra
                } else if (now.isAfter(dgg.getNgayKetThuc())) {
                    newStatus = 0; // Đã kết thúc
                } else {
                    newStatus = 1; // Đang diễn ra
                }
            }

            if (oldStatus != newStatus) {
                dgg.setTrangThai(newStatus);
                hasUpdates = true;
            }
        }

        if (hasUpdates) {
            dotGiamGiaRepository.saveAll(dsDotGiamGia);
            System.out.println("CronJob: Đã cập nhật trạng thái tự động cho Đợt giảm giá lúc " + now);
        }
    }
}

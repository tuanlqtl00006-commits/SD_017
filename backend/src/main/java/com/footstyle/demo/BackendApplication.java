package com.footstyle.demo;

import com.footstyle.demo.config.GiaiPhongCong;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        uuTienIPv4();
        // Đã có backend FootStyle chạy sẵn ở cổng 8081 thì GIỮ NGUYÊN nó, bản chạy thêm này in thông báo rồi thoát (xem GiaiPhongCong)
        GiaiPhongCong.truocKhiChay(args);
        choPhepJdkMoiHon();
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Máy cài JDK mới hơn bản Byte Buddy (thư viện Hibernate dùng để tạo lớp proxy) biết thì Byte Buddy báo
     * "Java NN is not supported by the current version of Byte Buddy" và backend không lên được. Cờ này cho nó chạy
     * theo phiên bản mới nhất nó biết; trên JDK được hỗ trợ thì không có tác dụng gì. Ai đã tự đặt cờ (-D...) thì giữ nguyên.
     * (pom.xml đã đặt sẵn bản Byte Buddy mới; cờ này là lớp bảo vệ thứ hai cho JDK còn mới hơn nữa.)
     */
    /**
     * Gọi API bản đồ (OpenStreetMap) để tính phí giao hàng: trên máy có mạng IPv6 chập chờn, Java nối qua IPv6 bị từ chối
     * ("Connection refused") nên luôn rơi về bảng phí tỉnh. Dùng IPv4 cho mọi kết nối mạng (CSDL, localhost vẫn chạy bình thường).
     * Phải đặt trước khi dùng bất kỳ lớp mạng nào. Ai đã tự đặt cờ (-D...) thì giữ nguyên.
     */
    static void uuTienIPv4() {
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
    }

    static void choPhepJdkMoiHon() {
        if (System.getProperty("net.bytebuddy.experimental") == null) {
            System.setProperty("net.bytebuddy.experimental", "true");
        }
    }

}

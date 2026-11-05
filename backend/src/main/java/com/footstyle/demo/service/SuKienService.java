package com.footstyle.demo.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Kênh báo thay đổi dữ liệu (Server-Sent Events, GET /api/su-kien): ghi dữ liệu thành công ở đâu (bán quầy, đặt online, đổi trạng thái đơn,
 * sửa sản phẩm...) thì mọi trình duyệt đang mở nhận ngay tin { chuDe: [...], nguon } và màn liên quan tự tải lại, không cần F5.
 * Tin chỉ có tên chủ đề (không có dữ liệu khách / đơn), màn nhận tin tự gọi API (có kiểm tra quyền) để lấy dữ liệu mới.
 * Gửi tin trên một luồng riêng nên yêu cầu ghi không phải chờ; kết nối hỏng thì bỏ khỏi danh sách.
 */
@Service
public class SuKienService {

    /** Kết nối sống tối đa 30 phút rồi trình duyệt tự nối lại (không để kết nối "chết" treo mãi). */
    static final long THOI_HAN_MS = 30 * 60_000L;
    /** Mất kết nối thì trình duyệt chờ bấy nhiêu rồi nối lại. */
    static final long NOI_LAI_SAU_MS = 2_000L;
    /** Mã tab do giao diện gửi kèm (header X-Tab-Id) để tab vừa ghi không tải lại thừa; chỉ nhận chữ, số, gạch nối. */
    private static final Pattern MA_TAB = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private final List<SseEmitter> ketNoi = new CopyOnWriteArrayList<>();
    private final AtomicLong soTin = new AtomicLong();
    private final ExecutorService luongGui = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "su-kien-sse");
        t.setDaemon(true);
        return t;
    });

    /** Mở một kết nối mới; gửi ngay tin "san-sang" để trình duyệt biết đã nối được (và nối lại sau 2 giây nếu rớt). */
    public SseEmitter moKetNoi() {
        SseEmitter e = new SseEmitter(THOI_HAN_MS);
        e.onCompletion(() -> ketNoi.remove(e));
        e.onTimeout(() -> {
            ketNoi.remove(e);
            e.complete();
        });
        e.onError(loi -> ketNoi.remove(e));
        try {
            e.send(SseEmitter.event().name("san-sang").reconnectTime(NOI_LAI_SAU_MS).data(Map.of("so", soTin.get()), MediaType.APPLICATION_JSON));
        } catch (Exception loi) {
            e.completeWithError(loi);
            return e;
        }
        ketNoi.add(e);
        return e;
    }

    /** Báo các chủ đề vừa đổi; `nguon` = mã tab đã ghi (có thể null). */
    public void bao(Set<String> chuDe, String nguon) {
        if (chuDe == null || chuDe.isEmpty() || ketNoi.isEmpty()) return;
        Map<String, Object> tin = new LinkedHashMap<>();
        tin.put("chuDe", List.copyOf(new TreeSet<>(chuDe)));
        tin.put("nguon", nguon != null && MA_TAB.matcher(nguon).matches() ? nguon : null);
        String id = String.valueOf(soTin.incrementAndGet());
        guiTatCa(() -> SseEmitter.event().id(id).name("thay-doi").data(tin, MediaType.APPLICATION_JSON));
    }

    /**
     * Nhịp tim 20 giây (tin "ping"): giữ kết nối qua proxy, máy chủ phát hiện trình duyệt đã đóng (gửi lỗi thì bỏ kết nối),
     * trình duyệt im lặng quá 45 giây thì biết kết nối đã chết và tự nối lại.
     */
    @Scheduled(fixedDelay = 20_000L, initialDelay = 20_000L)
    public void nhipTim() {
        if (!ketNoi.isEmpty()) guiTatCa(() -> SseEmitter.event().name("ping").data(soTin.get()));
    }

    public int soKetNoi() {
        return ketNoi.size();
    }

    /** Tắt backend: đóng hết kết nối ngay (trình duyệt tự nối lại khi backend chạy lên). */
    @EventListener(ContextClosedEvent.class)
    public void dongTatCa() {
        for (SseEmitter e : ketNoi) {
            try {
                e.complete();
            } catch (Exception ignored) {
                // đã đóng
            }
        }
        ketNoi.clear();
        luongGui.shutdownNow();
    }

    /** Mỗi kết nối một bản tin mới (SseEventBuilder không dùng lại được). */
    private void guiTatCa(Supplier<SseEmitter.SseEventBuilder> tin) {
        if (luongGui.isShutdown()) return;
        try {
            luongGui.execute(() -> {
                for (SseEmitter e : ketNoi) {
                    try {
                        e.send(tin.get());
                    } catch (Exception loi) {
                        ketNoi.remove(e); // trình duyệt đã đóng tab / mất mạng
                    }
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            // đang tắt backend
        }
    }
}

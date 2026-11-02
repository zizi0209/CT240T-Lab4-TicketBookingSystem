package com.ctu.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * STEP 1: Khởi tạo Luồng với Runnable và ExecutorService (Thread Pool)
 *
 * Ưu điểm:
 * - Tránh chi phí khởi tạo và hủy luồng liên tục bằng new Thread().
 * - Tái sử dụng tập hợp luồng có sẵn (Thread Pool).
 * - Kiểm soát số lượng luồng tối đa, tránh cạn kiệt tài nguyên CPU/RAM.
 */
public class ThreadPoolDemo {
    public static void main(String[] args) {
        // Tạo Thread Pool với 4 luồng tái sử dụng
        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            // Gửi tác vụ Runnable vào Thread Pool
            executor.submit(() -> {
                System.out.println("Tác vụ " + taskId + " đang thực thi bởi luồng: " + Thread.currentThread().getName());
                try {
                    Thread.sleep(300); // Giả lập thời gian xử lý I/O
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Tắt Thread Pool một cách an toàn (Graceful Shutdown)
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}

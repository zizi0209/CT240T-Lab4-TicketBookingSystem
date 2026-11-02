package com.ctu.concurrency;

import java.util.concurrent.CompletableFuture;

/**
 * STEP 4: Lập trình Bất đồng bộ với CompletableFuture
 * Thực thi các tác vụ chạy nền (non-blocking) và xâu chuỗi kết quả (pipeline).
 */
public class AsyncPipelineDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 Luồng chính bắt đầu...");

        // Chạy tác vụ lấy dữ liệu bất đồng bộ
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Dữ liệu người dùng ID: 101";
        }).thenApply(data -> {
            // Biến đổi dữ liệu (Transform)
            return data.toUpperCase() + " [ĐÃ XÁC THỰC]";
        });

        // Đăng ký Callback khi xử lý xong (Non-blocking)
        future.thenAccept(result -> {
            System.out.println("📩 Kết quả xử lý bất đồng bộ: " + result);
        });

        System.out.println("⏳ Luồng chính không bị nghẽn, tiếp tục làm việc khác...");
        Thread.sleep(1200); // Chờ để thấy kết quả callback
        System.out.println("🏁 Luồng chính kết thúc.");
    }
}

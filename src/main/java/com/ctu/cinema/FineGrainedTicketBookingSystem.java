package com.ctu.cinema;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * GIẢI PHÁP MỞ RỘNG NÂNG CAO (ADVANCED OPTIMIZATION):
 * Hệ thống đặt vé với cơ chế khóa mịn theo từng ghế (Fine-Grained Seat-Level Locking).
 *
 * So sánh với Coarse-Grained Lock (Khóa toàn bộ rạp):
 * - Coarse-Grained: 1 khóa duy nhất cho 50 ghế. Hai khách hàng đặt hai ghế KHÁC NHAU
 *   vẫn phải xếp hàng chờ nhau (Bottleneck / Giảm hiệu năng đa luồng).
 * - Fine-Grained: Mỗi ghế có 1 khóa ReentrantLock riêng biệt. Khách hàng đặt ghế #1
 *   và Khách hàng đặt ghế #2 xử lý HOÀN TOÀN SONG SONG, chỉ tranh chấp khi CÙNG đặt 1 ghế.
 *   => Giúp tăng Throughput và giảm độ trễ (Latency) đáng kể!
 */
public class FineGrainedTicketBookingSystem {
    private static final int TOTAL_SEATS = 50;

    // Trạng thái ghế
    private final boolean[] seats = new boolean[TOTAL_SEATS + 1];
    private final String[] bookedBy = new String[TOTAL_SEATS + 1];

    // Mảng khóa độc lập cho từng ghế (Index 1..50)
    private final ReentrantLock[] seatLocks = new ReentrantLock[TOTAL_SEATS + 1];

    // Thống kê bằng biến nguyên tử
    private final AtomicInteger successfulBookings = new AtomicInteger(0);
    private final AtomicInteger failedBookings = new AtomicInteger(0);

    public FineGrainedTicketBookingSystem() {
        for (int i = 1; i <= TOTAL_SEATS; i++) {
            seatLocks[i] = new ReentrantLock();
        }
    }

    /**
     * Đặt vé an toàn luồng với khóa mịn theo từng ghế
     */
    public boolean bookSeat(int seatNumber, String customerName) {
        if (seatNumber < 1 || seatNumber > TOTAL_SEATS) {
            System.err.println("❌ Ghế không hợp lệ: " + seatNumber);
            return false;
        }

        // Chỉ khóa duy nhất chiếc ghế mà khách hàng muốn đặt
        ReentrantLock seatLock = seatLocks[seatNumber];
        seatLock.lock();
        try {
            if (!seats[seatNumber]) {
                try {
                    Thread.sleep(10); // Giả lập I/O xử lý
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                seats[seatNumber] = true;
                bookedBy[seatNumber] = customerName;
                successfulBookings.incrementAndGet();
                System.out.println("✅ [Thành công] " + customerName + " đã đặt ghế #" + seatNumber);
                return true;
            } else {
                failedBookings.incrementAndGet();
                System.out.println("❌ [Thất bại] " + customerName + " đặt ghế #" + seatNumber + " nhưng ghế ĐÃ BỊ CHIẾM bởi " + bookedBy[seatNumber] + "!");
                return false;
            }
        } finally {
            seatLock.unlock(); // Luôn giải phóng khóa của ghế tương ứng
        }
    }

    public int getSuccessfulBookings() {
        return successfulBookings.get();
    }

    public int getFailedBookings() {
        return failedBookings.get();
    }

    public boolean isSeatBooked(int seatNumber) {
        if (seatNumber < 1 || seatNumber > TOTAL_SEATS) {
            return false;
        }
        ReentrantLock seatLock = seatLocks[seatNumber];
        seatLock.lock();
        try {
            return seats[seatNumber];
        } finally {
            seatLock.unlock();
        }
    }

    public String getCustomerForSeat(int seatNumber) {
        if (seatNumber < 1 || seatNumber > TOTAL_SEATS) {
            return null;
        }
        ReentrantLock seatLock = seatLocks[seatNumber];
        seatLock.lock();
        try {
            return bookedBy[seatNumber];
        } finally {
            seatLock.unlock();
        }
    }

    public static int getTotalSeats() {
        return TOTAL_SEATS;
    }

    public static void main(String[] args) throws InterruptedException {
        FineGrainedTicketBookingSystem bookingSystem = new FineGrainedTicketBookingSystem();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        System.out.println("🚀 Bắt đầu mô phỏng 100 yêu cầu đặt vé (FINE-GRAINED SEAT-LEVEL LOCK)...");
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= 100; i++) {
            final int customerId = i;
            final int targetSeat = (i % 50) + 1;
            executor.submit(() -> {
                bookingSystem.bookSeat(targetSeat, "Khách hàng #" + customerId);
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("\n========= THỐNG KÊ TỔNG KẾT (FINE-GRAINED LOCK RUN) =========");
        System.out.println("Thời gian thực thi         : " + duration + " ms (Nhanh vượt trội nhờ xử lý song song các ghế khác nhau)");
        System.out.println("Tổng số lượt đặt THÀNH CÔNG: " + bookingSystem.getSuccessfulBookings());
        System.out.println("Tổng số lượt đặt THẤT BẠI  : " + bookingSystem.getFailedBookings());
        System.out.println("Tổng số yêu cầu đã xử lý   : " + (bookingSystem.getSuccessfulBookings() + bookingSystem.getFailedBookings()));
        System.out.println("=============================================================");
    }
}

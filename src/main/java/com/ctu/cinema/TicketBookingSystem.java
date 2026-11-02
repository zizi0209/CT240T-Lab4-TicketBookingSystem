package com.ctu.cinema;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * BÀI TẬP 1 - BƯỚC 1: SKELETON CODE (THREAD-UNSAFE)
 * Hệ thống đặt vé xem phim real-time đa luồng CHƯA ĐỒNG BỘ.
 *
 * Lỗi kỹ thuật:
 * - Thao tác kiểm tra trạng thái ghế (!seats[seatNumber]) và cập nhật (seats[seatNumber] = true)
 *   không có cơ chế đồng bộ, dẫn đến hiện tượng Race Condition (Check-Then-Act race).
 * - Biến đếm successfulBookings++ và failedBookings++ là thao tác non-atomic (Read-Modify-Write),
 *   dẫn đến mất mát giá trị cập nhật.
 */
public class TicketBookingSystem {
    private static final int TOTAL_SEATS = 50;

    // Mảng trạng thái ghế (Index 1..50). Ban đầu mặc định false (Chưa đặt)
    private final boolean[] seats = new boolean[TOTAL_SEATS + 1];

    // Biến đếm thống kê đơn giản (Chưa an toàn luồng)
    private int successfulBookings = 0;
    private int failedBookings = 0;

    /**
     * Phương thức đặt vé cho ghế seatNumber
     * CHÚ Ý: Phiên bản ban đầu CHƯA ĐỒNG BỘ - Dễ bị nổ lỗi Race Condition!
     *
     * @param seatNumber   Số ghế cần đặt (1..50)
     * @param customerName Tên khách hàng đặt vé
     * @return true nếu đặt thành công, false nếu ghế đã bị chiếm
     */
    public boolean bookSeat(int seatNumber, String customerName) {
        if (seatNumber < 1 || seatNumber > TOTAL_SEATS) {
            System.err.println("❌ Ghế không hợp lệ: " + seatNumber);
            return false;
        }

        // --- MIỀN TỐI HẠN (CRITICAL SECTION) CHƯA ĐƯỢC BẢO VỆ ---
        if (!seats[seatNumber]) { // 1. Read: Kiểm tra trạng thái ghế
            // Giả lập thời gian độ trễ mạng / I/O xử lý (10ms)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            seats[seatNumber] = true; // 2. Write: Đánh dấu ghế đã bị chiếm
            successfulBookings++;     // 3. Increment (Non-atomic)
            System.out.println("✅ [Thành công] " + customerName + " đã đặt thành công ghế #" + seatNumber);
            return true;
        } else {
            failedBookings++;
            System.out.println("❌ [Thất bại] " + customerName + " đặt ghế #" + seatNumber + " nhưng ghế ĐÃ BỊ CHIẾM!");
            return false;
        }
        // --------------------------------------------------------
    }

    public int getSuccessfulBookings() {
        return successfulBookings;
    }

    public int getFailedBookings() {
        return failedBookings;
    }

    public boolean isSeatBooked(int seatNumber) {
        if (seatNumber < 1 || seatNumber > TOTAL_SEATS) {
            return false;
        }
        return seats[seatNumber];
    }

    public static int getTotalSeats() {
        return TOTAL_SEATS;
    }

    // Test Runner giả lập 100 khách hàng đặt vé đồng thời trên Thread Pool
    public static void main(String[] args) throws InterruptedException {
        TicketBookingSystem bookingSystem = new TicketBookingSystem();
        ExecutorService executor = Executors.newFixedThreadPool(10); // Pool 10 luồng chạy song song

        System.out.println("🚀 Bắt đầu mô phỏng 100 yêu cầu đặt vé đồng thời (UNSAFE RUN)...");
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= 100; i++) {
            final int customerId = i;
            // 100 khách hàng tranh nhau đặt ngẫu nhiên các ghế từ 1 đến 50
            final int targetSeat = (i % 50) + 1;
            executor.submit(() -> {
                bookingSystem.bookSeat(targetSeat, "Khách hàng #" + customerId);
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        System.out.println("\n========= THỐNG KÊ TỔNG KẾT (UNSAFE RUN) =========");
        System.out.println("Thời gian thực thi: " + duration + " ms");
        System.out.println("Tổng số lượt báo THÀNH CÔNG: " + bookingSystem.getSuccessfulBookings());
        System.out.println("Tổng số lượt báo THẤT BẠI  : " + bookingSystem.getFailedBookings());
        System.out.println("Tổng số yêu cầu đã xử lý   : " + (bookingSystem.getSuccessfulBookings() + bookingSystem.getFailedBookings()));
        System.out.println("=================================================");
        System.out.println("⚠️  NHẬN XÉT: Do không có đồng bộ, nhiều luồng cùng đọc !seats[seatNumber] == true,");
        System.out.println("    dẫn đến hiện tượng đè ghế (nhiều khách hàng cùng đặt 1 ghế) và");
        System.out.println("    tổng số vé THÀNH CÔNG vượt quá 50 (hoặc số liệu thống kê bị sai lệch)!");
    }
}

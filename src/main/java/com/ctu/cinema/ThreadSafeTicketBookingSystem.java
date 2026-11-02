package com.ctu.cinema;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * BÀI TẬP 1 - BƯỚC 2 & 3: GIẢI PHÁP MẪU HOÀN CHỈNH (THREAD-SAFE SOLUTION)
 * Hệ thống đặt vé xem phim real-time đa luồng chống đè ghế.
 *
 * Cơ chế đảm bảo an toàn luồng (Thread-Safety):
 * 1. ReentrantLock: Khóa tường minh bảo vệ miền tối hạn (Critical Section),
 *    đảm bảo chuỗi thao tác Check-Then-Act (!seats[seatNumber] -> seats[seatNumber] = true)
 *    chỉ được thực hiện tuần tự bởi một luồng tại một thời điểm.
 *    Khóa LUÔN được giải phóng trong khối finally.
 * 2. AtomicInteger: Sử dụng biến nguyên tử hoạt động trên cơ chế CAS (Compare-And-Swap)
 *    ở cấp độ phần cứng để thống kê số vé thành công / thất bại mà không bị tranh chấp.
 */
public class ThreadSafeTicketBookingSystem {
    private static final int TOTAL_SEATS = 50;

    // Mảng trạng thái ghế (Index 1..50). Ban đầu mặc định false (Chưa đặt)
    private final boolean[] seats = new boolean[TOTAL_SEATS + 1];

    // Mảng lưu thông tin khách hàng đã đặt từng ghế (Index 1..50)
    private final String[] bookedBy = new String[TOTAL_SEATS + 1];

    // 1. Khóa tường minh bảo vệ miền tối hạn
    private final ReentrantLock lock = new ReentrantLock();

    // 2. Biến đếm nguyên tử an toàn luồng
    private final AtomicInteger successfulBookings = new AtomicInteger(0);
    private final AtomicInteger failedBookings = new AtomicInteger(0);

    /**
     * Phương thức đặt vé an toàn luồng (Thread-Safe)
     *
     * @param seatNumber   Số ghế cần đặt (1..50)
     * @param customerName Tên khách hàng đặt vé
     * @return true nếu đặt thành công, false nếu ghế đã bị chiếm hoặc không hợp lệ
     */
    public boolean bookSeat(int seatNumber, String customerName) {
        if (seatNumber < 1 || seatNumber > TOTAL_SEATS) {
            System.err.println("❌ Ghế không hợp lệ: " + seatNumber);
            return false;
        }

        // BẮT ĐẦU KHÓA MIỀN TỐI HẠN
        lock.lock();
        try {
            if (!seats[seatNumber]) {
                // Giả lập thời gian độ trễ mạng / I/O xử lý (10ms)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                seats[seatNumber] = true;
                bookedBy[seatNumber] = customerName;
                successfulBookings.incrementAndGet();
                System.out.println("✅ [Thành công] " + customerName + " đã đặt thành công ghế #" + seatNumber);
                return true;
            } else {
                failedBookings.incrementAndGet();
                System.out.println("❌ [Thất bại] " + customerName + " đặt ghế #" + seatNumber + " nhưng ghế ĐÃ BỊ CHIẾM bởi " + bookedBy[seatNumber] + "!");
                return false;
            }
        } finally {
            lock.unlock(); // LUÔN GIẢI PHÓNG KHÓA TRONG KHỐI FINALLY
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
        lock.lock();
        try {
            return seats[seatNumber];
        } finally {
            lock.unlock();
        }
    }

    public String getCustomerForSeat(int seatNumber) {
        if (seatNumber < 1 || seatNumber > TOTAL_SEATS) {
            return null;
        }
        lock.lock();
        try {
            return bookedBy[seatNumber];
        } finally {
            lock.unlock();
        }
    }

    public static int getTotalSeats() {
        return TOTAL_SEATS;
    }

    // Test Runner giả lập 100 khách hàng đặt vé đồng thời trên Thread Pool
    public static void main(String[] args) throws InterruptedException {
        ThreadSafeTicketBookingSystem bookingSystem = new ThreadSafeTicketBookingSystem();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        System.out.println("🚀 Bắt đầu mô phỏng 100 yêu cầu đặt vé đồng thời (THREAD-SAFE)...");
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

        System.out.println("\n========= THỐNG KÊ TỔNG KẾT (THREAD-SAFE RUN) =========");
        System.out.println("Thời gian thực thi         : " + duration + " ms");
        System.out.println("Tổng số lượt đặt THÀNH CÔNG: " + bookingSystem.getSuccessfulBookings());
        System.out.println("Tổng số lượt đặt THẤT BẠI  : " + bookingSystem.getFailedBookings());
        System.out.println("Tổng số yêu cầu đã xử lý   : " + (bookingSystem.getSuccessfulBookings() + bookingSystem.getFailedBookings()));
        System.out.println("=======================================================");

        // Kiểm tra tính đúng đắn theo tiêu chí nghiệm thu của đề bài
        if (bookingSystem.getSuccessfulBookings() == 50 && bookingSystem.getFailedBookings() == 50) {
            System.out.println("🎯 KẾT QUẢ: ĐẠT CHUẨN! Không xảy ra đè ghế, đúng 50 vé thành công và 50 vé thất bại.");
        } else {
            System.out.println("❌ KẾT QUẢ: CHƯA ĐẠT! Số lượng vé không khớp.");
        }
    }
}
